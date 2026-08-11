package com.infoprodutos.api.commerce;

import com.infoprodutos.api.commerce.domain.CommerceOrder;
import com.infoprodutos.api.commerce.domain.OrderKind;
import com.infoprodutos.api.commerce.domain.OrderStatus;
import com.infoprodutos.api.commerce.domain.ProductPackage;
import com.infoprodutos.api.commerce.dto.CheckoutSessionRequest;
import com.infoprodutos.api.commerce.dto.CheckoutSessionResponse;
import com.infoprodutos.api.commerce.dto.OrderStatusResponse;
import com.infoprodutos.api.commerce.mp.MercadoPagoClient;
import com.infoprodutos.api.commerce.mp.MercadoPagoClientConfig;
import com.infoprodutos.api.commerce.repository.CommerceOrderRepository;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.config.ApiUrlProperties;
import com.infoprodutos.api.config.AppUrlProperties;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.CourseStatus;
import com.infoprodutos.api.course.repository.CourseRepository;
import com.infoprodutos.api.enrollment.EnrollmentService;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final CommerceOrderRepository orderRepository;
    private final CourseRepository courseRepository;
    private final PackageService packageService;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;
    private final MercadoPagoClient mercadoPagoClient;
    private final AppUrlProperties frontendProperties;
    private final ApiUrlProperties apiUrlProperties;

    @Transactional
    public CheckoutSessionResponse createSession(CheckoutSessionRequest request, CustomUserDetails principal) {
        boolean hasCourse = request.courseId() != null;
        boolean hasPackage = request.packageId() != null;
        if (hasCourse == hasPackage) {
            throw new BadRequestException("Informe courseId ou packageId (apenas um).");
        }

        User buyer = userRepository
                .findById(principal.getId())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        CommerceOrder order = new CommerceOrder();
        order.setBuyer(buyer);
        order.setStatus(OrderStatus.PENDING);
        order.setIdempotencyKey(UUID.randomUUID().toString());
        order.setCurrency("BRL");

        String title;
        if (hasCourse) {
            Course course = courseRepository
                    .findActiveById(request.courseId())
                    .orElseThrow(() -> new NotFoundException("Curso não encontrado."));
            if (course.getStatus() != CourseStatus.PUBLISHED) {
                throw new BadRequestException("Somente cursos publicados podem ser comprados.");
            }
            if (course.getPriceCents() <= 0) {
                throw new BadRequestException("Este curso não está disponível para compra online.");
            }
            if (enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                    buyer.getId(), course.getId(), EnrollmentStatus.ACTIVE)) {
                throw new BadRequestException("Você já tem acesso a este curso.");
            }
            order.setKind(OrderKind.COURSE);
            order.setCourse(course);
            order.setAmountCents(course.getPriceCents());
            order.addItem(course);
            title = course.getTitle();
        } else {
            ProductPackage pkg = packageService.findActiveOrThrow(request.packageId());
            if (pkg.getPriceCents() <= 0) {
                throw new BadRequestException("Este pacote não está disponível para compra online.");
            }
            order.setKind(OrderKind.PACKAGE);
            order.setProductPackage(pkg);
            order.setAmountCents(pkg.getPriceCents());
            for (Course course : pkg.getCourses()) {
                order.addItem(course);
            }
            title = pkg.getTitle();
        }

        order = orderRepository.save(order);

        String front = trimSlash(frontendProperties.getBaseUrl());
        String api = trimSlash(apiUrlProperties.getPublicBaseUrl());
        String returnBase = front + "/checkout/return?orderId=" + order.getId();
        var preference = mercadoPagoClient.createPreference(new MercadoPagoClient.PreferenceRequest(
                order.getId().toString(),
                title,
                order.getAmountCents(),
                order.getCurrency(),
                buyer.getEmail(),
                returnBase + "&status=success",
                returnBase + "&status=failure",
                returnBase + "&status=pending",
                api + "/api/v1/payments/mercadopago/webhook"));

        order.setMpPreferenceId(preference.preferenceId());
        orderRepository.save(order);

        boolean mock = mercadoPagoClient instanceof MercadoPagoClientConfig.MockClient;
        String init = preference.initPoint() != null ? preference.initPoint() : preference.sandboxInitPoint();
        return new CheckoutSessionResponse(
                order.getId().toString(), init, preference.sandboxInitPoint(), mock);
    }

    @Transactional(readOnly = true)
    public OrderStatusResponse getOrder(UUID orderId, CustomUserDetails principal) {
        CommerceOrder order = orderRepository
                .findByIdAndBuyerIdWithDetails(orderId, principal.getId())
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado."));
        return OrderStatusResponse.from(order);
    }

    /**
     * Consulta o Mercado Pago pelo external_reference do pedido e libera o curso se houver
     * pagamento aprovado. Necessário em localhost (webhook não chega) e como fallback.
     */
    @Transactional
    public OrderStatusResponse syncFromMercadoPago(UUID orderId, CustomUserDetails principal) {
        CommerceOrder order = orderRepository
                .findByIdAndBuyerIdWithDetails(orderId, principal.getId())
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado."));
        return syncOrderIfPaid(order);
    }

    /**
     * Sincroniza todos os pedidos PENDING do comprador com o Mercado Pago.
     * Chamado ao abrir Meus cursos / Descobrir após pagar sem voltar pela return URL.
     */
    @Transactional
    public List<OrderStatusResponse> syncPendingOrders(CustomUserDetails principal) {
        if (mercadoPagoClient instanceof MercadoPagoClientConfig.MockClient) {
            return List.of();
        }
        List<CommerceOrder> pending = orderRepository.findPendingByBuyerIdWithDetails(principal.getId());
        List<OrderStatusResponse> synced = new java.util.ArrayList<>();
        for (CommerceOrder order : pending) {
            OrderStatusResponse after = syncOrderIfPaid(order);
            if (after.status().equals(OrderStatus.APPROVED.name())) {
                synced.add(after);
            }
        }
        return synced;
    }

    private OrderStatusResponse syncOrderIfPaid(CommerceOrder order) {
        if (order.getStatus() == OrderStatus.APPROVED) {
            return OrderStatusResponse.from(order);
        }
        if (mercadoPagoClient instanceof MercadoPagoClientConfig.MockClient) {
            return OrderStatusResponse.from(order);
        }

        var payments = mercadoPagoClient.searchPaymentsByExternalReference(order.getId().toString());
        for (var payment : payments) {
            if (payment.isApproved()) {
                if (approveIfConsistent(order, payment)) {
                    return OrderStatusResponse.from(
                            orderRepository.findByIdWithDetails(order.getId()).orElse(order));
                }
                continue;
            }
            if (payment.isRejected() && order.getStatus() == OrderStatus.PENDING) {
                order.setMpPaymentId(payment.paymentId());
                order.setStatus(OrderStatus.REJECTED);
                orderRepository.save(order);
            }
        }
        return OrderStatusResponse.from(orderRepository.findByIdWithDetails(order.getId()).orElse(order));
    }

    @Transactional
    public void handleMercadoPagoWebhook(String paymentId, String topic) {
        if (paymentId == null || paymentId.isBlank()) {
            return;
        }
        if (topic != null
                && !topic.isBlank()
                && !"payment".equalsIgnoreCase(topic)
                && !"merchant_order".equalsIgnoreCase(topic)) {
            log.debug("Webhook MP ignorado topic={}", topic);
            return;
        }

        MercadoPagoClient.PaymentResult payment = mercadoPagoClient.fetchPayment(paymentId);
        if (payment.externalReference() == null || payment.externalReference().isBlank()) {
            log.warn("Pagamento MP sem external_reference: {}", paymentId);
            return;
        }

        UUID orderId;
        try {
            orderId = UUID.fromString(payment.externalReference());
        } catch (IllegalArgumentException e) {
            log.warn("external_reference inválido: {}", payment.externalReference());
            return;
        }

        CommerceOrder order = orderRepository
                .findLockedById(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado para o pagamento."));

        if (order.getStatus() == OrderStatus.APPROVED) {
            return;
        }

        if (payment.isApproved()) {
            approveIfConsistent(order, payment);
        } else if (payment.isRejected()) {
            order.setMpPaymentId(payment.paymentId());
            order.setStatus(OrderStatus.REJECTED);
            orderRepository.save(order);
        } else {
            order.setMpPaymentId(payment.paymentId());
            orderRepository.save(order);
        }
    }

    /**
     * Dev/mock: aprova o pedido do comprador sem Mercado Pago real.
     */
    @Transactional
    public OrderStatusResponse simulatePayment(UUID orderId, CustomUserDetails principal) {
        if (!(mercadoPagoClient instanceof MercadoPagoClientConfig.MockClient mock)) {
            throw new ForbiddenOperationException("Simulação disponível apenas sem MP_ACCESS_TOKEN.");
        }
        CommerceOrder order = orderRepository
                .findByIdAndBuyerIdWithDetails(orderId, principal.getId())
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado."));
        if (order.getStatus() == OrderStatus.APPROVED) {
            return OrderStatusResponse.from(order);
        }
        String paymentId = "mock-pay-" + UUID.randomUUID();
        mock.approve(paymentId, order.getId().toString(), order.getMpPreferenceId(), order.getAmountCents());
        approveAndGrant(order, paymentId);
        return OrderStatusResponse.from(orderRepository.findByIdWithDetails(orderId).orElse(order));
    }

    private void approveAndGrant(CommerceOrder order, String paymentId) {
        order.setStatus(OrderStatus.APPROVED);
        order.setMpPaymentId(paymentId);
        orderRepository.save(order);
        for (var item : order.getItems()) {
            enrollmentService.grantFromPurchase(
                    order.getBuyer().getId(), item.getCourse().getId(), order.getId());
        }
        log.info(
                "Pedido {} aprovado (payment={}); {} curso(s) liberado(s)",
                order.getId(),
                paymentId,
                order.getItems().size());
    }

    private boolean approveIfConsistent(CommerceOrder order, MercadoPagoClient.PaymentResult payment) {
        if (payment.transactionAmountCents() != order.getAmountCents()) {
            log.error(
                    "Pagamento MP {} não confere com pedido {}: valor recebido={} esperado={}",
                    payment.paymentId(),
                    order.getId(),
                    payment.transactionAmountCents(),
                    order.getAmountCents());
            return false;
        }
        if (payment.currency() == null || !order.getCurrency().equalsIgnoreCase(payment.currency())) {
            log.error(
                    "Pagamento MP {} não confere com pedido {}: moeda recebida={} esperada={}",
                    payment.paymentId(),
                    order.getId(),
                    payment.currency(),
                    order.getCurrency());
            return false;
        }
        if (payment.preferenceId() != null
                && !payment.preferenceId().isBlank()
                && order.getMpPreferenceId() != null
                && !order.getMpPreferenceId().equals(payment.preferenceId())) {
            log.error(
                    "Pagamento MP {} não confere com pedido {}: preferência recebida={} esperada={}",
                    payment.paymentId(),
                    order.getId(),
                    payment.preferenceId(),
                    order.getMpPreferenceId());
            return false;
        }
        var paymentOwner = orderRepository.findByMpPaymentId(payment.paymentId());
        if (paymentOwner.isPresent() && !paymentOwner.get().getId().equals(order.getId())) {
            log.error(
                    "Pagamento MP {} já está associado ao pedido {} e não pode aprovar {}",
                    payment.paymentId(),
                    paymentOwner.get().getId(),
                    order.getId());
            return false;
        }
        approveAndGrant(order, payment.paymentId());
        return true;
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}

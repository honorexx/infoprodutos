package com.infoprodutos.api.commerce;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.commerce.domain.CommerceOrder;
import com.infoprodutos.api.commerce.domain.OrderKind;
import com.infoprodutos.api.commerce.domain.OrderStatus;
import com.infoprodutos.api.commerce.mp.MercadoPagoClient;
import com.infoprodutos.api.commerce.repository.CommerceOrderRepository;
import com.infoprodutos.api.config.ApiUrlProperties;
import com.infoprodutos.api.config.AppUrlProperties;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.repository.CourseRepository;
import com.infoprodutos.api.enrollment.EnrollmentService;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private CommerceOrderRepository orderRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private PackageService packageService;
    @Mock private UserRepository userRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private EnrollmentService enrollmentService;
    @Mock private MercadoPagoClient mercadoPagoClient;

    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService(
                orderRepository,
                courseRepository,
                packageService,
                userRepository,
                enrollmentRepository,
                enrollmentService,
                mercadoPagoClient,
                new AppUrlProperties(),
                new ApiUrlProperties());
    }

    @Test
    void approvedPaymentWithMatchingAmountCurrencyAndPreferenceGrantsCourse() {
        CommerceOrder order = order(49_700L);
        MercadoPagoClient.PaymentResult payment =
                new MercadoPagoClient.PaymentResult("pay-1", "approved", order.getId().toString(), "pref-1", 49_700L, "BRL");
        when(mercadoPagoClient.fetchPayment("pay-1")).thenReturn(payment);
        when(orderRepository.findLockedById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.findByMpPaymentId("pay-1")).thenReturn(Optional.empty());

        service.handleMercadoPagoWebhook("pay-1", "payment");

        verify(enrollmentService).grantFromPurchase(order.getBuyer().getId(), order.getCourse().getId(), order.getId());
        verify(orderRepository).save(order);
        org.assertj.core.api.Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
    }

    @Test
    void approvedPaymentWithDifferentAmountDoesNotGrantCourse() {
        CommerceOrder order = order(49_700L);
        MercadoPagoClient.PaymentResult payment =
                new MercadoPagoClient.PaymentResult("pay-2", "approved", order.getId().toString(), "pref-1", 100L, "BRL");
        when(mercadoPagoClient.fetchPayment("pay-2")).thenReturn(payment);
        when(orderRepository.findLockedById(order.getId())).thenReturn(Optional.of(order));

        service.handleMercadoPagoWebhook("pay-2", "payment");

        verify(enrollmentService, never()).grantFromPurchase(any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void approvedPaymentWithDifferentPreferenceDoesNotGrantCourse() {
        CommerceOrder order = order(49_700L);
        MercadoPagoClient.PaymentResult payment =
                new MercadoPagoClient.PaymentResult("pay-3", "approved", order.getId().toString(), "other-pref", 49_700L, "BRL");
        when(mercadoPagoClient.fetchPayment("pay-3")).thenReturn(payment);
        when(orderRepository.findLockedById(order.getId())).thenReturn(Optional.of(order));

        service.handleMercadoPagoWebhook("pay-3", "payment");

        verify(enrollmentService, never()).grantFromPurchase(any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    private static CommerceOrder order(long amountCents) {
        User buyer = new User("Aluno Teste", "aluno@example.com", "hash");
        setId(buyer);
        Course course = new Course("Curso", "curso", buyer);
        setId(course);
        CommerceOrder order = new CommerceOrder();
        setId(order);
        order.setBuyer(buyer);
        order.setKind(OrderKind.COURSE);
        order.setCourse(course);
        order.setAmountCents(amountCents);
        order.setCurrency("BRL");
        order.setStatus(OrderStatus.PENDING);
        order.setMpPreferenceId("pref-1");
        order.setIdempotencyKey(UUID.randomUUID().toString());
        order.addItem(course);
        return order;
    }

    private static void setId(com.infoprodutos.api.common.domain.BaseEntity entity) {
        try {
            var field = com.infoprodutos.api.common.domain.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

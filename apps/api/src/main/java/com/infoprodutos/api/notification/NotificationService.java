package com.infoprodutos.api.notification;

import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.config.AppUrlProperties;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.CourseInstructor;
import com.infoprodutos.api.course.repository.CourseInstructorRepository;
import com.infoprodutos.api.mail.AppMailer;
import com.infoprodutos.api.notification.domain.Notification;
import com.infoprodutos.api.notification.domain.NotificationType;
import com.infoprodutos.api.notification.dto.NotificationListResponse;
import com.infoprodutos.api.notification.dto.NotificationResponse;
import com.infoprodutos.api.notification.repository.NotificationRepository;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final AppMailer appMailer;
    private final UserRepository userRepository;
    private final AppUrlProperties appUrlProperties;

    @Transactional
    public void notifyWelcome(User user) {
        create(
                user.getId(),
                NotificationType.WELCOME,
                "Bem-vindo à PKS Consultoria",
                "Parabéns pela escolha — sua conta está pronta. Explore os cursos e comece quando quiser.",
                "/my-courses");
    }

    /**
     * Boas-vindas de retorno: no máximo 1x por dia, e só em logins seguintes ao primeiro.
     */
    @Transactional
    public void notifyWelcomeBackIfDue(User user, boolean wasReturningUser) {
        if (!wasReturningUser) {
            return;
        }
        Instant since = Instant.now().minus(20, ChronoUnit.HOURS);
        if (notificationRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                user.getId(), NotificationType.WELCOME_BACK, since)) {
            return;
        }
        create(
                user.getId(),
                NotificationType.WELCOME_BACK,
                "Que bom ter você de volta",
                "Você entrou na plataforma. Continue de onde parou — seus cursos estão te esperando.",
                "/dashboard");
    }

    @Transactional
    public void notifyEnrollmentGranted(User student, Course course) {
        String studentTitle = "Acesso liberado";
        String studentBody =
                "Você foi matriculado em \"" + course.getTitle() + "\". O conteúdo já está disponível.";
        String studentLink = "/my-courses/" + course.getId();
        create(student.getId(), NotificationType.ENROLLMENT_STUDENT, studentTitle, studentBody, studentLink);
        sendEmailSafely(student.getId(), studentTitle, studentBody, studentLink);

        List<CourseInstructor> instructors = courseInstructorRepository.findByCourseId(course.getId());
        for (CourseInstructor ci : instructors) {
            User instructor = ci.getInstructor();
            if (instructor == null || instructor.getId().equals(student.getId())) {
                continue;
            }
            String instructorTitle = "Novo aluno no seu curso";
            String instructorBody =
                    student.getName() + " acabou de garantir acesso a \"" + course.getTitle() + "\".";
            String instructorLink = "/courses/" + course.getId();
            create(
                    instructor.getId(),
                    NotificationType.ENROLLMENT_INSTRUCTOR,
                    instructorTitle,
                    instructorBody,
                    instructorLink);
            sendEmailSafely(instructor.getId(), instructorTitle, instructorBody, instructorLink);
        }
    }

    @Transactional
    public void notifyCertificateIssued(User student, String courseTitle, UUID certificateId) {
        String title = "Certificado emitido";
        String body = "Parabéns! Seu certificado de \"" + courseTitle + "\" está pronto para download.";
        String link = "/my-certificates/" + certificateId;
        create(student.getId(), NotificationType.CERTIFICATE_ISSUED, title, body, link);
        sendEmailSafely(student.getId(), title, body, link);
    }

    @Transactional(readOnly = true)
    public NotificationListResponse listMine(UUID userId, int size) {
        int limit = Math.min(Math.max(size, 1), 50);
        List<NotificationResponse> items = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(NotificationResponse::from)
                .toList();
        long unread = notificationRepository.countByUserIdAndReadAtIsNull(userId);
        return new NotificationListResponse(unread, items);
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        Notification n = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada."));
        if (!n.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("Notificação de outro usuário.");
        }
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId, Instant.now());
    }

    private void create(UUID userId, String type, String title, String body, String linkHref) {
        notificationRepository.save(new Notification(userId, type, truncate(title, 200), truncate(body, 500), linkHref));
    }

    /** Envia e-mail após a notificação in-app; falhas não afetam o commit da transação. */
    private void sendEmailSafely(UUID userId, String subject, String body, String linkHref) {
        try {
            userRepository
                    .findById(userId)
                    .map(User::getEmail)
                    .filter(email -> email != null && !email.isBlank())
                    .ifPresentOrElse(
                            email -> {
                                String absoluteLink = absoluteLink(linkHref);
                                String text = body
                                        + (absoluteLink.isEmpty() ? "" : "\n\n" + absoluteLink);
                                String html = "<p>"
                                        + escapeHtml(body)
                                        + "</p>"
                                        + (absoluteLink.isEmpty()
                                                ? ""
                                                : "<p><a href=\"" + absoluteLink + "\">Abrir</a></p>");
                                appMailer.send(email, subject, text, html);
                            },
                            () -> log.warn("E-mail de notificação omitido: usuário {} sem e-mail", userId));
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de notificação userId={} subject={}: {}", userId, subject, e.toString());
        }
    }

    private String absoluteLink(String linkHref) {
        if (linkHref == null || linkHref.isBlank()) {
            return "";
        }
        String base = appUrlProperties.getBaseUrl();
        if (base == null || base.isBlank()) {
            return linkHref;
        }
        if (linkHref.startsWith("http://") || linkHref.startsWith("https://")) {
            return linkHref;
        }
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return linkHref.startsWith("/") ? normalizedBase + linkHref : normalizedBase + "/" + linkHref;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}

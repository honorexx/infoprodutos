package com.infoprodutos.api.notification;

import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.CourseInstructor;
import com.infoprodutos.api.course.repository.CourseInstructorRepository;
import com.infoprodutos.api.notification.domain.Notification;
import com.infoprodutos.api.notification.domain.NotificationType;
import com.infoprodutos.api.notification.dto.NotificationListResponse;
import com.infoprodutos.api.notification.dto.NotificationResponse;
import com.infoprodutos.api.notification.repository.NotificationRepository;
import com.infoprodutos.api.user.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CourseInstructorRepository courseInstructorRepository;

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
        create(
                student.getId(),
                NotificationType.ENROLLMENT_STUDENT,
                "Acesso liberado",
                "Você foi matriculado em \"" + course.getTitle() + "\". O conteúdo já está disponível.",
                "/my-courses/" + course.getId());

        List<CourseInstructor> instructors = courseInstructorRepository.findByCourseId(course.getId());
        for (CourseInstructor ci : instructors) {
            User instructor = ci.getInstructor();
            if (instructor == null || instructor.getId().equals(student.getId())) {
                continue;
            }
            create(
                    instructor.getId(),
                    NotificationType.ENROLLMENT_INSTRUCTOR,
                    "Novo aluno no seu curso",
                    student.getName() + " acabou de garantir acesso a \"" + course.getTitle() + "\".",
                    "/courses/" + course.getId());
        }
    }

    @Transactional
    public void notifyCertificateIssued(User student, String courseTitle, UUID certificateId) {
        create(
                student.getId(),
                NotificationType.CERTIFICATE_ISSUED,
                "Certificado emitido",
                "Parabéns! Seu certificado de \"" + courseTitle + "\" está pronto para download.",
                "/my-certificates/" + certificateId);
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

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}

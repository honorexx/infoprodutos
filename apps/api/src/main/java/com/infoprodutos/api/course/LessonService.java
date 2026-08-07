package com.infoprodutos.api.course;

import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.LessonStatus;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.dto.LessonRequest;
import com.infoprodutos.api.course.dto.LessonResponse;
import com.infoprodutos.api.course.dto.ReorderRequest;
import com.infoprodutos.api.course.repository.LessonRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.infoprodutos.api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleService moduleService;
    private final CourseService courseService;
    private final CourseAccessGuard accessGuard;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<LessonResponse> list(UUID moduleId, CustomUserDetails principal) {
        Module module = moduleService.findActiveOrThrow(moduleId);
        courseService.requireViewAccess(module.getCourse(), principal);
        return lessonRepository.findAllActiveByModuleOrderByOrderIndex(moduleId).stream()
                .map(LessonResponse::from)
                .toList();
    }

    @Transactional
    public LessonResponse create(UUID moduleId, LessonRequest request, CustomUserDetails principal) {
        Module module = moduleService.findActiveOrThrow(moduleId);
        accessGuard.requireManageAccess(module.getCourse().getId(), principal);

        int nextOrder = lessonRepository.findMaxOrderIndexByModule(moduleId) + 1;
        Lesson lesson = new Lesson(module, request.title().trim(), nextOrder);
        lesson.setDescription(request.description());
        lesson.setDurationSeconds(request.durationSeconds());
        lesson.setAccessType(request.accessType());
        lesson = lessonRepository.save(lesson);

        auditService.record(principal.getId(), "LESSON_CREATED", "Lesson", lesson.getId(), null);
        return LessonResponse.from(lesson);
    }

    @Transactional
    public LessonResponse update(UUID lessonId, LessonRequest request, CustomUserDetails principal) {
        Lesson lesson = findActiveOrThrow(lessonId);
        accessGuard.requireManageAccess(lesson.getModule().getCourse().getId(), principal);

        lesson.setTitle(request.title().trim());
        lesson.setDescription(request.description());
        lesson.setDurationSeconds(request.durationSeconds());
        lesson.setAccessType(request.accessType());
        lesson = lessonRepository.save(lesson);

        auditService.record(principal.getId(), "LESSON_UPDATED", "Lesson", lesson.getId(), null);
        return LessonResponse.from(lesson);
    }

    @Transactional
    public void delete(UUID lessonId, CustomUserDetails principal) {
        Lesson lesson = findActiveOrThrow(lessonId);
        accessGuard.requireManageAccess(lesson.getModule().getCourse().getId(), principal);

        // Regra de progresso de aluno (docs/API.md §2.5) será aplicada quando
        // LessonProgress existir (Fase 4); por ora, exclusão é sempre permitida.
        lesson.setDeletedAt(Instant.now());
        lessonRepository.save(lesson);
        auditService.record(principal.getId(), "LESSON_DELETED", "Lesson", lesson.getId(), null);
    }

    @Transactional
    public void publish(UUID lessonId, CustomUserDetails principal) {
        Lesson lesson = findActiveOrThrow(lessonId);
        accessGuard.requireManageAccess(lesson.getModule().getCourse().getId(), principal);
        if (lesson.getStatus() == LessonStatus.PUBLISHED) {
            return;
        }
        lesson.setStatus(LessonStatus.PUBLISHED);
        lessonRepository.save(lesson);
        auditService.record(principal.getId(), "LESSON_PUBLISHED", "Lesson", lesson.getId(), null);
    }

    @Transactional
    public void reorder(UUID moduleId, ReorderRequest request, CustomUserDetails principal) {
        Module module = moduleService.findActiveOrThrow(moduleId);
        accessGuard.requireManageAccess(module.getCourse().getId(), principal);

        List<Lesson> current = lessonRepository.findAllActiveByModuleOrderByOrderIndex(moduleId);
        Set<UUID> currentIds = new HashSet<>();
        current.forEach(l -> currentIds.add(l.getId()));
        Set<UUID> requestedIds = new HashSet<>(request.orderedIds());

        if (!currentIds.equals(requestedIds)) {
            throw new BadRequestException("A lista de aulas informada não corresponde às aulas ativas do módulo.");
        }

        for (int i = 0; i < request.orderedIds().size(); i++) {
            UUID id = request.orderedIds().get(i);
            Lesson lesson = current.stream().filter(l -> l.getId().equals(id)).findFirst().orElseThrow();
            lesson.setOrderIndex(i);
        }
        lessonRepository.saveAll(current);
        auditService.record(principal.getId(), "LESSON_REORDERED", "Module", moduleId, null);
    }

    public Lesson findActiveOrThrow(UUID id) {
        return lessonRepository.findActiveById(id).orElseThrow(() -> new NotFoundException("Aula não encontrada."));
    }
}

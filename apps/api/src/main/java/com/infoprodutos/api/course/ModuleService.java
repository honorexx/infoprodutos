package com.infoprodutos.api.course;

import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.domain.ModuleStatus;
import com.infoprodutos.api.course.dto.LessonResponse;
import com.infoprodutos.api.course.dto.ModuleRequest;
import com.infoprodutos.api.course.dto.ModuleResponse;
import com.infoprodutos.api.course.dto.ReorderRequest;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.course.repository.ModuleRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final CourseService courseService;
    private final CourseAccessGuard accessGuard;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ModuleResponse> list(UUID courseId, CustomUserDetails principal) {
        Course course = courseService.findActiveOrThrow(courseId);
        courseService.requireViewAccess(course, principal);
        return moduleRepository.findAllActiveByCourseOrderByOrderIndex(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ModuleResponse create(UUID courseId, ModuleRequest request, CustomUserDetails principal) {
        Course course = courseService.findActiveOrThrow(courseId);
        accessGuard.requireManageAccess(courseId, principal);

        int nextOrder = moduleRepository.findMaxOrderIndexByCourse(courseId) + 1;
        Module module = new Module(course, request.title().trim(), nextOrder);
        module.setDescription(request.description());
        module = moduleRepository.save(module);

        auditService.record(principal.getId(), "MODULE_CREATED", "Module", module.getId(), null);
        return ModuleResponse.from(module, List.of());
    }

    @Transactional
    public ModuleResponse update(UUID moduleId, ModuleRequest request, CustomUserDetails principal) {
        Module module = findActiveOrThrow(moduleId);
        accessGuard.requireManageAccess(module.getCourse().getId(), principal);

        module.setTitle(request.title().trim());
        module.setDescription(request.description());
        module = moduleRepository.save(module);

        auditService.record(principal.getId(), "MODULE_UPDATED", "Module", module.getId(), null);
        List<LessonResponse> lessons = lessonRepository.findAllActiveByModuleOrderByOrderIndex(moduleId).stream()
                .map(LessonResponse::from)
                .toList();
        return ModuleResponse.from(module, lessons);
    }

    @Transactional
    public void delete(UUID moduleId, CustomUserDetails principal) {
        Module module = findActiveOrThrow(moduleId);
        accessGuard.requireManageAccess(module.getCourse().getId(), principal);

        // Regra de progresso de aluno (docs/API.md §2.4) será aplicada quando
        // LessonProgress existir (Fase 4); por ora, exclusão é sempre permitida.
        module.setDeletedAt(Instant.now());
        moduleRepository.save(module);
        auditService.record(principal.getId(), "MODULE_DELETED", "Module", module.getId(), null);
    }

    @Transactional
    public void publish(UUID moduleId, CustomUserDetails principal) {
        Module module = findActiveOrThrow(moduleId);
        accessGuard.requireManageAccess(module.getCourse().getId(), principal);
        if (module.getStatus() == ModuleStatus.PUBLISHED) {
            return;
        }
        module.setStatus(ModuleStatus.PUBLISHED);
        moduleRepository.save(module);
        auditService.record(principal.getId(), "MODULE_PUBLISHED", "Module", module.getId(), null);
    }

    @Transactional
    public void reorder(UUID courseId, ReorderRequest request, CustomUserDetails principal) {
        courseService.findActiveOrThrow(courseId);
        accessGuard.requireManageAccess(courseId, principal);

        List<Module> current = moduleRepository.findAllActiveByCourseOrderByOrderIndex(courseId);
        Set<UUID> currentIds = new HashSet<>();
        current.forEach(m -> currentIds.add(m.getId()));
        Set<UUID> requestedIds = new HashSet<>(request.orderedIds());

        if (!currentIds.equals(requestedIds)) {
            throw new BadRequestException("A lista de módulos informada não corresponde aos módulos ativos do curso.");
        }

        for (int i = 0; i < request.orderedIds().size(); i++) {
            UUID id = request.orderedIds().get(i);
            Module module = current.stream().filter(m -> m.getId().equals(id)).findFirst().orElseThrow();
            module.setOrderIndex(i);
        }
        moduleRepository.saveAll(current);
        auditService.record(principal.getId(), "MODULE_REORDERED", "Course", courseId, null);
    }

    Module findActiveOrThrow(UUID id) {
        return moduleRepository.findActiveById(id).orElseThrow(() -> new NotFoundException("Módulo não encontrado."));
    }

    private ModuleResponse toResponse(Module module) {
        List<LessonResponse> lessons = lessonRepository.findAllActiveByModuleOrderByOrderIndex(module.getId()).stream()
                .map(LessonResponse::from)
                .toList();
        return ModuleResponse.from(module, lessons);
    }
}

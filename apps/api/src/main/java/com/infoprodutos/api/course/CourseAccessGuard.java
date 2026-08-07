package com.infoprodutos.api.course;

import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.course.repository.CourseInstructorRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.RoleCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Centraliza a checagem de "posse" (ownership) de curso, usada por
 * CourseService/ModuleService/LessonService antes de qualquer escrita.
 * Autorização por papel (@PreAuthorize nos controllers) é a primeira camada;
 * esta classe é a segunda camada, obrigatória para papel INSTRUCTOR
 * (docs/SECURITY.md - autorização nunca depende só do papel, mas também de
 * posse quando aplicável).
 */
@Component
@RequiredArgsConstructor
public class CourseAccessGuard {

    private final CourseInstructorRepository courseInstructorRepository;

    /** Lança exceção se o usuário autenticado não puder gerenciar (editar/excluir/publicar) o curso. */
    public void requireManageAccess(UUID courseId, CustomUserDetails principal) {
        if (canManage(courseId, principal)) {
            return;
        }
        throw new ForbiddenOperationException("Você não tem permissão para gerenciar este curso.");
    }

    public boolean canManage(UUID courseId, CustomUserDetails principal) {
        if (principal.getRoleCodes().contains(RoleCode.SUPER_ADMIN)) {
            return true;
        }
        return principal.getRoleCodes().contains(RoleCode.INSTRUCTOR)
                && courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, principal.getId());
    }
}

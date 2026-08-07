package com.infoprodutos.api.course.dto;

import com.infoprodutos.api.course.domain.Module;
import java.util.List;

public record ModuleResponse(
        String id,
        String courseId,
        String title,
        String description,
        int orderIndex,
        String status,
        List<LessonResponse> lessons) {

    public static ModuleResponse from(Module module, List<LessonResponse> lessons) {
        return new ModuleResponse(
                module.getId().toString(),
                module.getCourse().getId().toString(),
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                module.getStatus().name(),
                lessons);
    }
}

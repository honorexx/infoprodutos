package com.infoprodutos.api.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateEnrollmentRequest(
        @NotNull UUID courseId,
        UUID studentUserId,
        String studentEmail) {}

package com.infoprodutos.api.course.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddInstructorRequest(@NotNull(message = "instructorUserId é obrigatório") UUID instructorUserId, boolean primary) {}

package com.infoprodutos.api.course.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderRequest(@NotEmpty(message = "lista de ids é obrigatória") List<UUID> orderedIds) {}

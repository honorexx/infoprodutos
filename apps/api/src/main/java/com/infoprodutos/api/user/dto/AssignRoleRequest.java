package com.infoprodutos.api.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(@NotBlank(message = "roleCode é obrigatório") String roleCode) {}

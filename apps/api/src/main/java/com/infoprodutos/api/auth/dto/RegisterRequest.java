package com.infoprodutos.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro público. Sempre cria um usuário com papel STUDENT
 * (docs/DECISIONS.md - pergunta aberta #1, proposta padrão adotada).
 */
public record RegisterRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
                String name,
        @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido") @Size(max = 255)
                String email,
        @NotBlank(message = "senha é obrigatória")
                @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
                String password) {}

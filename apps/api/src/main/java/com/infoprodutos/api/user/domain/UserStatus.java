package com.infoprodutos.api.user.domain;

/**
 * ACTIVE: usuário pode autenticar normalmente.
 * BLOCKED: bloqueio lógico - impede login, mas preserva todo o histórico
 * acadêmico/de auditoria (docs/PRD.md secao 3.1, docs/DATABASE.md secao 3).
 */
public enum UserStatus {
    ACTIVE,
    BLOCKED
}

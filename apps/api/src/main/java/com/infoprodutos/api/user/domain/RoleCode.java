package com.infoprodutos.api.user.domain;

/**
 * Códigos de papel seedados via Flyway (V2__seed_roles.sql). A entidade
 * {@link Role} é uma tabela de referência (não um enum de banco), permitindo
 * evolução futura sem migração destrutiva - ver docs/DATABASE.md secao 5.2.
 */
public final class RoleCode {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String INSTRUCTOR = "INSTRUCTOR";
    public static final String STUDENT = "STUDENT";

    private RoleCode() {}
}

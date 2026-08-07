package com.infoprodutos.api.common.web;

import java.time.Instant;
import java.util.List;

/**
 * Formato padronizado de erro (inspirado em RFC 7807 - Problem Details),
 * conforme docs/API.md secao 1. Nunca inclui stack trace ou detalhes
 * internos de implementação.
 */
public record ErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp,
        String correlationId,
        List<FieldError> errors) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(
            String type, String title, int status, String detail, String instance, String correlationId) {
        return new ErrorResponse(type, title, status, detail, instance, Instant.now(), correlationId, List.of());
    }

    public static ErrorResponse of(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String correlationId,
            List<FieldError> errors) {
        return new ErrorResponse(type, title, status, detail, instance, Instant.now(), correlationId, errors);
    }
}

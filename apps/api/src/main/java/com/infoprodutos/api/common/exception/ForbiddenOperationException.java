package com.infoprodutos.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Usada para negações de autorização baseadas em posse/regra de negócio
 * (ex.: instrutor tentando operar em curso de outro instrutor), distintas
 * da negação por ausência de papel (tratada pelo Spring Security).
 */
public class ForbiddenOperationException extends ApiException {

    public ForbiddenOperationException(String safeMessage) {
        super(HttpStatus.FORBIDDEN, "forbidden-operation", safeMessage);
    }
}

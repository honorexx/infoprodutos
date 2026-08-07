package com.infoprodutos.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção base para erros de negócio conhecidos e esperados.
 * Sempre carrega uma mensagem segura, adequada para ser exibida ao usuário final.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorType;

    protected ApiException(HttpStatus status, String errorType, String safeMessage) {
        super(safeMessage);
        this.status = status;
        this.errorType = errorType;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorType() {
        return errorType;
    }
}

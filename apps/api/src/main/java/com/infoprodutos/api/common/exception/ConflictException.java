package com.infoprodutos.api.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String safeMessage) {
        super(HttpStatus.CONFLICT, "conflict", safeMessage);
    }
}

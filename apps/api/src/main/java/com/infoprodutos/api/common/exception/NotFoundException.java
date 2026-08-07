package com.infoprodutos.api.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String safeMessage) {
        super(HttpStatus.NOT_FOUND, "not-found", safeMessage);
    }
}

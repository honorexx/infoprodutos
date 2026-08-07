package com.infoprodutos.api.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String safeMessage) {
        super(HttpStatus.BAD_REQUEST, "bad-request", safeMessage);
    }
}

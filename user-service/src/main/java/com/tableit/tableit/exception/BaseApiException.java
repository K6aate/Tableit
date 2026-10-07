package com.tableit.tableit.exception;

import org.springframework.http.HttpStatus;

public abstract class BaseApiException extends RuntimeException {
    private final HttpStatus status;

    public BaseApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

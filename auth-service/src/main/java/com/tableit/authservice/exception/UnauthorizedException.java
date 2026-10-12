package com.tableit.authservice.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseApiException {
    public UnauthorizedException(String message) { super(HttpStatus.UNAUTHORIZED, message); }
}

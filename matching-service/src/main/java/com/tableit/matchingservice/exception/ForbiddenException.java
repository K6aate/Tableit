package com.tableit.matchingservice.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseApiException {
    public ForbiddenException(String message) { super(HttpStatus.FORBIDDEN, message); }
}

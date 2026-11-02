package com.tableit.userservice.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseApiException {
    public ConflictException(String message) { super(HttpStatus.CONFLICT, message); }
}

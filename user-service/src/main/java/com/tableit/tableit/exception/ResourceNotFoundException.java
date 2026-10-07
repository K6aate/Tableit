package com.tableit.tableit.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseApiException {
    public ResourceNotFoundException(String message) { super(HttpStatus.NOT_FOUND, message); }
}

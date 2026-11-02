package com.tableit.userservice.security;

import java.util.UUID;

// Claim names and values of the access tokens issued by auth-service.
public final class JwtClaims {

    public static final String SUBJECT = "sub";
    public static final String ROLE = "role";
    public static final String TOKEN_TYPE = "token_type";
    public static final String ACCESS_TOKEN_TYPE = "access";

    private JwtClaims() {
    }

    public static boolean isUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

package com.tableit.authservice.dto.auth;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}
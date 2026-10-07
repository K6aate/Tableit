package com.tableit.tableit.dto.auth;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}
package com.tableit.userservice.dto.auth;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}
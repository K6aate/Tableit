package com.tableit.tableit.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TelegramPasswordResetResponse {
    private String username;
    private String newPassword;
}

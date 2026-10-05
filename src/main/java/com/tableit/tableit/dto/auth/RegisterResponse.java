package com.tableit.tableit.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private Long userId;
    private String authLink; // Full link to Telegram (authLink "https://t.me/paltsy_eu_bot?start=auth_E2717MMB")
    private String bindingCode; // Code for param ( bindingCode "E2717MMB")
    private String accessToken;
    private String refreshToken;

}

package com.tableit.tableit.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginInitResponse {
    private String authLink; // Full link to Telegram (authLink "https://t.me/paltsy_eu_bot?start=auth_E2717MMB")
    private String bindingCode; // Code for param ( bindingCode "E2717MMB")
}

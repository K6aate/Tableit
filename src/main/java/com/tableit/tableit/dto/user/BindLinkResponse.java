package com.tableit.tableit.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BindLinkResponse {
    private String authLink; // Full link to Telegram (authLink "https://t.me/paltsy_eu_bot?start=auth_E2717MMB")
    private String bindingCode; // Code for param ( bindingCode "E2717MMB")
}

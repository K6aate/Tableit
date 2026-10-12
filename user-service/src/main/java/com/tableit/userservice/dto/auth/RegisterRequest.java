package com.tableit.userservice.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {

    private String login;
    private String password;

}

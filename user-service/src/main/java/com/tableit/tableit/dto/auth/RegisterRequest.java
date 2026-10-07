package com.tableit.tableit.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {

    private String login;
    private String password;
    private Long universityId;
    private Long facultyId;
    private Long specialtyId;

}

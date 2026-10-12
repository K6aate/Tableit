package com.tableit.userservice.controller;

import com.tableit.userservice.config.auth.RoleSecured;
import com.tableit.userservice.dto.user.UserResponse;
import com.tableit.userservice.service.UserService;
import com.tableit.userservice.util.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @RoleSecured(UserRole.USER)
    public ResponseEntity<UserResponse> getMe() {
        return ResponseEntity.ok(userService.getMe());
    }

}

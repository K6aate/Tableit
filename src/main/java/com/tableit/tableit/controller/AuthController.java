package com.tableit.tableit.controller;

import com.tableit.tableit.config.auth.RoleSecured;
import com.tableit.tableit.dto.auth.*;
import com.tableit.tableit.service.AuthService;
import com.tableit.tableit.util.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        log.info("Registration started for username={}", request.getLogin());

        RegisterResponse res = authService.register(request);

        log.info("Registration finished for username={}", request.getLogin());
        return ResponseEntity.ok(res);
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for username={}", request.getLogin());

        LoginResponse res = authService.login(request);

        log.info("Login success for username={}", request.getLogin());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        log.info("Refresh attempt");
        LoginResponse res = authService.refresh(request);
        log.info("Refresh success");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/change-password")
    @RoleSecured(UserRole.WAITING_TELEGRAM)
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        log.info("Change password attempt");

        authService.changePassword(request);

        log.info("Change password success");
        return ResponseEntity.ok().build();
    }

}

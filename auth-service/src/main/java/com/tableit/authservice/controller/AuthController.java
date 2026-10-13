package com.tableit.authservice.controller;

import com.tableit.authservice.config.auth.RoleSecured;
import com.tableit.authservice.dto.auth.*;
import com.tableit.authservice.dto.user.UserResponse;
import com.tableit.authservice.service.AuthService;
import com.tableit.authservice.service.UserService;
import com.tableit.authservice.util.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

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

    @GetMapping("/me")
    @RoleSecured(UserRole.USER)
    public ResponseEntity<UserResponse> getMe() {
        return ResponseEntity.ok(userService.getMe());
    }

    @PostMapping("/change-password")
    @RoleSecured(UserRole.USER)
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        log.info("Change password attempt");

        authService.changePassword(request);

        log.info("Change password success");
        return ResponseEntity.ok().build();
    }

}

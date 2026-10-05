package com.tableit.tableit.controller;

import com.tableit.tableit.config.auth.RoleSecured;
import com.tableit.tableit.dto.user.BindLinkResponse;
import com.tableit.tableit.dto.user.UserRequest;
import com.tableit.tableit.dto.user.UserResponse;
import com.tableit.tableit.service.UserService;
import com.tableit.tableit.util.enums.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @RoleSecured(UserRole.WAITING_TELEGRAM)
    public ResponseEntity<UserResponse> getMe() {
        return ResponseEntity.ok(userService.getMe());
    }

    @PutMapping
    @RoleSecured(UserRole.WAITING_TELEGRAM)
    public ResponseEntity<UserResponse> update(@RequestBody UserRequest request) {
        log.info("Update user Request object = {}", request);
        UserResponse userResponse = userService.update(request);

        log.info("Update success user");
        return ResponseEntity.ok(userResponse);
    }

}

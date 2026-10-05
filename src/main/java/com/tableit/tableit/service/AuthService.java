package com.tableit.tableit.service;

import com.tableit.tableit.exception.BadRequestException;
import com.tableit.tableit.exception.ForbiddenException;
import com.tableit.tableit.exception.UnauthorizedException;
import com.tableit.tableit.model.User;
import com.tableit.tableit.util.enums.Sex;
import com.tableit.tableit.util.enums.UserRole;
import com.tableit.tableit.dto.auth.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${notification.service.api-key}")
    private String apiKey;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (!isLoginValid(request.getLogin())) {
            throw new BadRequestException("Invalid username, username is required and username can contains only latin letters, digits and symbols @.!?_");
        }

        if (!isPasswordValid(request.getPassword())) {
            throw new BadRequestException("Invalid password: must be at least 8 characters long and contain no spaces.");
        }

        if (userService.existsByUsername(request.getLogin())) {
            throw new BadRequestException("User already exists");
        }

        User user = new User()
                .setUsername(request.getLogin())
                .setPassword(passwordEncoder.encode(request.getPassword().replaceAll("\\s+", "")))
                .setSex(Sex.OTHER)
                .setRole(UserRole.WAITING_TELEGRAM);

        User savedUser = userService.saveUser(user);
        log.debug("SavedUser to return: {}", savedUser);



        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        RegisterResponse response = RegisterResponse.builder()
                .userId(savedUser.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        log.debug("RegisterResponse to return: {}", response);

        return response;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
        );

        User user = userService.findByUsername(request.getLogin());

        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return new LoginResponse(user.getId(), accessToken, refreshToken, user.getRole().getName());
    }

    public UserRole getUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        String username;
        
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        
        User user = userService.findByUsername(username);
        return user.getRole();
    }

    public LoginResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token"); // 401 Unauthorized
        }
        String login = jwtService.extractUsernameFromRefreshToken(refreshToken);
        User user = userService.findByUsername(login);

        var newAccessToken = jwtService.generateAccessToken(user);
        var newRefreshToken = jwtService.generateRefreshToken(user);

        return new LoginResponse(user.getId(), newAccessToken, newRefreshToken, user.getRole().getName());
    }

    public User getMe() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        // Получить текущего пользователя
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);

        if (user == null) {
            throw new BadRequestException("User not found");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is incorrect");
        }

        // Валидировать новый пароль
        if (!isPasswordValid(request.getNewPassword())) {
            throw new BadRequestException("Invalid password: must be at least 8 characters long and contain no spaces.");
        }

        // Кодировать и сохранить новый пароль
        user.setPassword(passwordEncoder.encode(request.getNewPassword().replaceAll("\\s+", "")));
        userService.saveUser(user);

        log.info("Password changed for user={}", username);
    }

    private boolean isLoginValid(String login) {
        if (login == null) {
            return false;
        }

        return login.matches("^[A-Za-z0-9@.!?_]+$");
    }

    private boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }

        return password.length() >= 8 && !password.contains(" ");
    }


    private String generatePassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

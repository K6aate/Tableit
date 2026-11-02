package com.tableit.authservice.controller;

import com.tableit.authservice.exception.GlobalExceptionHandler;
import com.tableit.authservice.model.User;
import com.tableit.authservice.service.AuthService;
import com.tableit.authservice.service.JwtService;
import com.tableit.authservice.service.TestJwtServices;
import com.tableit.authservice.service.UserService;
import com.tableit.authservice.util.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.KeyPair;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Real controller, AuthService, JwtService and exception handler; only the database-backed collaborators are mocked.
class AuthControllerRefreshTest {

    private final UUID userId = UUID.randomUUID();
    private final User user = new User().setId(userId).setRole(UserRole.USER);
    private final UserService userService = mock(UserService.class);

    @Test
    void acceptsRefreshToken() throws Exception {
        JwtService jwtService = TestJwtServices.jwtService(TestJwtServices.newRsaKeyPair(), TestJwtServices.newRsaKeyPair());
        when(userService.findByUuid(anyString())).thenReturn(user);

        refresh(mockMvc(jwtService), jwtService.generateRefreshToken(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void rejectsAccessToken() throws Exception {
        JwtService jwtService = TestJwtServices.jwtService(TestJwtServices.newRsaKeyPair(), TestJwtServices.newRsaKeyPair());

        refresh(mockMvc(jwtService), jwtService.generateAccessToken(user))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAccessTokenEvenWhenBothTokenTypesShareOneKeyPair() throws Exception {
        // Misconfiguration case: the signature alone can no longer tell the token types apart.
        KeyPair sharedKeys = TestJwtServices.newRsaKeyPair();
        JwtService jwtService = TestJwtServices.jwtService(sharedKeys, sharedKeys);
        when(userService.findByUuid(anyString())).thenReturn(user);

        refresh(mockMvc(jwtService), jwtService.generateAccessToken(user))
                .andExpect(status().isUnauthorized());
    }

    private MockMvc mockMvc(JwtService jwtService) {
        AuthService authService = new AuthService(
                userService, mock(PasswordEncoder.class), jwtService, mock(AuthenticationManager.class));
        return MockMvcBuilders.standaloneSetup(new AuthController(authService, userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static ResultActions refresh(MockMvc mockMvc, String token) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }
}

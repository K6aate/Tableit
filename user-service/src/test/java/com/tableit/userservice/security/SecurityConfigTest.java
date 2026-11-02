package com.tableit.userservice.security;

import com.tableit.userservice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full context with real token decoding, so the production filter chain and decoder are what is tested.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityConfigTest {

    private static final String PROBE = "/api/users/test-probe";

    @DynamicPropertySource
    static void jwtKey(DynamicPropertyRegistry registry) {
        TestJwt.registerAccessPublicKey(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get(PROBE))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        callProbe(TestJwt.accessToken().signedWith(TestJwt.UNRELATED_KEYS).sign())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String expired = TestJwt.accessToken()
                .issuedAt(Instant.now().minus(Duration.ofMinutes(20)))
                .lifetime(Duration.ofMinutes(15))
                .sign();

        callProbe(expired).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMalformedSubject() throws Exception {
        callProbe(TestJwt.accessToken().subject("not-a-uuid").sign())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRefreshToken() throws Exception {
        String refreshToken = TestJwt.accessToken()
                .tokenType("refresh")
                .lifetime(Duration.ofDays(7))
                .signedWith(TestJwt.REFRESH_KEYS)
                .sign();

        callProbe(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRefreshTypedTokenEvenWhenSignedWithAccessKey() throws Exception {
        // Key misconfiguration case: only the token_type check can reject this one.
        callProbe(TestJwt.accessToken().tokenType("refresh").sign())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithoutTokenType() throws Exception {
        callProbe(TestJwt.accessToken().tokenType(null).sign())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongAlgorithm() throws Exception {
        callProbe(TestJwt.accessToken().signWithHmacUsingAccessPublicKey())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsValidAccessTokenAndExposesUserIdFromSubject() throws Exception {
        TestJwt.Token token = TestJwt.accessToken();

        callProbe(token.sign())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(token.subject()))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")));
    }

    @Test
    void mapsAdminRoleToRoleAdminAuthority() throws Exception {
        callProbe(TestJwt.accessToken().role("ADMIN").sign())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_ADMIN")));
    }

    @Test
    void keepsOpenApiDocsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    private ResultActions callProbe(String token) throws Exception {
        return mockMvc.perform(get(PROBE).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}

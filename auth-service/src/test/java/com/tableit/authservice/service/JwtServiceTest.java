package com.tableit.authservice.service;

import com.tableit.authservice.model.User;
import com.tableit.authservice.util.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final KeyPair accessKeys = TestJwtServices.newRsaKeyPair();
    private final KeyPair refreshKeys = TestJwtServices.newRsaKeyPair();
    private final JwtService jwtService = TestJwtServices.jwtService(accessKeys, refreshKeys);

    private final UUID userId = UUID.randomUUID();
    private final User user = new User().setId(userId).setRole(UserRole.USER);

    @Test
    void accessTokenHasAccessTokenType() {
        Claims claims = parse(jwtService.generateAccessToken(user), accessKeys.getPublic());

        assertThat(claims.get("token_type", String.class)).isEqualTo("access");
        assertStandardClaims(claims);
    }

    @Test
    void refreshTokenHasRefreshTokenType() {
        Claims claims = parse(jwtService.generateRefreshToken(user), refreshKeys.getPublic());

        assertThat(claims.get("token_type", String.class)).isEqualTo("refresh");
        assertStandardClaims(claims);
    }

    private void assertStandardClaims(Claims claims) {
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    private static Claims parse(String token, PublicKey publicKey) {
        return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody();
    }
}

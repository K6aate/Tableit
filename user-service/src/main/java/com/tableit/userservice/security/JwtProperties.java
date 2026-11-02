package com.tableit.userservice.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param accessPublicKey auth-service's access token public key, base64-encoded X.509 (SubjectPublicKeyInfo).
 *                        user-service never receives private keys or the refresh token key.
 */
@Validated
@ConfigurationProperties(prefix = "joinara.security.jwt")
public record JwtProperties(@NotBlank String accessPublicKey) {
}

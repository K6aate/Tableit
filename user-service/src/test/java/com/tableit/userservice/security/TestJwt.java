package com.tableit.userservice.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Key pairs generated per test run (nothing is committed) and tokens shaped like auth-service's:
 * RS256, claims sub/role/token_type/iat/exp, no typ header.
 */
public final class TestJwt {

    public static final KeyPair ACCESS_KEYS = newRsaKeyPair();
    public static final KeyPair REFRESH_KEYS = newRsaKeyPair();
    public static final KeyPair UNRELATED_KEYS = newRsaKeyPair();

    private TestJwt() {
    }

    // For full-context tests: user-service only ever receives the access public key.
    public static void registerAccessPublicKey(DynamicPropertyRegistry registry) {
        registry.add("joinara.security.jwt.access-public-key",
                () -> Base64.getEncoder().encodeToString(ACCESS_KEYS.getPublic().getEncoded()));
    }

    public static Token accessToken() {
        return new Token();
    }

    public static final class Token {
        private String subject = UUID.randomUUID().toString();
        private String role = "USER";
        private String tokenType = "access";
        private Instant issuedAt = Instant.now();
        private Duration lifetime = Duration.ofMinutes(15);
        private KeyPair signingKeys = ACCESS_KEYS;

        public Token subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Token role(String role) {
            this.role = role;
            return this;
        }

        public Token tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Token issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Token lifetime(Duration lifetime) {
            this.lifetime = lifetime;
            return this;
        }

        public Token signedWith(KeyPair signingKeys) {
            this.signingKeys = signingKeys;
            return this;
        }

        public String subject() {
            return subject;
        }

        public String sign() {
            return sign(JWSAlgorithm.RS256, new RSASSASigner(signingKeys.getPrivate()));
        }

        // HS256 keyed with the public key bytes: the classic algorithm-confusion attack.
        public String signWithHmacUsingAccessPublicKey() {
            try {
                return sign(JWSAlgorithm.HS256, new MACSigner(ACCESS_KEYS.getPublic().getEncoded()));
            } catch (JOSEException e) {
                throw new IllegalStateException(e);
            }
        }

        private String sign(JWSAlgorithm algorithm, JWSSigner signer) {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .claim("role", role)
                    .claim("token_type", tokenType)
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(issuedAt.plus(lifetime)))
                    .build();
            try {
                SignedJWT jwt = new SignedJWT(new JWSHeader(algorithm), claims);
                jwt.sign(signer);
                return jwt.serialize();
            } catch (JOSEException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static KeyPair newRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.tableit.authservice.service;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// Builds a real JwtService with key pairs generated for the test run; no keys are stored in the repository.
public final class TestJwtServices {

    private TestJwtServices() {
    }

    public static KeyPair newRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static JwtService jwtService(KeyPair accessKeys, KeyPair refreshKeys) {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtAccessPrivateKey", encode(accessKeys.getPrivate().getEncoded()));
        ReflectionTestUtils.setField(jwtService, "jwtAccessPublicKey", encode(accessKeys.getPublic().getEncoded()));
        ReflectionTestUtils.setField(jwtService, "jwtRefreshPrivateKey", encode(refreshKeys.getPrivate().getEncoded()));
        ReflectionTestUtils.setField(jwtService, "jwtRefreshPublicKey", encode(refreshKeys.getPublic().getEncoded()));
        ReflectionTestUtils.setField(jwtService, "jwtAccessExpiration", 900_000L);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpiration", 604_800_000L);
        jwtService.init();
        return jwtService;
    }

    // For full-context tests: supplies the four jwt.* key properties that production reads from the environment.
    public static void registerJwtKeyProperties(DynamicPropertyRegistry registry) {
        KeyPair accessKeys = newRsaKeyPair();
        KeyPair refreshKeys = newRsaKeyPair();
        registry.add("jwt.private-key.access", () -> encode(accessKeys.getPrivate().getEncoded()));
        registry.add("jwt.public-key.access", () -> encode(accessKeys.getPublic().getEncoded()));
        registry.add("jwt.private-key.refresh", () -> encode(refreshKeys.getPrivate().getEncoded()));
        registry.add("jwt.public-key.refresh", () -> encode(refreshKeys.getPublic().getEncoded()));
    }

    private static String encode(byte[] key) {
        return Base64.getEncoder().encodeToString(key);
    }
}

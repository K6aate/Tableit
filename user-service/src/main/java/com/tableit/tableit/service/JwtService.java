package com.tableit.tableit.service;

import com.tableit.tableit.exception.UnauthorizedException;
import com.tableit.tableit.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.private-key.access}")
    private String jwtAccessPrivateKey;

    @Value("${jwt.public-key.access}")
    private String jwtAccessPublicKey;

    @Value("${jwt.private-key.refresh}")
    private String jwtRefreshPrivateKey;

    @Value("${jwt.public-key.refresh}")
    private String jwtRefreshPublicKey;

    @Value("${jwt.expiration.access-ms}")
    private long jwtAccessExpiration;

    @Value("${jwt.expiration.refresh-ms}")
    private long jwtRefreshExpiration;

    private PrivateKey accessPrivateKeyObj;
    private PrivateKey refreshPrivateKeyObj;
    private PublicKey accessPublicKeyObj;
    private PublicKey refreshPublicKeyObj;

    @PostConstruct
    public void init() {
        try {
            accessPrivateKeyObj = getPrivateKey(jwtAccessPrivateKey);
            refreshPrivateKeyObj = getPrivateKey(jwtRefreshPrivateKey);
            accessPublicKeyObj = getPublicKey(jwtAccessPublicKey);
            refreshPublicKeyObj = getPublicKey(jwtRefreshPublicKey);
            log.info("JWT private keys loaded successfully");
        } catch (Exception e) {
            log.error("Failed to initialize JWT private keys", e);
            throw new IllegalStateException("Invalid JWT private keys", e);
        }
    }
    public String generateAccessToken(User userDetails) {
        return generateToken(new HashMap<>(), userDetails, jwtAccessExpiration, accessPrivateKeyObj);
    }

    public String generateRefreshToken(User userDetails) {
        return generateToken(new HashMap<>(), userDetails, jwtRefreshExpiration, refreshPrivateKeyObj);
    }

    private String generateToken(Map<String, Object> extraClaims, User userDetails, long expiration, PrivateKey privateKey) {
        User user = userDetails;

        extraClaims.put("role", user.getRole().getName());

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    private PrivateKey getPrivateKey(String privateKeyString) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            log.error("Error while parsing private key (base64 length={}): {}",
                    privateKeyString == null ? 0 : privateKeyString.length(), e.getMessage(), e);
            throw new RuntimeException("Failed to load private key", e);
        }
    }

    private PublicKey getPublicKey(String publicKeyString) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(publicKeyString);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            log.error("Error while parsing public key (base64 length={}): {}",
                    publicKeyString == null ? 0 : publicKeyString.length(), e.getMessage(), e);
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(accessPublicKeyObj)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(accessPublicKeyObj)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }

    public String extractUsernameFromRefreshToken(String token) {
        final Claims claims = extractAllClaims(token, refreshPublicKeyObj);
        return claims.getSubject();
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            return !isTokenExpired(token, refreshPublicKeyObj);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    private boolean isTokenExpired(String token, PublicKey publicKey) {
        return extractExpiration(token, publicKey).before(new Date());
    }

    private Date extractExpiration(String token, PublicKey publicKey) {
        return extractAllClaims(token, publicKey).getExpiration();
    }

    private Claims extractAllClaims(String token, PublicKey publicKey) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

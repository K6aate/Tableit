package com.tableit.userservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

// Test-only endpoint under /api/users/** that exposes what a real controller would receive.
@RestController
class SecurityProbeController {

    @GetMapping("/api/users/test-probe")
    Map<String, Object> probe(@CurrentUserId UUID userId, Authentication authentication) {
        return Map.of(
                "userId", userId,
                "authorities", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }
}

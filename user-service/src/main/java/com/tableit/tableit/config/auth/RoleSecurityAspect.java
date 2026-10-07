package com.tableit.tableit.config.auth;

import com.tableit.tableit.exception.ForbiddenException;
import com.tableit.tableit.exception.UnauthorizedException;
import com.tableit.tableit.util.enums.UserRole;
import com.tableit.tableit.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RoleSecurityAspect {
    private final AuthService authService;

    @Before("@annotation(roleSecured)")
    public void checkRoleAccess(JoinPoint joinPoint, RoleSecured roleSecured) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Attempt to access @RoleSecured endpoint without JWT token");
            throw new UnauthorizedException("JWT token is required. Use 'Authorization: Bearer <token>' header.");
        }

        UserRole userRole = authService.getUserRole();
        UserRole requiredRole = roleSecured.value();

        if (userRole.getLevel() < requiredRole.getLevel()) {
            log.warn("Access denied for user with role {} to endpoint requiring {}. Required level: {}, User level: {}",
                    userRole.getName(), requiredRole.getName(), requiredRole.getLevel(), userRole.getLevel());
            throw new ForbiddenException(
                    String.format("Access denied. This endpoint requires role: %s or higher. Your role: %s",
                            requiredRole.getName(), userRole.getName())
            );
        }

        log.debug("Access granted for user with role {} to endpoint requiring {}", userRole.getName(), requiredRole.getName());
    }
}

package com.vulntrack.utils;

import com.vulntrack.enums.Role;
import com.vulntrack.model.SecurityUser;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static SecurityUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser)) {
            throw new IllegalStateException("User not authenticated");
        }
        return (SecurityUser) auth.getPrincipal();
    }

    public static boolean hasRole(Role requiredRole) {
        SecurityUser user = getCurrentUser();
        // hierarchy: ADMIN > SECURITY_ADMIN > DEVELOPER
        if (requiredRole == Role.ROLE_DEVELOPER) {
            return user.getAuthorities().stream()
                .anyMatch(
                    a -> a.getAuthority().equals(Role.ROLE_DEVELOPER.name())
                        || a.getAuthority().equals(Role.ROLE_SECURITY_ADMIN.name())
                        || a.getAuthority().equals(Role.ROLE_ADMIN.name())
                );
        }
        if (requiredRole == Role.ROLE_SECURITY_ADMIN) {
            return user.getAuthorities().stream()
                .anyMatch(
                    a -> a.getAuthority().equals(Role.ROLE_SECURITY_ADMIN.name())
                        || a.getAuthority().equals(Role.ROLE_ADMIN.name())
                );
        }
        if (requiredRole == Role.ROLE_ADMIN) {
            return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
        }
        return false;
    }

    public static boolean isAdmin() { return hasRole(Role.ROLE_ADMIN); }

    public static boolean isSecurityAdmin() { return hasRole(Role.ROLE_SECURITY_ADMIN); }
}
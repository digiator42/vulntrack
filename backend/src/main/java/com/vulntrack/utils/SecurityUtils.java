package com.vulntrack.utils;

import com.vulntrack.model.SecurityUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {} // Prevent instantiation

    public static SecurityUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser)) {
            throw new IllegalStateException("User not authenticated");
        }
        return (SecurityUser) auth.getPrincipal();
    }
}
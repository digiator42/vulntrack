package com.vulntrack.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.vulntrack.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component
@RequestScope
public class UserContext {

    private final HttpServletRequest request;
    private String role;

    public UserContext(HttpServletRequest request) {
        this.request = request;
    }

    public Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() != null) {
            Object principal = auth.getPrincipal();

            if (principal instanceof Long) {
                System.out.println("====>>> {{" + principal + "}}");
                return (Long) principal;
            }

            if (principal instanceof String) {
                try {
                    System.out.println("====>>> String {{" + principal + "}}");
                    return Long.parseLong((String) principal);
                } catch (NumberFormatException e) {
                    // Pass
                }
            }
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("USER_ID") != null) {
            return (Long) session.getAttribute("USER_ID");
        }

        throw new UnauthorizedException("User is not Authenticated");
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}

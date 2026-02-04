package com.megastar.vulntrack.filter;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.megastar.vulntrack.config.UserContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserContextFilter extends OncePerRequestFilter {

    private final UserContext userContext;

    public UserContextFilter(UserContext userContext) {
        this.userContext = userContext;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("X-USER-ID");

        if (header == null || header.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing X-USER-ID");
            return;
            // throw new UnauthorizedException("Missing X-USER-ID");
        }

        try {
            userContext.setUserID(Long.valueOf(header));
        } catch (NumberFormatException e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing X-USER-ID");
            return;
            // throw new UnauthorizedException("Invalid X-USER-ID");
        }

        filterChain.doFilter(request, response);
    }

}

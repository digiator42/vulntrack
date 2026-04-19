package com.vulntrack.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        System.out.println("Session: ==> " + (session != null));

        if (session != null) {
            System.out.println("User ID in Session: ==> " + session.getAttribute("USER_ID"));
        }

        // If there's a session and we haven't authenticated (JWT) this request yet
        if (session != null && session.getAttribute("USER_ID") != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            Long userId = (Long) session.getAttribute("USER_ID");

            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
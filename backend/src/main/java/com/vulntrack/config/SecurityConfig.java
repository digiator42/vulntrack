package com.vulntrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.vulntrack.filter.JwtAuthFilter;
import com.vulntrack.filter.SessionAuthFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;
    private final SessionAuthFilter sessionFilter;

    public SecurityConfig(JwtAuthFilter jwtFilter, SessionAuthFilter sessionFilter) {
        this.jwtFilter = jwtFilter;
        this.sessionFilter = sessionFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/web/login",
                                "/auth/**",
                                "/error")
                        .permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/web/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/issues/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(sessionFilter, JwtAuthFilter.class);

        return http.build();
    }
}
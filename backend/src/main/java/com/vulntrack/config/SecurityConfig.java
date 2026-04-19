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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.vulntrack.filter.JwtAuthFilter;
import com.vulntrack.filter.SessionAuthFilter;

import java.util.Arrays;

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
            // Enable CORS with custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .csrf(AbstractHttpConfigurer::disable)

            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            .authorizeHttpRequests(
                auth -> auth
                    // Allow public resources
                    .requestMatchers(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/web/login",
                        "/auth/**",
                        "/error",
                        // Swagger UI paths
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                    )
                    .permitAll()

                    // API endpoints require authentication
                    .requestMatchers("/api/**").authenticated()

                    // DELETE requires ADMIN role
                    .requestMatchers(HttpMethod.DELETE, "/api/issues/**").hasRole("ADMIN")

                    .anyRequest().authenticated()
            )

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(sessionFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow credentials (cookies, authorization headers, etc.)
        configuration.setAllowCredentials(true);

        // Allow requests from Next.js frontend
        configuration.setAllowedOrigins(
            Arrays.asList(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
            )
        );

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow all HTTP methods
        configuration.setAllowedMethods(
            Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
            )
        );

        // Expose headers to the frontend
        configuration.setExposedHeaders(
            Arrays.asList(
                "Authorization",
                "Content-Type"
            )
        );

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply to all endpoints
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
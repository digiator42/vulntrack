package com.vulntrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.vulntrack.filter.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
        throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS with custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .csrf(AbstractHttpConfigurer::disable)

            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(
                auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Allow the handshake
                    .requestMatchers("/ws/**").permitAll()
                    // Allow public resources
                    .requestMatchers(
                        "/public/**", "/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**"
                    ).permitAll()
                    // API endpoints require authentication
                    .requestMatchers("/api/**").authenticated()
                    // DELETE requires ADMIN role
                    .requestMatchers(HttpMethod.DELETE, "/api/issues/**").hasAuthority("ROLE_ADMIN")

                    .anyRequest().authenticated()
            )

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

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
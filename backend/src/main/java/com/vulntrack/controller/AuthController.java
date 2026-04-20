package com.vulntrack.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vulntrack.dto.LoginRequest;
import com.vulntrack.dto.RegisterRequest;
import com.vulntrack.model.Role;
import com.vulntrack.model.User;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.service.JwtService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {
        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        System.out.println("=========>>>>> " + auth.getPrincipal());

        User user = userRepository.findByUsername(req.username())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String token = jwtService.generateToken(user.getUsername(), user.getId(), user.getRole().name());

        return Map.of("token", token);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        // Check if user already exists
        if (userRepository.existsByUsername(req.username())) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        // Create and hash password
        User newUser = new User();
        newUser.setUsername(req.username());
        newUser.setPassword(passwordEncoder.encode(req.password()));
        newUser.setRole(Role.ROLE_USER); // Default role

        userRepository.save(newUser);

        return ResponseEntity.ok("User registered successfully!");
    }
}
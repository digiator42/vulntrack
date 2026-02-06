package com.megastar.vulntrack.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.megastar.vulntrack.config.UserContext;
import com.megastar.vulntrack.dto.LoginRequest;
import com.megastar.vulntrack.exception.UnauthorizedException;
import com.megastar.vulntrack.security.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtService jwtService;
    
    public AuthController(JwtService jwtService, UserContext userContext) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public Map<String, String> getToken(@RequestBody LoginRequest req) {
        if (!"user".equals(req.username())) {
            throw new UnauthorizedException("Invalid Credentials");
        }

        String token = jwtService.generateToken(1L, "USER");
        java.lang.System.out.println("----->> " + token);
        return Map.of("token", token);
    }
}

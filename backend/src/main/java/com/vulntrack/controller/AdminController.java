package com.vulntrack.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vulntrack.enums.Role;
import com.vulntrack.model.User;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.service.JwtService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')") // Only admins can access this controller
@RequiredArgsConstructor
public class AdminController {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PatchMapping("/{userId}/role")
    public void updateUserRole(@PathVariable Long userId, @RequestBody Role newRole) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setRole(newRole);
        userRepository.save(user);
    }

}

package com.vulntrack.dto;

import com.vulntrack.enums.Role;
import com.vulntrack.model.User;

public record UserResponse(
    Long id,
    String username,
    String email,
    Role role
) {

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole()
        );
    }
}

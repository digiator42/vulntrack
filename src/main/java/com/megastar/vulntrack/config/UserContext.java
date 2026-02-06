package com.megastar.vulntrack.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class UserContext {
    private Long userId;
    private String role;

    public Long getUserId() {
        return this.userId;
    }
    
    public void setUserID(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    
}

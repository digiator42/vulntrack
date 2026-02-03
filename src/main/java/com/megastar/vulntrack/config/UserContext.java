package com.megastar.vulntrack.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class UserContext {
    private Long userId;

    public Long getUserId() {
        return this.userId;
    }

    public void setUserID(Long userId) {
        this.userId = userId;
    }
    
}

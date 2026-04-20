package com.vulntrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Username is required") 
    @Size(min = 5, max = 20) 
    String username,
    
    @NotBlank(message = "Password is required") 
    @Size(min = 5, max = 20)
    String password,
    
    @NotBlank(message = "Email is required") 
    String email
) {
}
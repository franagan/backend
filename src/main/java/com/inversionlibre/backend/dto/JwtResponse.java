package com.inversionlibre.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de login JWT
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {
    
    private String token;
    
    @Builder.Default
    private String type = "Bearer";
    
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private LocalDateTime expiresAt;
    private LocalDateTime issuedAt;
    
    public JwtResponse(String token, String email, String firstName, String lastName, String role) {
        this.token = token;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.type = "Bearer";
        this.issuedAt = LocalDateTime.now();
    }
}





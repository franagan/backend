package com.inversionlibre.backend.dto;

import com.inversionlibre.backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para respuestas de información de usuario
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private String fullName;
    private Boolean enabled;
    private List<String> portfolioIds;
    private User.UserPreferences preferences;
    private User.RiskProfile riskProfile;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    
    /**
     * Convierte un User a UserResponse (sin incluir la contraseña)
     */
    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .role(user.getRole() != null ? user.getRole().name() : null)
            .fullName(user.getFullName())
            .enabled(user.getEnabled())
            .portfolioIds(user.getPortfolioIds())
            .preferences(user.getPreferences())
            .riskProfile(user.getRiskProfile())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
}



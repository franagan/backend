package com.inversionlibre.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Entidad User - Representa un usuario de la plataforma Inversión Libre
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User implements UserDetails {

    @Id
    private String id;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Indexed(unique = true)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String firstName;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    private String lastName;

    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "El teléfono debe tener un formato válido")
    private String phone;

    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Boolean accountNonExpired = true;

    @Builder.Default
    private Boolean accountNonLocked = true;

    @Builder.Default
    private Boolean credentialsNonExpired = true;

    // Lista de IDs de carteras del usuario
    @Builder.Default
    private List<String> portfolioIds = new ArrayList<>();

    // Preferencias del usuario
    private UserPreferences preferences;

    // Perfil de riesgo del inversor
    private RiskProfile riskProfile;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Metadatos adicionales
    private String lastLoginIp;
    private LocalDateTime lastLoginAt;
    private Integer loginAttempts;

    /**
     * Enum para los roles de usuario
     */
    public enum Role {
        USER,       // Usuario estándar
        PREMIUM,    // Usuario premium
        ADMIN       // Administrador
    }

    /**
     * Enum para el perfil de riesgo
     */
    public enum RiskProfile {
        CONSERVATIVE,   // Conservador
        MODERATE,       // Moderado
        AGGRESSIVE      // Agresivo
    }

    /**
     * Clase interna para preferencias del usuario
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserPreferences {
        @Builder.Default
        private String currency = "EUR";
        
        @Builder.Default
        private String language = "es";
        
        @Builder.Default
        private Boolean emailNotifications = true;
        
        @Builder.Default
        private Boolean pushNotifications = false;
        
        @Builder.Default
        private Boolean marketingEmails = false;
    }

    /**
     * Obtiene el nombre completo del usuario
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Verifica si el usuario tiene rol de administrador
     */
    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }

    /**
     * Verifica si el usuario tiene rol premium
     */
    public boolean isPremium() {
        return Role.PREMIUM.equals(this.role);
    }

    /**
     * Añade un portfolio ID a la lista del usuario
     */
    public void addPortfolioId(String portfolioId) {
        if (this.portfolioIds == null) {
            this.portfolioIds = new ArrayList<>();
        }
        if (!this.portfolioIds.contains(portfolioId)) {
            this.portfolioIds.add(portfolioId);
        }
    }

    /**
     * Elimina un portfolio ID de la lista del usuario
     */
    public void removePortfolioId(String portfolioId) {
        if (this.portfolioIds != null) {
            this.portfolioIds.remove(portfolioId);
        }
    }

    // ========== MÉTODOS REQUERIDOS POR USERDETAILS ==========

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
        return authorities;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}
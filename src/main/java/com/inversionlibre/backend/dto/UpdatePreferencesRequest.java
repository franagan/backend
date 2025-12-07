package com.inversionlibre.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar preferencias del usuario
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreferencesRequest {
    
    private String currency;
    private String language;
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean marketingEmails;
}



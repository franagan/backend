package com.inversionlibre.backend.dto.portfolio;

import com.inversionlibre.backend.model.Portfolio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar un portfolio existente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePortfolioRequest {
    
    @NotBlank(message = "El nombre del portfolio es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String name;
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;
    
    private Portfolio.PortfolioType type;
    
    private Portfolio.RiskLevel riskLevel;
    
    private String currency;
}

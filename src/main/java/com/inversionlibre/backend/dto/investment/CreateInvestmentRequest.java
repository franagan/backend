package com.inversionlibre.backend.dto.investment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para crear una nueva inversión
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvestmentRequest {
    
    @NotBlank(message = "El ID del portfolio es obligatorio")
    private String portfolioId;
    
    @NotBlank(message = "El ID del stock es obligatorio")
    private String stockId;
    
    @NotBlank(message = "El símbolo del stock es obligatorio")
    private String stockSymbol;
    
    @NotBlank(message = "El nombre del stock es obligatorio")
    private String stockName;
    
    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad no puede ser negativa")
    @Digits(integer = 12, fraction = 8)
    private BigDecimal quantity;
    
    @NotNull(message = "El precio promedio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Digits(integer = 12, fraction = 4)
    private BigDecimal averagePrice;
    
    @DecimalMin(value = "0.0", message = "El precio actual no puede ser negativo")
    @Digits(integer = 12, fraction = 4)
    private BigDecimal currentPrice;
}

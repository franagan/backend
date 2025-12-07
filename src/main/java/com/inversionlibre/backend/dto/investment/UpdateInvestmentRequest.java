package com.inversionlibre.backend.dto.investment;

import com.inversionlibre.backend.model.Investment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para actualizar una inversión existente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvestmentRequest {
    
    @DecimalMin(value = "0.0", message = "La cantidad no puede ser negativa")
    @Digits(integer = 12, fraction = 8)
    private BigDecimal quantity;
    
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Digits(integer = 12, fraction = 4)
    private BigDecimal averagePrice;
    
    @DecimalMin(value = "0.0", message = "El precio actual no puede ser negativo")
    @Digits(integer = 12, fraction = 4)
    private BigDecimal currentPrice;
    
    private Investment.InvestmentStrategy strategy;
    
    private Investment.InvestmentStatus status;
}

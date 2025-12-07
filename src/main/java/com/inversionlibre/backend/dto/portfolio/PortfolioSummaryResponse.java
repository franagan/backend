package com.inversionlibre.backend.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta resumida para portfolio (solo métricas clave)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryResponse {
    
    private String id;
    private String name;
    private BigDecimal totalValue;
    private BigDecimal totalReturn;
    private Integer positions;
}

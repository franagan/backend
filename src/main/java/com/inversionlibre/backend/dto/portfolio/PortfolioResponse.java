package com.inversionlibre.backend.dto.portfolio;

import com.inversionlibre.backend.model.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para portfolio (información básica)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    
    private String id;
    private String name;
    private String description;
    private Portfolio.PortfolioType type;
    private Portfolio.RiskLevel riskLevel;
    private String currency;
    
    private BigDecimal totalValue;
    private BigDecimal totalInvested;
    private BigDecimal totalGainLoss;
    private BigDecimal totalGainLossPercentage;
    
    private Integer totalInvestments;
    private Boolean isActive;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

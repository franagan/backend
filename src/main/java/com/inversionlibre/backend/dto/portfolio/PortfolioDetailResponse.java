package com.inversionlibre.backend.dto.portfolio;

import com.inversionlibre.backend.model.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta detallada para portfolio (incluye analytics y goals)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDetailResponse {
    
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
    private Boolean isPublic;
    
    private Portfolio.FinancialGoal goal;
    private Portfolio.PortfolioAnalytics analytics;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRebalanceAt;
}

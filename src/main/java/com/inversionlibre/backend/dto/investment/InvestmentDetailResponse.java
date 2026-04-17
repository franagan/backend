package com.inversionlibre.backend.dto.investment;

import com.inversionlibre.backend.model.Investment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta detallada para inversión (incluye alerts, goals, performance)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentDetailResponse {
    
    private String id;
    private String portfolioId;
    private String stockId;
    private String stockSymbol;
    private String stockName;
    
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;
    
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal gainLoss;
    private BigDecimal gainLossPercentage;
    
    private Investment.InvestmentStrategy strategy;
    private Investment.InvestmentStatus status;
    
    private Investment.InvestmentAlerts alerts;
    private Investment.InvestmentGoals goals;
    private Investment.PerformanceMetrics performance;
    
    private Integer totalTransactions;
    private java.util.List<com.inversionlibre.backend.model.Transaction> transactions;
    private LocalDateTime firstPurchaseDate;
    private LocalDateTime lastTransactionDate;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Entidad Portfolio - Representa una cartera de inversión de un usuario
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "portfolios")
public class Portfolio {

    @Id
    private String id;

    @NotBlank(message = "El nombre de la cartera es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String name;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;

    @NotNull(message = "El ID del usuario propietario es obligatorio")
    @Indexed
    private String userId;

    // Lista de inversiones en esta cartera
    @Builder.Default
    private List<String> investmentIds = new ArrayList<>();

    // Valores monetarios de la cartera
    @Builder.Default
    @DecimalMin(value = "0.0", message = "El valor total no puede ser negativo")
    @Digits(integer = 12, fraction = 2, message = "El valor total debe tener máximo 12 dígitos enteros y 2 decimales")
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "El valor invertido no puede ser negativo")
    @Digits(integer = 12, fraction = 2, message = "El valor invertido debe tener máximo 12 dígitos enteros y 2 decimales")
    private BigDecimal totalInvested = BigDecimal.ZERO;

    @Builder.Default
    @Digits(integer = 12, fraction = 2, message = "La ganancia/pérdida debe tener máximo 12 dígitos enteros y 2 decimales")
    private BigDecimal totalGainLoss = BigDecimal.ZERO;

    @Builder.Default
    @Digits(integer = 5, fraction = 4, message = "El porcentaje de ganancia debe tener máximo 5 dígitos enteros y 4 decimales")
    private BigDecimal totalGainLossPercentage = BigDecimal.ZERO;

    // Configuración de la cartera
    @Builder.Default
    private PortfolioType type = PortfolioType.PERSONAL;

    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.MODERATE;

    @Builder.Default
    private String currency = "EUR";

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isPublic = false;

    // Objetivos financieros
    private FinancialGoal goal;

    // Metadatos de análisis
    private PortfolioAnalytics analytics;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime lastRebalanceAt;
    private LocalDateTime lastUpdateAt;

    /**
     * Enum para el tipo de cartera
     */
    public enum PortfolioType {
        PERSONAL,       // Cartera personal
        RETIREMENT,     // Cartera para jubilación
        EDUCATION,      // Cartera para educación
        EMERGENCY,      // Fondo de emergencia
        SAVINGS,        // Ahorros
        SPECULATIVE     // Especulativa/Trading
    }

    /**
     * Enum para el nivel de riesgo
     */
    public enum RiskLevel {
        VERY_LOW,       // Muy bajo riesgo
        LOW,            // Bajo riesgo
        MODERATE,       // Riesgo moderado
        HIGH,           // Alto riesgo
        VERY_HIGH       // Muy alto riesgo
    }

    /**
     * Clase interna para objetivos financieros
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinancialGoal {
        private String description;
        private BigDecimal targetAmount;
        private LocalDateTime targetDate;
        private BigDecimal monthlyContribution;
        private GoalStatus status;

        public enum GoalStatus {
            ACTIVE,
            ACHIEVED,
            PAUSED,
            CANCELLED
        }
    }

    /**
     * Clase interna para análisis de la cartera
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PortfolioAnalytics {
        private BigDecimal volatility;
        private BigDecimal sharpeRatio;
        private BigDecimal beta;
        private BigDecimal alpha;
        private BigDecimal maxDrawdown;
        private BigDecimal averageReturn;
        private Integer totalTransactions;
        private LocalDateTime lastAnalysisAt;
    }

    /**
     * Añade una inversión a la cartera
     */
    public void addInvestmentId(String investmentId) {
        if (this.investmentIds == null) {
            this.investmentIds = new ArrayList<>();
        }
        if (!this.investmentIds.contains(investmentId)) {
            this.investmentIds.add(investmentId);
        }
    }

    /**
     * Elimina una inversión de la cartera
     */
    public void removeInvestmentId(String investmentId) {
        if (this.investmentIds != null) {
            this.investmentIds.remove(investmentId);
        }
    }

    /**
     * Calcula el porcentaje de ganancia/pérdida
     */
    public void calculateGainLossPercentage() {
        if (totalInvested != null && totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            this.totalGainLossPercentage = totalGainLoss
                .divide(totalInvested, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        } else {
            this.totalGainLossPercentage = BigDecimal.ZERO;
        }
    }

    /**
     * Actualiza los valores calculados de la cartera
     */
    public void updateCalculatedValues() {
        if (totalValue != null && totalInvested != null) {
            this.totalGainLoss = totalValue.subtract(totalInvested);
            calculateGainLossPercentage();
        }
        this.lastUpdateAt = LocalDateTime.now();
    }

    /**
     * Verifica si la cartera necesita rebalanceado
     */
    public boolean needsRebalancing() {
        if (lastRebalanceAt == null) {
            return true;
        }
        return lastRebalanceAt.isBefore(LocalDateTime.now().minusMonths(3));
    }

    /**
     * Obtiene el número total de inversiones
     */
    public int getTotalInvestments() {
        return investmentIds != null ? investmentIds.size() : 0;
    }

    /**
     * Verifica si la cartera está diversificada (más de 5 inversiones)
     */
    public boolean isDiversified() {
        return getTotalInvestments() >= 5;
    }
}
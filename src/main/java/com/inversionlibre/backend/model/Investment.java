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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Entidad Investment - Representa una inversión específica en una cartera
 * (la relación entre un Portfolio y un Stock con cantidad y precio)
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "investments")
public class Investment {

    @Id
    private String id;

    @NotBlank(message = "El ID del portfolio es obligatorio")
    @Indexed
    private String portfolioId;

    @NotBlank(message = "El ID del stock es obligatorio")
    @Indexed
    private String stockId;

    @NotBlank(message = "El símbolo del stock es obligatorio")
    private String stockSymbol; // Desnormalizado para consultas rápidas

    @NotBlank(message = "El nombre del stock es obligatorio")
    private String stockName; // Desnormalizado para consultas rápidas

    // Cantidad de acciones/unidades
    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad no puede ser negativa")
    @Digits(integer = 12, fraction = 8, message = "La cantidad debe tener máximo 12 dígitos enteros y 8 decimales")
    private BigDecimal quantity;

    // Precio promedio de compra (weighted average)
    @NotNull(message = "El precio promedio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio promedio no puede ser negativo")
    @Digits(integer = 12, fraction = 4, message = "El precio promedio debe tener máximo 12 dígitos enteros y 4 decimales")
    private BigDecimal averagePrice;

    // Precio actual del activo (cache del Stock)
    @DecimalMin(value = "0.0", message = "El precio actual no puede ser negativo")
    @Digits(integer = 12, fraction = 4)
    private BigDecimal currentPrice;

    // Valores calculados
    @Builder.Default
    @DecimalMin(value = "0.0", message = "El valor total invertido no puede ser negativo")
    @Digits(integer = 15, fraction = 2)
    private BigDecimal totalInvested = BigDecimal.ZERO;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "El valor actual no puede ser negativo")
    @Digits(integer = 15, fraction = 2)
    private BigDecimal currentValue = BigDecimal.ZERO;

    @Builder.Default
    @Digits(integer = 15, fraction = 2)
    private BigDecimal gainLoss = BigDecimal.ZERO;

    @Builder.Default
    @Digits(integer = 5, fraction = 4)
    private BigDecimal gainLossPercentage = BigDecimal.ZERO;

    // Configuración de la inversión
    @Builder.Default
    private InvestmentStrategy strategy = InvestmentStrategy.BUY_AND_HOLD;

    @Builder.Default
    private InvestmentStatus status = InvestmentStatus.ACTIVE;

    // Alertas y límites
    private InvestmentAlerts alerts;

    // Objetivos específicos de esta inversión
    private InvestmentGoals goals;

    // Metadatos
    @Builder.Default
    private List<String> transactionIds = new ArrayList<>();

    @Builder.Default
    private Integer totalTransactions = 0;

    private LocalDateTime firstPurchaseDate;
    private LocalDateTime lastTransactionDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime lastPriceUpdate;

    // Análisis de rendimiento
    private PerformanceMetrics performance;

    /**
     * Enum para estrategias de inversión
     */
    public enum InvestmentStrategy {
        BUY_AND_HOLD,       // Comprar y mantener
        DOLLAR_COST_AVERAGE, // Promedio de costo en dólares
        VALUE_INVESTING,     // Inversión en valor
        GROWTH_INVESTING,    // Inversión en crecimiento
        MOMENTUM,           // Momentum
        SWING_TRADING,      // Swing trading
        DAY_TRADING,        // Day trading
        DIVIDEND_INCOME     // Ingresos por dividendos
    }

    /**
     * Enum para el estado de la inversión
     */
    public enum InvestmentStatus {
        ACTIVE,             // Activa
        PARTIAL_EXIT,       // Salida parcial
        CLOSED,             // Cerrada
        PAUSED,             // Pausada
        UNDER_REVIEW        // En revisión
    }

    /**
     * Clase interna para alertas de inversión
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvestmentAlerts {
        // Alertas de precio
        private BigDecimal stopLoss;            // Stop loss
        private BigDecimal takeProfit;          // Take profit
        private BigDecimal priceLowerAlert;     // Alerta precio bajo
        private BigDecimal priceUpperAlert;     // Alerta precio alto
        
        // Alertas de porcentaje
        private BigDecimal lossPercentageAlert;  // Alerta pérdida %
        private BigDecimal gainPercentageAlert;  // Alerta ganancia %
        
        // Configuración de alertas
        @Builder.Default
        private Boolean alertsEnabled = false;
        
        @Builder.Default
        private Boolean emailAlerts = false;
        
        @Builder.Default
        private Boolean pushAlerts = false;
    }

    /**
     * Clase interna para objetivos de inversión
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvestmentGoals {
        private BigDecimal targetValue;          // Valor objetivo
        private BigDecimal targetGainPercentage; // Ganancia objetivo %
        private LocalDateTime targetDate;        // Fecha objetivo
        private BigDecimal monthlyContribution;  // Contribución mensual
        private String description;              // Descripción del objetivo
        
        @Builder.Default
        private GoalStatus status = GoalStatus.ACTIVE;
        
        public enum GoalStatus {
            ACTIVE,
            ACHIEVED,
            MODIFIED,
            CANCELLED
        }
    }

    /**
     * Clase interna para métricas de rendimiento
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PerformanceMetrics {
        private BigDecimal annualizedReturn;     // Rentabilidad anualizada
        private BigDecimal volatility;           // Volatilidad
        private BigDecimal sharpeRatio;          // Ratio de Sharpe
        private BigDecimal maxDrawdown;          // Máxima caída
        private BigDecimal totalReturn;          // Rentabilidad total
        private Integer holdingPeriodDays;       // Días de tenencia
        private BigDecimal dividendsReceived;    // Dividendos recibidos
        private LocalDateTime lastCalculated;    // Última calculación
    }

    /**
     * Actualiza los valores calculados de la inversión
     */
    public void updateCalculatedValues() {
        if (quantity != null && averagePrice != null) {
            this.totalInvested = quantity.multiply(averagePrice);
        }

        if (quantity != null && currentPrice != null) {
            this.currentValue = quantity.multiply(currentPrice);
        }

        if (currentValue != null && totalInvested != null) {
            this.gainLoss = currentValue.subtract(totalInvested);
            
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                this.gainLossPercentage = gainLoss
                    .divide(totalInvested, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }
        }

        this.lastPriceUpdate = LocalDateTime.now();
    }

    /**
     * Actualiza el precio actual y recalcula valores
     */
    public void updateCurrentPrice(BigDecimal newPrice) {
        this.currentPrice = newPrice;
        updateCalculatedValues();
    }

    /**
     * Añade una transacción a la lista
     */
    public void addTransactionId(String transactionId) {
        if (this.transactionIds == null) {
            this.transactionIds = new ArrayList<>();
        }
        if (!this.transactionIds.contains(transactionId)) {
            this.transactionIds.add(transactionId);
            this.totalTransactions = this.transactionIds.size();
            this.lastTransactionDate = LocalDateTime.now();
        }
    }

    /**
     * Verifica si la inversión está en ganancia
     */
    public boolean isInProfit() {
        return gainLoss != null && gainLoss.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Verifica si la inversión está en pérdida
     */
    public boolean isInLoss() {
        return gainLoss != null && gainLoss.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Obtiene el porcentaje de la cartera que representa esta inversión
     */
    public BigDecimal getPortfolioPercentage(BigDecimal totalPortfolioValue) {
        if (currentValue == null || totalPortfolioValue == null || 
            totalPortfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return currentValue
            .divide(totalPortfolioValue, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Verifica si se han activado alertas de stop loss
     */
    public boolean shouldTriggerStopLoss() {
        if (alerts == null || alerts.getStopLoss() == null || currentPrice == null) {
            return false;
        }
        return currentPrice.compareTo(alerts.getStopLoss()) <= 0;
    }

    /**
     * Verifica si se han activado alertas de take profit
     */
    public boolean shouldTriggerTakeProfit() {
        if (alerts == null || alerts.getTakeProfit() == null || currentPrice == null) {
            return false;
        }
        return currentPrice.compareTo(alerts.getTakeProfit()) >= 0;
    }

    /**
     * Calcula los días de tenencia de la inversión
     */
    public long getHoldingPeriodDays() {
        if (firstPurchaseDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(
            firstPurchaseDate.toLocalDate(), 
            LocalDateTime.now().toLocalDate()
        );
    }

    /**
     * Verifica si la inversión necesita rebalanceado
     */
    public boolean needsRebalancing(BigDecimal targetAllocation, BigDecimal currentAllocation) {
        if (targetAllocation == null || currentAllocation == null) {
            return false;
        }
        BigDecimal difference = currentAllocation.subtract(targetAllocation).abs();
        return difference.compareTo(BigDecimal.valueOf(5.0)) > 0; // 5% threshold
    }
}
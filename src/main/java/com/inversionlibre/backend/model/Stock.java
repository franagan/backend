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
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Entidad Stock - Representa un activo financiero (acción, ETF, etc.)
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "stocks")
public class Stock {

    @Id
    private String id;

    @NotBlank(message = "El símbolo del stock es obligatorio")
    @Size(min = 1, max = 10, message = "El símbolo debe tener entre 1 y 10 caracteres")
    @Indexed(unique = true)
    private String symbol;

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    private String companyName;

    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    private String description;

    // Clasificación del activo
    @Builder.Default
    private AssetType assetType = AssetType.STOCK;

    @NotBlank(message = "El sector es obligatorio")
    private String sector;

    @NotBlank(message = "La industria es obligatoria")
    private String industry;

    @NotBlank(message = "El país es obligatorio")
    private String country;

    @NotBlank(message = "La moneda es obligatoria")
    @Builder.Default
    private String currency = "EUR";

    @NotBlank(message = "El intercambio es obligatorio")
    private String exchange;

    // Información de precios
    @NotNull(message = "El precio actual es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Digits(integer = 12, fraction = 4, message = "El precio debe tener máximo 12 dígitos enteros y 4 decimales")
    private BigDecimal currentPrice;

    @DecimalMin(value = "0.0", message = "El precio de apertura no puede ser negativo")
    @Digits(integer = 12, fraction = 4)
    private BigDecimal openPrice;

    @DecimalMin(value = "0.0", message = "El precio más alto no puede ser negativo")
    @Digits(integer = 12, fraction = 4)
    private BigDecimal highPrice;

    @DecimalMin(value = "0.0", message = "El precio más bajo no puede ser negativo")
    @Digits(integer = 12, fraction = 4)
    private BigDecimal lowPrice;

    @DecimalMin(value = "0.0", message = "El precio de cierre anterior no puede ser negativo")
    @Digits(integer = 12, fraction = 4)
    private BigDecimal previousClose;

    // Cambios y variaciones
    @Digits(integer = 12, fraction = 4)
    private BigDecimal change;

    @Digits(integer = 5, fraction = 4)
    private BigDecimal changePercent;

    // Volumen y capitalización
    @Min(value = 0, message = "El volumen no puede ser negativo")
    private Long volume;

    @Min(value = 0, message = "El volumen promedio no puede ser negativo")
    private Long avgVolume;

    @DecimalMin(value = "0.0", message = "La capitalización de mercado no puede ser negativa")
    @Digits(integer = 15, fraction = 2)
    private BigDecimal marketCap;

    // Métricas fundamentales
    private FundamentalData fundamentals;

    // Datos técnicos
    private TechnicalData technicals;

    // Dividendos
    private DividendInfo dividendInfo;

    // Estado del activo
    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isTradeable = true;

    @Builder.Default
    private MarketStatus marketStatus = MarketStatus.CLOSED;

    // Metadatos
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime lastPriceUpdate;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Enum para el tipo de activo
     */
    public enum AssetType {
        STOCK,          // Acción
        ETF,            // Fondo cotizado
        MUTUAL_FUND,    // Fondo de inversión
        BOND,           // Bono
        COMMODITY,      // Materia prima
        CRYPTO,         // Criptomoneda
        REIT,           // REIT (Real Estate Investment Trust)
        INDEX           // Índice
    }

    /**
     * Enum para el estado del mercado
     */
    public enum MarketStatus {
        OPEN,           // Mercado abierto
        CLOSED,         // Mercado cerrado
        PRE_MARKET,     // Pre-mercado
        AFTER_HOURS,    // Después del horario
        HALTED          // Suspendido
    }

    /**
     * Clase interna para datos fundamentales
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FundamentalData {
        private BigDecimal peRatio;          // Precio/Ganancia
        private BigDecimal pbRatio;          // Precio/Valor en libros
        private BigDecimal psRatio;          // Precio/Ventas
        private BigDecimal pegRatio;         // PEG Ratio
        private BigDecimal debtToEquity;     // Deuda/Patrimonio
        private BigDecimal roe;              // Rentabilidad sobre patrimonio
        private BigDecimal roa;              // Rentabilidad sobre activos
        private BigDecimal grossMargin;      // Margen bruto
        private BigDecimal operatingMargin;  // Margen operativo
        private BigDecimal netMargin;        // Margen neto
        private BigDecimal eps;              // Ganancias por acción
        private BigDecimal revenue;          // Ingresos
        private BigDecimal netIncome;        // Ingresos netos
        private Long sharesOutstanding;      // Acciones en circulación
        private BigDecimal bookValue;        // Valor en libros
        private LocalDate lastReportDate;    // Fecha del último reporte
    }

    /**
     * Clase interna para datos técnicos
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechnicalData {
        private BigDecimal sma20;            // Media móvil simple 20 días
        private BigDecimal sma50;            // Media móvil simple 50 días
        private BigDecimal sma200;           // Media móvil simple 200 días
        private BigDecimal ema12;            // Media móvil exponencial 12 días
        private BigDecimal ema26;            // Media móvil exponencial 26 días
        private BigDecimal rsi;              // Índice de fuerza relativa
        private BigDecimal macd;             // MACD
        private BigDecimal macdSignal;       // Señal MACD
        private BigDecimal macdHistogram;    // Histograma MACD
        private BigDecimal bollingerUpper;   // Banda de Bollinger superior
        private BigDecimal bollingerLower;   // Banda de Bollinger inferior
        private BigDecimal volatility;       // Volatilidad
        private BigDecimal beta;             // Beta
        private BigDecimal week52High;       // Máximo 52 semanas
        private BigDecimal week52Low;        // Mínimo 52 semanas
    }

    /**
     * Clase interna para información de dividendos
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DividendInfo {
        private BigDecimal annualDividend;       // Dividendo anual
        private BigDecimal dividendYield;        // Rentabilidad por dividendo
        private BigDecimal quarterlyDividend;    // Dividendo trimestral
        private LocalDate exDividendDate;        // Fecha ex-dividendo
        private LocalDate paymentDate;           // Fecha de pago
        private Integer paymentFrequency;        // Frecuencia de pago (veces por año)
        private Boolean paysDividends;           // Si paga dividendos
        private List<DividendPayment> history;   // Historial de dividendos
    }

    /**
     * Clase interna para pagos de dividendos individuales
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DividendPayment {
        private LocalDate date;
        private BigDecimal amount;
        private String type; // "Regular", "Special", "Extra"
    }

    /**
     * Actualiza el precio y calcula cambios
     */
    public void updatePrice(BigDecimal newPrice) {
        if (this.currentPrice != null) {
            this.change = newPrice.subtract(this.currentPrice);
            if (this.currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                this.changePercent = this.change
                    .divide(this.currentPrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }
        }
        this.currentPrice = newPrice;
        this.lastPriceUpdate = LocalDateTime.now();
    }

    /**
     * Verifica si el stock ha subido en precio
     */
    public boolean isPriceUp() {
        return change != null && change.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Verifica si el stock ha bajado en precio
     */
    public boolean isPriceDown() {
        return change != null && change.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Obtiene la variación de precio formateada
     */
    public String getFormattedChange() {
        if (change == null || changePercent == null) {
            return "N/A";
        }
        String sign = isPriceUp() ? "+" : "";
        return String.format("%s%.2f (%.2f%%)", sign, change, changePercent);
    }

    /**
     * Verifica si los datos necesitan actualización (más de 1 hora)
     */
    public boolean needsPriceUpdate() {
        if (lastPriceUpdate == null) {
            return true;
        }
        return lastPriceUpdate.isBefore(LocalDateTime.now().minusHours(1));
    }

    /**
     * Verifica si es una acción de gran capitalización
     */
    public boolean isLargeCap() {
        if (marketCap == null) {
            return false;
        }
        return marketCap.compareTo(BigDecimal.valueOf(10_000_000_000L)) >= 0; // >= 10B
    }

    /**
     * Verifica si paga dividendos
     */
    public boolean paysDividends() {
        return dividendInfo != null && 
               dividendInfo.getPaysDividends() != null && 
               dividendInfo.getPaysDividends();
    }
}
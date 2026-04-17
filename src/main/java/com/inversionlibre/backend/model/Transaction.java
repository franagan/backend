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
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Entidad Transaction - Representa una transacción individual (compra, venta, dividendo, etc.)
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    //@NotBlank(message = "El ID del usuario es obligatorio")
    @Indexed
    private String userId;

    @NotBlank(message = "El ID del portfolio es obligatorio")
    @Indexed
    private String portfolioId;

    //@NotBlank(message = "El ID de la inversión es obligatorio")
    @Indexed
    private String investmentId;

    @NotBlank(message = "El ID del stock es obligatorio")
    @Indexed
    private String stockId;

    @NotBlank(message = "El símbolo del stock es obligatorio")
    private String stockSymbol; // Desnormalizado para consultas rápidas

    @NotBlank(message = "El nombre del stock es obligatorio")
    private String stockName; // Desnormalizado para consultas rápidas

    // Tipo y estado de la transacción
    @NotNull(message = "El tipo de transacción es obligatorio")
    private TransactionType type;

    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    // Detalles de la transacción
    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad no puede ser negativa")
    @Digits(integer = 12, fraction = 8, message = "La cantidad debe tener máximo 12 dígitos enteros y 8 decimales")
    private BigDecimal quantity;

    @NotNull(message = "El precio por unidad es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Digits(integer = 12, fraction = 4, message = "El precio debe tener máximo 12 dígitos enteros y 4 decimales")
    private BigDecimal unitPrice;

    // Valores calculados
    @Builder.Default
    @DecimalMin(value = "0.0", message = "El valor bruto no puede ser negativo")
    @Digits(integer = 15, fraction = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    // Costos y comisiones
    @Builder.Default
    @DecimalMin(value = "0.0", message = "La comisión no puede ser negativa")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal commission = BigDecimal.ZERO;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "Las tasas no pueden ser negativas")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal fees = BigDecimal.ZERO;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "Los impuestos no pueden ser negativos")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal taxes = BigDecimal.ZERO;

    // Valor neto final
    @Builder.Default
    @Digits(integer = 15, fraction = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @NotBlank(message = "La moneda es obligatoria")
    @Builder.Default
    private String currency = "EUR";

    // Fechas importantes
    @NotNull(message = "La fecha de ejecución es obligatoria")
    private LocalDateTime executedAt;

    private LocalDateTime settlementDate; // Fecha de liquidación

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Información del broker/plataforma
    private String brokerId;
    private String brokerName;
    private String brokerOrderId;
    private String brokerReference;

    // Información adicional
    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notes;

    @Size(max = 100, message = "La fuente no puede exceder 100 caracteres")
    private String source; // Manual, API, Import, etc.

    // Metadatos específicos para diferentes tipos de transacción
    private DividendDetails dividendDetails;
    private SplitDetails splitDetails;
    private TransferDetails transferDetails;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Enum para tipos de transacción
     */
    public enum TransactionType {
        BUY,                // Compra
        SELL,               // Venta
        DIVIDEND,           // Dividendo recibido
        DIVIDEND_REINVEST,  // Reinversión de dividendo
        STOCK_SPLIT,        // División de acciones
        STOCK_MERGE,        // Fusión de acciones
        TRANSFER_IN,        // Transferencia entrante
        TRANSFER_OUT,       // Transferencia saliente
        ADJUSTMENT,         // Ajuste manual
        FEE,                // Comisión/tarifa
        INTEREST,           // Interés recibido
        BONUS_SHARES,       // Acciones bonus
        RIGHTS_ISSUE,       // Emisión de derechos
        CORPORATE_ACTION    // Acción corporativa
    }

    /**
     * Enum para estados de transacción
     */
    public enum TransactionStatus {
        PENDING,            // Pendiente
        EXECUTED,           // Ejecutada
        SETTLED,            // Liquidada
        CANCELLED,          // Cancelada
        FAILED,             // Fallida
        PARTIAL_FILLED,     // Parcialmente ejecutada
        REJECTED            // Rechazada
    }

    /**
     * Clase interna para detalles de dividendos
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DividendDetails {
        private BigDecimal dividendPerShare;     // Dividendo por acción
        private String dividendType;             // Regular, Special, Extra
        private LocalDateTime exDividendDate;    // Fecha ex-dividendo
        private LocalDateTime recordDate;        // Fecha de registro
        private LocalDateTime paymentDate;       // Fecha de pago
        private Boolean isReinvested;            // Si fue reinvertido
        private BigDecimal taxWithheld;          // Impuestos retenidos
        private String taxCountry;               // País de retención
    }

    /**
     * Clase interna para detalles de división de acciones
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SplitDetails {
        private BigDecimal splitRatio;           // Ratio de división (ej: 2.0 para 2:1)
        private String splitType;                // Split, Reverse Split
        private BigDecimal oldQuantity;          // Cantidad antes del split
        private BigDecimal newQuantity;          // Cantidad después del split
        private BigDecimal oldPrice;             // Precio antes del split
        private BigDecimal newPrice;             // Precio después del split
        private LocalDateTime effectiveDate;     // Fecha efectiva
    }

    /**
     * Clase interna para detalles de transferencias
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransferDetails {
        private String fromPortfolioId;          // Portfolio origen
        private String toPortfolioId;            // Portfolio destino
        private String fromBroker;               // Broker origen
        private String toBroker;                 // Broker destino
        private String transferReason;           // Razón de la transferencia
        private String transferReference;        // Referencia de la transferencia
        private LocalDateTime transferDate;      // Fecha de transferencia
    }

    /**
     * Calcula los valores derivados de la transacción
     */
    public void calculateAmounts() {
        if (quantity != null && unitPrice != null) {
            this.grossAmount = quantity.multiply(unitPrice);
        }

        BigDecimal totalCosts = BigDecimal.ZERO;
        if (commission != null) totalCosts = totalCosts.add(commission);
        if (fees != null) totalCosts = totalCosts.add(fees);
        if (taxes != null) totalCosts = totalCosts.add(taxes);

        if (grossAmount != null) {
            if (type == TransactionType.BUY) {
                // Para compras, el costo neto incluye los costos
                this.netAmount = grossAmount.add(totalCosts);
            } else if (type == TransactionType.SELL) {
                // Para ventas, los costos se restan del valor bruto
                this.netAmount = grossAmount.subtract(totalCosts);
            } else {
                // Para otros tipos (dividendos, etc.), generalmente se restan los costos
                this.netAmount = grossAmount.subtract(totalCosts);
            }
        }
    }

    /**
     * Verifica si es una transacción de compra
     */
    public boolean isPurchase() {
        return type == TransactionType.BUY || 
               type == TransactionType.DIVIDEND_REINVEST ||
               type == TransactionType.TRANSFER_IN ||
               type == TransactionType.BONUS_SHARES;
    }

    /**
     * Verifica si es una transacción de venta
     */
    public boolean isSale() {
        return type == TransactionType.SELL || 
               type == TransactionType.TRANSFER_OUT;
    }

    /**
     * Verifica si es una transacción de dividendo
     */
    public boolean isDividend() {
        return type == TransactionType.DIVIDEND || 
               type == TransactionType.DIVIDEND_REINVEST;
    }

    /**
     * Verifica si es una acción corporativa
     */
    public boolean isCorporateAction() {
        return type == TransactionType.STOCK_SPLIT ||
               type == TransactionType.STOCK_MERGE ||
               type == TransactionType.BONUS_SHARES ||
               type == TransactionType.RIGHTS_ISSUE ||
               type == TransactionType.CORPORATE_ACTION;
    }

    /**
     * Obtiene el costo total de la transacción
     */
    public BigDecimal getTotalCosts() {
        BigDecimal total = BigDecimal.ZERO;
        if (commission != null) total = total.add(commission);
        if (fees != null) total = total.add(fees);
        if (taxes != null) total = total.add(taxes);
        return total;
    }

    /**
     * Verifica si la transacción está completada
     */
    public boolean isCompleted() {
        return status == TransactionStatus.EXECUTED || 
               status == TransactionStatus.SETTLED;
    }

    /**
     * Verifica si la transacción está pendiente
     */
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    /**
     * Obtiene el impacto en la cantidad de acciones
     * Positivo para compras, negativo para ventas
     */
    public BigDecimal getQuantityImpact() {
        if (quantity == null) {
            return BigDecimal.ZERO;
        }

        if (isPurchase()) {
            return quantity;
        } else if (isSale()) {
            return quantity.negate();
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Obtiene el impacto monetario en el portfolio
     * Negativo para compras (sale dinero), positivo para ventas y dividendos
     */
    public BigDecimal getCashImpact() {
        if (netAmount == null) {
            return BigDecimal.ZERO;
        }

        if (type == TransactionType.BUY) {
            return netAmount.negate();
        } else if (type == TransactionType.SELL || isDividend()) {
            return netAmount;
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Formatea la transacción para mostrar
     */
    public String getFormattedSummary() {
        return String.format("%s %s %s @ %s %s", 
            type.name(), 
            quantity, 
            stockSymbol, 
            unitPrice, 
            currency);
    }

    /**
     * Verifica si la transacción afecta el cálculo de precio promedio
     */
    public boolean affectsAveragePrice() {
        return isPurchase() && !isCorporateAction();
    }
}
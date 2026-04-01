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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Alert - Representa una alerta de precio para un activo financiero
 *
 * Los usuarios pueden configurar alertas de tipo:
 * - STOP_LOSS: Alerta cuando el precio cae por debajo de un umbral
 * - TAKE_PROFIT: Alerta cuando el precio sube por encima de un umbral
 * - PRICE_TARGET: Alerta cuando el precio alcanza un objetivo
 *
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "alerts")
public class Alert {

    @Id
    private String id;

    // Usuario propietario de la alerta (seguridad)
    @NotBlank(message = "El ID del usuario es obligatorio")
    @Indexed
    private String userId;

    // Simbolo del activo financiero (ej: AAPL, MSFT, BTC)
    @NotBlank(message = "El simbolo del activo es obligatorio")
    @Indexed
    private String symbol;

    // Nombre del activo para visualizacion
    private String symbolName;

    // Tipo de alerta
    @NotNull(message = "El tipo de alerta es obligatorio")
    private AlertType alertType;

    // Precio objetivo que activa la alerta
    @NotNull(message = "El precio objetivo es obligatorio")
    @DecimalMin(value = "0.0001", message = "El precio debe ser mayor que 0")
    @Digits(integer = 12, fraction = 4, message = "El precio debe tener maximo 12 digitos enteros y 4 decimales")
    private BigDecimal targetPrice;

    // Precio actual del activo (cache del ultimo precio conocido)
    private BigDecimal currentPrice;

    // Estado de la alerta
    @Builder.Default
    private AlertStatus status = AlertStatus.ACTIVE;

    // Si la alerta esta activa
    @Builder.Default
    private Boolean isActive = true;

    // Fecha y hora cuando se disparo la alerta
    private LocalDateTime triggeredAt;

    // Mensaje de notificacion cuando se dispara
    private String triggerMessage;

    // Contador de veces que se ha disparado (para alertas recurrentes)
    @Builder.Default
    private Integer triggerCount = 0;

    // Notas del usuario sobre la alerta
    private String notes;

    // Configuracion de notificaciones
    @Builder.Default
    private Boolean emailNotification = true;

    @Builder.Default
    private Boolean pushNotification = true;

    // Para alertas recurrentes (volver a activar despues de dispararse)
    @Builder.Default
    private Boolean recurring = false;

    // Fecha de expiracion (opcional)
    private LocalDateTime expiresAt;

    // Metadatos
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Ultima vez que se verifico la alerta
    private LocalDateTime lastCheckedAt;

    /**
     * Enum para tipos de alerta
     */
    public enum AlertType {
        STOP_LOSS,      // Vender cuando el precio cae por debajo
        TAKE_PROFIT,    // Vender cuando el precio sube por encima
        PRICE_TARGET    // Notificar cuando el precio alcanza un objetivo
    }

    /**
     * Enum para estado de la alerta
     */
    public enum AlertStatus {
        ACTIVE,         // Alerta activa, pendiente de dispararse
        TRIGGERED,      // Alerta disparada, pendiente de notificacion
        NOTIFIED,       // Usuario ya notificado
        DISMISSED,      // Usuario descarto la alerta
        EXPIRED         // Alerta expirada sin dispararse
    }

    /**
     * Verifica si la alerta debe dispararse dado el precio actual
     *
     * @param currentPrice Precio actual del activo
     * @return true si la alerta debe dispararse
     */
    public boolean shouldTrigger(BigDecimal currentPrice) {
        if (currentPrice == null || !isActive || status != AlertStatus.ACTIVE) {
            return false;
        }

        // Verificar expiracion
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }

        return switch (alertType) {
            case STOP_LOSS -> currentPrice.compareTo(targetPrice) <= 0;
            case TAKE_PROFIT, PRICE_TARGET -> currentPrice.compareTo(targetPrice) >= 0;
        };
    }

    /**
     * Marca la alerta como disparada
     *
     * @param currentPrice Precio actual del activo
     * @return Mensaje descriptivo de la alerta
     */
    public String trigger(BigDecimal currentPrice) {
        this.triggeredAt = LocalDateTime.now();
        this.currentPrice = currentPrice;
        this.triggerCount++;
        this.status = AlertStatus.TRIGGERED;

        return switch (alertType) {
            case STOP_LOSS -> String.format("STOP LOSS: %s ha caido a $%s (objetivo: $%s)",
                    symbol, currentPrice.toPlainString(), targetPrice.toPlainString());
            case TAKE_PROFIT -> String.format("TAKE PROFIT: %s ha subido a $%s (objetivo: $%s)",
                    symbol, currentPrice.toPlainString(), targetPrice.toPlainString());
            case PRICE_TARGET -> String.format("OBJETIVO ALCANZADO: %s esta en $%s (objetivo: $%s)",
                    symbol, currentPrice.toPlainString(), targetPrice.toPlainString());
        };
    }

    /**
     * Verifica si la alerta ha expirado
     *
     * @return true si la alerta ha expirado
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Reactiva la alerta (para alertas recurrentes)
     */
    public void reactivate() {
        this.status = AlertStatus.ACTIVE;
        this.triggeredAt = null;
        this.triggerMessage = null;
        this.isActive = true;
    }

    /**
     * Descarta la alerta
     */
    public void dismiss() {
        this.status = AlertStatus.DISMISSED;
        this.isActive = false;
    }
}
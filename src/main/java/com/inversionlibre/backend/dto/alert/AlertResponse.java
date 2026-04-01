package com.inversionlibre.backend.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para alertas
 *
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {

    private String id;

    private String userId;

    private String symbol;

    private String symbolName;

    private String alertType;

    private BigDecimal targetPrice;

    private BigDecimal currentPrice;

    private String status;

    private Boolean isActive;

    private LocalDateTime triggeredAt;

    private String triggerMessage;

    private Integer triggerCount;

    private String notes;

    private Boolean emailNotification;

    private Boolean pushNotification;

    private Boolean recurring;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastCheckedAt;

    // Campos calculados
    private Double distancePercentage;

    private Boolean isExpired;

    private Boolean isNearTarget;
}
package com.inversionlibre.backend.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para actualizar una alerta existente
 *
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAlertRequest {

    @DecimalMin(value = "0.0001", message = "El precio debe ser mayor que 0")
    @Digits(integer = 12, fraction = 4, message = "El precio debe tener maximo 12 digitos y 4 decimales")
    private BigDecimal targetPrice;

    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notes;

    private Boolean emailNotification;

    private Boolean pushNotification;

    private Boolean recurring;

    private Boolean isActive;

    private LocalDateTime expiresAt;
}
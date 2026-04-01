package com.inversionlibre.backend.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para crear una nueva alerta
 *
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAlertRequest {

    @NotBlank(message = "El simbolo del activo es obligatorio")
    @Size(min = 1, max = 10, message = "El simbolo debe tener entre 1 y 10 caracteres")
    private String symbol;

    private String symbolName;

    @NotNull(message = "El tipo de alerta es obligatorio")
    private String alertType;

    @NotNull(message = "El precio objetivo es obligatorio")
    @DecimalMin(value = "0.0001", message = "El precio debe ser mayor que 0")
    @Digits(integer = 12, fraction = 4, message = "El precio debe tener maximo 12 digitos y 4 decimales")
    private BigDecimal targetPrice;

    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notes;

    @Builder.Default
    private Boolean emailNotification = true;

    @Builder.Default
    private Boolean pushNotification = true;

    @Builder.Default
    private Boolean recurring = false;

    private LocalDateTime expiresAt;
}
package com.inversionlibre.backend.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para el resumen de alertas de un usuario
 *
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertSummaryResponse {

    private Long totalAlerts;

    private Long activeAlerts;

    private Long triggeredAlerts;

    private Long stopLossAlerts;

    private Long takeProfitAlerts;

    private Long priceTargetAlerts;

    private List<AlertResponse> recentTriggered;

    private List<AlertResponse> nearTarget;
}
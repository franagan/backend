package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.dto.alert.*;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador REST para la gestion de alertas de precios
 *
 * Endpoints disponibles:
 * - POST /api/alerts - Crear alerta
 * - GET /api/alerts - Listar alertas del usuario
 * - GET /api/alerts/active - Alertas activas
 * - GET /api/alerts/triggered - Alertas disparadas
 * - GET /api/alerts/summary - Resumen de alertas
 * - GET /api/alerts/{id} - Obtener alerta especifica
 * - PUT /api/alerts/{id} - Actualizar alerta
 * - DELETE /api/alerts/{id} - Eliminar alerta
 * - POST /api/alerts/{id}/dismiss - Descartar alerta
 * - POST /api/alerts/{id}/reactivate - Reactivar alerta
 * - POST /api/alerts/refresh-price - Refrescar precio de simbolo
 *
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Alerts", description = "Endpoints para gestion de alertas de precios")
@SecurityRequirement(name = "Bearer Authentication")
public class AlertController {

    private final AlertService alertService;

    /**
     * Crea una nueva alerta
     */
    @PostMapping
    @Operation(summary = "Crear alerta", description = "Crea una nueva alerta de precio para un activo")
    public ResponseEntity<ApiResponse<AlertResponse>> createAlert(
            @Valid @RequestBody CreateAlertRequest request,
            @AuthenticationPrincipal User user) {

        log.info("Creando alerta para usuario {} - simbolo: {}", user.getId(), request.getSymbol());

        try {
            AlertResponse alert = alertService.createAlert(request, user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Alerta creada exitosamente", alert));

        } catch (IllegalArgumentException e) {
            log.warn("Error de validacion al crear alerta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error al crear alerta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al crear la alerta"));
        }
    }

    /**
     * Obtiene todas las alertas del usuario
     */
    @GetMapping
    @Operation(summary = "Listar alertas", description = "Obtiene todas las alertas del usuario autenticado")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlerts(
            @AuthenticationPrincipal User user) {

        log.info("Obteniendo alertas del usuario: {}", user.getId());

        try {
            List<AlertResponse> alerts = alertService.getAlertsByUser(user.getId());
            return ResponseEntity.ok(ApiResponse.success("Alertas obtenidas exitosamente", alerts));

        } catch (Exception e) {
            log.error("Error al obtener alertas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener las alertas"));
        }
    }

    /**
     * Obtiene alertas activas del usuario
     */
    @GetMapping("/active")
    @Operation(summary = "Alertas activas", description = "Obtiene solo las alertas activas del usuario")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getActiveAlerts(
            @AuthenticationPrincipal User user) {

        log.info("Obteniendo alertas activas del usuario: {}", user.getId());

        try {
            List<AlertResponse> alerts = alertService.getActiveAlerts(user.getId());
            return ResponseEntity.ok(ApiResponse.success("Alertas activas obtenidas", alerts));

        } catch (Exception e) {
            log.error("Error al obtener alertas activas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener las alertas activas"));
        }
    }

    /**
     * Obtiene alertas disparadas del usuario
     */
    @GetMapping("/triggered")
    @Operation(summary = "Alertas disparadas", description = "Obtiene las alertas que se han disparado")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getTriggeredAlerts(
            @AuthenticationPrincipal User user) {

        log.info("Obteniendo alertas disparadas del usuario: {}", user.getId());

        try {
            List<AlertResponse> alerts = alertService.getTriggeredAlerts(user.getId());
            return ResponseEntity.ok(ApiResponse.success("Alertas disparadas obtenidas", alerts));

        } catch (Exception e) {
            log.error("Error al obtener alertas disparadas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener las alertas disparadas"));
        }
    }

    /**
     * Obtiene resumen de alertas
     */
    @GetMapping("/summary")
    @Operation(summary = "Resumen de alertas", description = "Obtiene un resumen con estadisticas de las alertas")
    public ResponseEntity<ApiResponse<AlertSummaryResponse>> getAlertSummary(
            @AuthenticationPrincipal User user) {

        log.info("Obteniendo resumen de alertas del usuario: {}", user.getId());

        try {
            AlertSummaryResponse summary = alertService.getAlertSummary(user.getId());
            return ResponseEntity.ok(ApiResponse.success("Resumen obtenido", summary));

        } catch (Exception e) {
            log.error("Error al obtener resumen de alertas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener el resumen"));
        }
    }

    /**
     * Obtiene una alerta especifica por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener alerta", description = "Obtiene los detalles de una alerta especifica")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlert(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {

        log.info("Obteniendo alerta {} del usuario {}", id, user.getId());

        try {
            AlertResponse alert = alertService.getAlertById(id, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Alerta obtenida", alert));

        } catch (IllegalArgumentException e) {
            log.warn("Alerta no encontrada: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Alerta no encontrada"));

        } catch (SecurityException e) {
            log.warn("Acceso no autorizado a alerta {} por usuario {}", id, user.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para acceder a esta alerta"));

        } catch (Exception e) {
            log.error("Error al obtener alerta {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener la alerta"));
        }
    }

    /**
     * Actualiza una alerta existente
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar alerta", description = "Actualiza los datos de una alerta existente")
    public ResponseEntity<ApiResponse<AlertResponse>> updateAlert(
            @PathVariable String id,
            @Valid @RequestBody UpdateAlertRequest request,
            @AuthenticationPrincipal User user) {

        log.info("Actualizando alerta {} del usuario {}", id, user.getId());

        try {
            AlertResponse alert = alertService.updateAlert(id, request, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Alerta actualizada exitosamente", alert));

        } catch (IllegalArgumentException e) {
            log.warn("Alerta no encontrada: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Alerta no encontrada"));

        } catch (SecurityException e) {
            log.warn("Acceso no autorizado a alerta {} por usuario {}", id, user.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para modificar esta alerta"));

        } catch (Exception e) {
            log.error("Error al actualizar alerta {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al actualizar la alerta"));
        }
    }

    /**
     * Elimina una alerta
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar alerta", description = "Elimina una alerta del sistema")
    public ResponseEntity<ApiResponse<Void>> deleteAlert(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {

        log.info("Eliminando alerta {} del usuario {}", id, user.getId());

        try {
            alertService.deleteAlert(id, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Alerta eliminada exitosamente"));

        } catch (IllegalArgumentException e) {
            log.warn("Alerta no encontrada: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Alerta no encontrada"));

        } catch (SecurityException e) {
            log.warn("Acceso no autorizado a alerta {} por usuario {}", id, user.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para eliminar esta alerta"));

        } catch (Exception e) {
            log.error("Error al eliminar alerta {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al eliminar la alerta"));
        }
    }

    /**
     * Descarta una alerta disparada
     */
    @PostMapping("/{id}/dismiss")
    @Operation(summary = "Descartar alerta", description = "Descarta una alerta que ha sido disparada")
    public ResponseEntity<ApiResponse<AlertResponse>> dismissAlert(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {

        log.info("Descartando alerta {} del usuario {}", id, user.getId());

        try {
            AlertResponse alert = alertService.dismissAlert(id, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Alerta descartada", alert));

        } catch (IllegalArgumentException e) {
            log.warn("Alerta no encontrada: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Alerta no encontrada"));

        } catch (SecurityException e) {
            log.warn("Acceso no autorizado a alerta {} por usuario {}", id, user.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para descartar esta alerta"));

        } catch (Exception e) {
            log.error("Error al descartar alerta {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al descartar la alerta"));
        }
    }

    /**
     * Reactiva una alerta
     */
    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivar alerta", description = "Reactiva una alerta que fue descartada o disparada")
    public ResponseEntity<ApiResponse<AlertResponse>> reactivateAlert(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {

        log.info("Reactivando alerta {} del usuario {}", id, user.getId());

        try {
            AlertResponse alert = alertService.reactivateAlert(id, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Alerta reactivada", alert));

        } catch (IllegalArgumentException e) {
            log.warn("Alerta no encontrada: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Alerta no encontrada"));

        } catch (SecurityException e) {
            log.warn("Acceso no autorizado a alerta {} por usuario {}", id, user.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para reactivar esta alerta"));

        } catch (Exception e) {
            log.error("Error al reactivar alerta {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al reactivar la alerta"));
        }
    }

    /**
     * Refresca el precio de un simbolo
     */
    @PostMapping("/refresh-price")
    @Operation(summary = "Refrescar precio", description = "Fuerza la actualizacion del precio de un simbolo")
    public ResponseEntity<ApiResponse<BigDecimal>> refreshPrice(
            @Parameter(description = "Simbolo del activo") @RequestParam String symbol,
            @AuthenticationPrincipal User user) {

        log.info("Refrescando precio de {} para usuario {}", symbol, user.getId());

        try {
            BigDecimal price = alertService.forceRefreshPrice(symbol);

            if (price == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("No se pudo obtener el precio para " + symbol));
            }

            return ResponseEntity.ok(ApiResponse.success("Precio actualizado", price));

        } catch (Exception e) {
            log.error("Error al refrescar precio de {}: {}", symbol, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener el precio"));
        }
    }
}
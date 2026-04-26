package com.inversionlibre.backend.service;

import com.inversionlibre.backend.dto.alert.*;
import com.inversionlibre.backend.dto.stock.FinnhubQuote;
import com.inversionlibre.backend.model.Alert;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.repository.AlertRepository;
import com.inversionlibre.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de alertas de precios
 *
 * Contiene la lógica de negocio para:
 * - CRUD completo de alertas
 * - Verificación periódica de precios (@Scheduled)
 * - Integración con Finnhub API
 * - Sistema de notificaciones
 *
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Service
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final StockDataService stockDataService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public AlertService(AlertRepository alertRepository, StockDataService stockDataService, EmailService emailService, UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.stockDataService = stockDataService;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    // Cache en memoria para precios recientes (evita llamadas excesivas a Finnhub)
    private final Map<String, CachedPrice> priceCache = new ConcurrentHashMap<>();

    // Tiempo de vida del cache de precios (10 minutos)
    private static final int PRICE_CACHE_TTL_MINUTES = 10;

    // Porcentaje de distancia al objetivo para considerar "cerca"
    private static final double NEAR_TARGET_THRESHOLD = 5.0;

    // ===================================================================
    // OPERACIONES CRUD
    // ===================================================================

    /**
     * Crea una nueva alerta para un usuario
     *
     * @param request Datos de la alerta
     * @param user Usuario autenticado
     * @return Alerta creada
     */
    @Transactional
    @CacheEvict(value = "alerts", key = "#user.id")
    public AlertResponse createAlert(CreateAlertRequest request, User user) {
        log.info("Creando alerta para usuario {} - simbolo: {}, tipo: {}",
                user.getId(), request.getSymbol(), request.getAlertType());

        // Validar tipo de alerta
        Alert.AlertType alertType;
        try {
            alertType = Alert.AlertType.valueOf(request.getAlertType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de alerta no valido: " + request.getAlertType());
        }

        // Obtener precio actual del activo
        BigDecimal currentPrice = getCurrentPrice(request.getSymbol());
        if (currentPrice == null) {
            log.warn("No se pudo obtener precio para {}", request.getSymbol());
            // Continuamos sin precio actual, se actualizara en la primera verificacion
        }

        // Validar logica de precios segun tipo de alerta
        validateAlertPrice(alertType, request.getTargetPrice(), currentPrice);

        // Crear la alerta
        Alert alert = Alert.builder()
                .userId(user.getId())
                .symbol(request.getSymbol().toUpperCase())
                .symbolName(request.getSymbolName())
                .alertType(alertType)
                .targetPrice(request.getTargetPrice())
                .currentPrice(currentPrice)
                .status(Alert.AlertStatus.ACTIVE)
                .isActive(true)
                .notes(request.getNotes())
                .emailNotification(request.getEmailNotification() != null ? request.getEmailNotification() : true)
                .pushNotification(request.getPushNotification() != null ? request.getPushNotification() : true)
                .recurring(request.getRecurring() != null ? request.getRecurring() : false)
                .expiresAt(request.getExpiresAt())
                .triggerCount(0)
                .build();

        Alert savedAlert = alertRepository.save(alert);
        log.info("Alerta creada exitosamente con ID: {}", savedAlert.getId());

        return convertToResponse(savedAlert);
    }

    /**
     * Obtiene una alerta por ID, validando ownership
     *
     * @param alertId ID de la alerta
     * @param userId ID del usuario
     * @return Alerta encontrada
     */
    @Cacheable(value = "alerts", key = "#alertId")
    public AlertResponse getAlertById(String alertId, String userId) {
        log.debug("Obteniendo alerta: {}", alertId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada: " + alertId));

        // Validar ownership
        if (!alert.getUserId().equals(userId)) {
            throw new SecurityException("No tienes permiso para acceder a esta alerta");
        }

        return convertToResponse(alert);
    }

    /**
     * Obtiene todas las alertas de un usuario
     *
     * @param userId ID del usuario
     * @return Lista de alertas
     */
    public List<AlertResponse> getAlertsByUser(String userId) {
        log.debug("Obteniendo alertas del usuario: {}", userId);

        List<Alert> alerts = alertRepository.findByUserId(userId);
        return alerts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene alertas activas de un usuario
     *
     * @param userId ID del usuario
     * @return Lista de alertas activas
     */
    public List<AlertResponse> getActiveAlerts(String userId) {
        log.debug("Obteniendo alertas activas del usuario: {}", userId);

        List<Alert> alerts = alertRepository.findByUserIdAndIsActiveTrue(userId);
        return alerts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene alertas disparadas de un usuario
     *
     * @param userId ID del usuario
     * @return Lista de alertas disparadas
     */
    public List<AlertResponse> getTriggeredAlerts(String userId) {
        log.debug("Obteniendo alertas disparadas del usuario: {}", userId);

        List<Alert> alerts = alertRepository.findTriggeredAlertsByUserId(userId);
        return alerts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza una alerta existente
     *
     * @param alertId ID de la alerta
     * @param request Datos a actualizar
     * @param userId ID del usuario
     * @return Alerta actualizada
     */
    @Transactional
    @CacheEvict(value = "alerts", key = "#alertId")
    public AlertResponse updateAlert(String alertId, UpdateAlertRequest request, String userId) {
        log.info("Actualizando alerta: {}", alertId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada: " + alertId));

        // Validar ownership
        if (!alert.getUserId().equals(userId)) {
            throw new SecurityException("No tienes permiso para modificar esta alerta");
        }

        // Actualizar campos
        if (request.getTargetPrice() != null) {
            BigDecimal currentPrice = alert.getCurrentPrice();
            validateAlertPrice(alert.getAlertType(), request.getTargetPrice(), currentPrice);
            alert.setTargetPrice(request.getTargetPrice());
        }

        if (request.getNotes() != null) {
            alert.setNotes(request.getNotes());
        }

        if (request.getEmailNotification() != null) {
            alert.setEmailNotification(request.getEmailNotification());
        }

        if (request.getPushNotification() != null) {
            alert.setPushNotification(request.getPushNotification());
        }

        if (request.getRecurring() != null) {
            alert.setRecurring(request.getRecurring());
        }

        if (request.getIsActive() != null) {
            alert.setIsActive(request.getIsActive());
            if (!request.getIsActive()) {
                alert.setStatus(Alert.AlertStatus.DISMISSED);
            }
        }

        if (request.getExpiresAt() != null) {
            alert.setExpiresAt(request.getExpiresAt());
        }

        Alert updatedAlert = alertRepository.save(alert);
        log.info("Alerta actualizada exitosamente: {}", alertId);

        return convertToResponse(updatedAlert);
    }

    /**
     * Elimina una alerta
     *
     * @param alertId ID de la alerta
     * @param userId ID del usuario
     */
    @Transactional
    @CacheEvict(value = "alerts", key = "#alertId")
    public void deleteAlert(String alertId, String userId) {
        log.info("Eliminando alerta: {}", alertId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada: " + alertId));

        // Validar ownership
        if (!alert.getUserId().equals(userId)) {
            throw new SecurityException("No tienes permiso para eliminar esta alerta");
        }

        alertRepository.deleteById(alertId);
        log.info("Alerta eliminada exitosamente: {}", alertId);
    }

    /**
     * Descarta una alerta disparada
     *
     * @param alertId ID de la alerta
     * @param userId ID del usuario
     * @return Alerta actualizada
     */
    @Transactional
    @CacheEvict(value = "alerts", key = "#alertId")
    public AlertResponse dismissAlert(String alertId, String userId) {
        log.info("Descartando alerta: {}", alertId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada: " + alertId));

        // Validar ownership
        if (!alert.getUserId().equals(userId)) {
            throw new SecurityException("No tienes permiso para descartar esta alerta");
        }

        alert.dismiss();
        Alert updatedAlert = alertRepository.save(alert);

        return convertToResponse(updatedAlert);
    }

    /**
     * Reactiva una alerta
     *
     * @param alertId ID de la alerta
     * @param userId ID del usuario
     * @return Alerta reactivada
     */
    @Transactional
    @CacheEvict(value = "alerts", key = "#alertId")
    public AlertResponse reactivateAlert(String alertId, String userId) {
        log.info("Reactivando alerta: {}", alertId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alerta no encontrada: " + alertId));

        // Validar ownership
        if (!alert.getUserId().equals(userId)) {
            throw new SecurityException("No tienes permiso para reactivar esta alerta");
        }

        alert.reactivate();
        Alert updatedAlert = alertRepository.save(alert);

        return convertToResponse(updatedAlert);
    }

    /**
     * Obtiene resumen de alertas del usuario
     *
     * @param userId ID del usuario
     * @return Resumen de alertas
     */
    public AlertSummaryResponse getAlertSummary(String userId) {
        log.debug("Obteniendo resumen de alertas para: {}", userId);

        List<Alert> allAlerts = alertRepository.findByUserId(userId);

        long totalAlerts = allAlerts.size();
        long activeAlerts = allAlerts.stream().filter(Alert::getIsActive).count();
        long triggeredAlerts = allAlerts.stream()
                .filter(a -> a.getStatus() == Alert.AlertStatus.TRIGGERED || a.getStatus() == Alert.AlertStatus.NOTIFIED)
                .count();
        long stopLossAlerts = allAlerts.stream()
                .filter(a -> a.getAlertType() == Alert.AlertType.STOP_LOSS && a.getIsActive())
                .count();
        long takeProfitAlerts = allAlerts.stream()
                .filter(a -> a.getAlertType() == Alert.AlertType.TAKE_PROFIT && a.getIsActive())
                .count();
        long priceTargetAlerts = allAlerts.stream()
                .filter(a -> a.getAlertType() == Alert.AlertType.PRICE_TARGET && a.getIsActive())
                .count();

        // Alertas recientes disparadas (ultimas 24h)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<AlertResponse> recentTriggered = allAlerts.stream()
                .filter(a -> a.getTriggeredAt() != null && a.getTriggeredAt().isAfter(yesterday))
                .sorted((a1, a2) -> a2.getTriggeredAt().compareTo(a1.getTriggeredAt()))
                .limit(5)
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // Alertas cerca del objetivo
        List<AlertResponse> nearTarget = allAlerts.stream()
                .filter(Alert::getIsActive)
                .filter(a -> a.getStatus() == Alert.AlertStatus.ACTIVE)
                .filter(a -> a.getCurrentPrice() != null)
                .filter(this::isNearTarget)
                .sorted(Comparator.comparingDouble(this::calculateDistancePercentage))
                .limit(5)
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return AlertSummaryResponse.builder()
                .totalAlerts(totalAlerts)
                .activeAlerts(activeAlerts)
                .triggeredAlerts(triggeredAlerts)
                .stopLossAlerts(stopLossAlerts)
                .takeProfitAlerts(takeProfitAlerts)
                .priceTargetAlerts(priceTargetAlerts)
                .recentTriggered(recentTriggered)
                .nearTarget(nearTarget)
                .build();
    }

    // ===================================================================
    // SCHEDULER - VERIFICACION DE PRECIOS
    // ===================================================================

    /**
     * Verifica todas las alertas activas cada hora
     *
     * Cron: cada hora (0 0 * * * *)
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void checkAlerts() {
        log.debug("Iniciando verificacion de alertas programada");

        try {
            // Obtener todas las alertas activas
            List<Alert> activeAlerts = alertRepository.findAllActiveAlerts();

            if (activeAlerts.isEmpty()) {
                log.debug("No hay alertas activas para verificar");
                return;
            }

            log.info("Verificando {} alertas activas", activeAlerts.size());

            // Agrupar alertas por simbolo para minimizar llamadas a Finnhub
            Map<String, List<Alert>> alertsBySymbol = activeAlerts.stream()
                    .collect(Collectors.groupingBy(Alert::getSymbol));

            List<Alert> triggeredAlerts = new ArrayList<>();

            // Verificar cada simbolo
            for (Map.Entry<String, List<Alert>> entry : alertsBySymbol.entrySet()) {
                String symbol = entry.getKey();
                List<Alert> symbolAlerts = entry.getValue();

                try {
                    // Obtener precio actual
                    BigDecimal currentPrice = getCurrentPrice(symbol);

                    if (currentPrice == null) {
                        log.warn("No se pudo obtener precio para {}", symbol);
                        continue;
                    }

                    // Verificar cada alerta de este simbolo
                    for (Alert alert : symbolAlerts) {
                        try {
                            if (processAlert(alert, currentPrice)) {
                                triggeredAlerts.add(alert);
                            }
                        } catch (Exception e) {
                            log.error("Error procesando alerta {}: {}", alert.getId(), e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    log.error("Error obteniendo precio para {}: {}", symbol, e.getMessage());
                }
            }

            // Guardar alertas disparadas y enviar notificaciones
            if (!triggeredAlerts.isEmpty()) {
                processTriggeredAlerts(triggeredAlerts);
            }

            // Procesar alertas expiradas
            processExpiredAlerts();

            log.info("Verificacion completada. {} alertas disparadas", triggeredAlerts.size());

        } catch (Exception e) {
            log.error("Error en verificacion de alertas: {}", e.getMessage(), e);
        }
    }

    /**
     * Procesa una alerta individual
     *
     * @param alert Alerta a procesar
     * @param currentPrice Precio actual
     * @return true si la alerta se disparo
     */
    private boolean processAlert(Alert alert, BigDecimal currentPrice) {
        // Actualizar ultimo check
        alert.setLastCheckedAt(LocalDateTime.now());
        alert.setCurrentPrice(currentPrice);

        // Verificar expiracion
        if (alert.isExpired()) {
            alert.setStatus(Alert.AlertStatus.EXPIRED);
            alert.setIsActive(false);
            alertRepository.save(alert);
            log.info("Alerta {} expirada", alert.getId());
            return false;
        }

        // Verificar si se debe disparar
        if (alert.shouldTrigger(currentPrice)) {
            String message = alert.trigger(currentPrice);
            alert.setTriggerMessage(message);
            alertRepository.save(alert);
            log.info("Alerta {} disparada: {}", alert.getId(), message);
            return true;
        }

        // Guardar actualizacion de precio
        alertRepository.save(alert);
        return false;
    }

    /**
     * Procesa las alertas disparadas (envia notificaciones)
     *
     * @param triggeredAlerts Lista de alertas disparadas
     */
    private void processTriggeredAlerts(List<Alert> triggeredAlerts) {
        for (Alert alert : triggeredAlerts) {
            try {
                // Enviar notificación por email si está habilitada
                if (alert.getEmailNotification()) {
                    userRepository.findById(alert.getUserId()).ifPresent(user -> {
                        emailService.sendPriceAlertEmail(
                            user.getEmail(),
                            user.getFirstName(),
                            alert.getSymbol(),
                            alert.getSymbolName(),
                            alert.getAlertType().name(),
                            alert.getTargetPrice(),
                            alert.getCurrentPrice()
                        );
                        log.info("Email de alerta enviado a {} para {}", user.getEmail(), alert.getSymbol());
                    });
                }

                log.warn("NOTIFICACION: Usuario {} - {}", alert.getUserId(), alert.getTriggerMessage());

                // Si es recurrente, reactivar despues de notificar
                if (alert.getRecurring()) {
                    alert.reactivate();
                    alertRepository.save(alert);
                    log.info("Alerta {} reactivada (recurrente)", alert.getId());
                } else {
                    // Marcar como notificada
                    alert.setStatus(Alert.AlertStatus.NOTIFIED);
                    alert.setIsActive(false); // Una vez notificada, la desactivamos si no es recurrente
                    alertRepository.save(alert);
                }
            } catch (Exception e) {
                log.error("Error procesando notificacion para alerta {}: {}", alert.getId(), e.getMessage());
            }
        }
    }

    /**
     * Procesa alertas expiradas
     */
    private void processExpiredAlerts() {
        List<Alert> expiredAlerts = alertRepository.findExpiredAlerts(LocalDateTime.now());

        for (Alert alert : expiredAlerts) {
            alert.setStatus(Alert.AlertStatus.EXPIRED);
            alert.setIsActive(false);
            alertRepository.save(alert);
            log.info("Alerta {} expirada automaticamente", alert.getId());
        }
    }

    // ===================================================================
    // INTEGRACION CON FINNHUB
    // ===================================================================

    private BigDecimal getCurrentPrice(String symbol) {
        // Verificar cache
        CachedPrice cached = priceCache.get(symbol);
        if (cached != null && cached.isValid()) {
            return cached.getPrice();
        }

        // Obtener del servicio unificado de datos (con fallback automático)
        try {
            BigDecimal price = stockDataService.getStockPrice(symbol);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                // Guardar en cache
                priceCache.put(symbol, new CachedPrice(price));
                return price;
            }
        } catch (Exception e) {
            log.error("Error obteniendo precio para {} desde StockDataService: {}", symbol, e.getMessage());
        }

        return null;
    }

    /**
     * Fuerza actualizacion de precio para un simbolo
     *
     * @param symbol Simbolo del activo
     * @return Precio actualizado
     */
    public BigDecimal forceRefreshPrice(String symbol) {
        priceCache.remove(symbol);
        return getCurrentPrice(symbol);
    }

    // ===================================================================
    // METODOS AUXILIARES
    // ===================================================================

    /**
     * Valida la logica de precios segun el tipo de alerta
     */
    private void validateAlertPrice(Alert.AlertType alertType, BigDecimal targetPrice, BigDecimal currentPrice) {
        if (currentPrice == null) return; // No podemos validar sin precio actual

        switch (alertType) {
            case STOP_LOSS:
                if (targetPrice.compareTo(currentPrice) >= 0) {
                    throw new IllegalArgumentException(
                            "Para STOP_LOSS, el precio objetivo debe ser menor al precio actual ($" +
                                    currentPrice.toPlainString() + ")");
                }
                break;
            case TAKE_PROFIT:
                if (targetPrice.compareTo(currentPrice) <= 0) {
                    throw new IllegalArgumentException(
                            "Para TAKE_PROFIT, el precio objetivo debe ser mayor al precio actual ($" +
                                    currentPrice.toPlainString() + ")");
                }
                break;
            case PRICE_TARGET:
                // PRICE_TARGET puede ser cualquier precio
                break;
        }
    }

    /**
     * Calcula la distancia porcentual al objetivo
     */
    private double calculateDistancePercentage(Alert alert) {
        if (alert.getCurrentPrice() == null || alert.getTargetPrice() == null) return 100.0;

        BigDecimal current = alert.getCurrentPrice();
        BigDecimal target = alert.getTargetPrice();

        BigDecimal difference = target.subtract(current).abs();
        BigDecimal percentage = difference.divide(current, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return percentage.doubleValue();
    }

    /**
     * Verifica si una alerta esta cerca del objetivo
     */
    private boolean isNearTarget(Alert alert) {
        return calculateDistancePercentage(alert) <= NEAR_TARGET_THRESHOLD;
    }

    /**
     * Convierte entidad a DTO de respuesta
     */
    private AlertResponse convertToResponse(Alert alert) {
        Double distancePercentage = null;

        if (alert.getCurrentPrice() != null && alert.getTargetPrice() != null) {
            distancePercentage = calculateDistancePercentage(alert);
        }

        return AlertResponse.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .symbol(alert.getSymbol())
                .symbolName(alert.getSymbolName())
                .alertType(alert.getAlertType().name())
                .targetPrice(alert.getTargetPrice())
                .currentPrice(alert.getCurrentPrice())
                .status(alert.getStatus().name())
                .isActive(alert.getIsActive())
                .triggeredAt(alert.getTriggeredAt())
                .triggerMessage(alert.getTriggerMessage())
                .triggerCount(alert.getTriggerCount())
                .notes(alert.getNotes())
                .emailNotification(alert.getEmailNotification())
                .pushNotification(alert.getPushNotification())
                .recurring(alert.getRecurring())
                .expiresAt(alert.getExpiresAt())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .lastCheckedAt(alert.getLastCheckedAt())
                .distancePercentage(distancePercentage)
                .isExpired(alert.isExpired())
                .isNearTarget(distancePercentage != null && distancePercentage <= NEAR_TARGET_THRESHOLD)
                .build();
    }

    /**
     * Clase interna para cache de precios
     */
    private static class CachedPrice {
        private final BigDecimal price;
        private final LocalDateTime timestamp;

        public CachedPrice(BigDecimal price) {
            this.price = price;
            this.timestamp = LocalDateTime.now();
        }

        public BigDecimal getPrice() {
            return price;
        }

        public boolean isValid() {
            return timestamp.plusMinutes(PRICE_CACHE_TTL_MINUTES).isAfter(LocalDateTime.now());
        }
    }
}
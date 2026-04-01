package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad Alert
 * Proporciona operaciones CRUD y consultas personalizadas para alertas
 *
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Repository
public interface AlertRepository extends MongoRepository<Alert, String> {

    // ===================================================================
    // CONSULTAS POR USUARIO (SEGURIDAD)
    // ===================================================================

    /**
     * Encuentra todas las alertas de un usuario
     */
    List<Alert> findByUserId(String userId);

    /**
     * Encuentra alertas activas de un usuario
     */
    List<Alert> findByUserIdAndIsActiveTrue(String userId);

    /**
     * Encuentra alertas de un usuario por estado
     */
    List<Alert> findByUserIdAndStatus(String userId, Alert.AlertStatus status);

    /**
     * Encuentra alertas disparadas de un usuario
     */
    @Query("{ 'userId': ?0, 'status': { $in: ['TRIGGERED', 'NOTIFIED'] } }")
    List<Alert> findTriggeredAlertsByUserId(String userId);

    /**
     * Cuenta alertas activas de un usuario
     */
    long countByUserIdAndIsActiveTrue(String userId);

    /**
     * Cuenta alertas disparadas no notificadas de un usuario
     */
    long countByUserIdAndStatus(String userId, Alert.AlertStatus status);

    // ===================================================================
    // CONSULTAS POR SIMBOLO
    // ===================================================================

    /**
     * Encuentra alertas de un usuario para un simbolo especifico
     */
    List<Alert> findByUserIdAndSymbol(String userId, String symbol);

    /**
     * Encuentra todas las alertas para un simbolo (usado por el scheduler)
     */
    List<Alert> findBySymbolAndIsActiveTrue(String symbol);

    /**
     * Verifica si existe alerta activa para un simbolo de un usuario
     */
    boolean existsByUserIdAndSymbolAndIsActiveTrue(String userId, String symbol);

    // ===================================================================
    // CONSULTAS PARA SCHEDULER
    // ===================================================================

    /**
     * Encuentra todas las alertas activas que necesitan verificacion
     */
    @Query("{ 'isActive': true, 'status': 'ACTIVE' }")
    List<Alert> findAllActiveAlerts();

    /**
     * Encuentra alertas activas verificadas antes de una fecha
     */
    @Query("{ 'isActive': true, 'status': 'ACTIVE', 'lastCheckedAt': { $lt: ?0 } }")
    List<Alert> findAlertsNeedingCheck(LocalDateTime threshold);

    /**
     * Encuentra alertas expiradas
     */
    @Query("{ 'isActive': true, 'expiresAt': { $lt: ?0 }, 'status': 'ACTIVE' }")
    List<Alert> findExpiredAlerts(LocalDateTime now);

    // ===================================================================
    // CONSULTAS POR TIPO
    // ===================================================================

    /**
     * Encuentra alertas de un usuario por tipo
     */
    List<Alert> findByUserIdAndAlertType(String userId, Alert.AlertType alertType);

    /**
     * Encuentra alertas stop-loss activas
     */
    @Query("{ 'userId': ?0, 'alertType': 'STOP_LOSS', 'isActive': true }")
    List<Alert> findStopLossAlertsByUser(String userId);

    /**
     * Encuentra alertas take-profit activas
     */
    @Query("{ 'userId': ?0, 'alertType': 'TAKE_PROFIT', 'isActive': true }")
    List<Alert> findTakeProfitAlertsByUser(String userId);

    // ===================================================================
    // CONSULTAS POR FECHAS
    // ===================================================================

    /**
     * Encuentra alertas creadas despues de una fecha
     */
    List<Alert> findByUserIdAndCreatedAtAfter(String userId, LocalDateTime date);

    /**
     * Encuentra alertas disparadas en un rango de fechas
     */
    List<Alert> findByUserIdAndTriggeredAtBetween(String userId, LocalDateTime start, LocalDateTime end);

    /**
     * Encuentra alertas disparadas hoy
     */
    @Query("{ 'userId': ?0, 'triggeredAt': { $gte: ?1, $lte: ?2 } }")
    List<Alert> findTodayTriggeredAlerts(String userId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    // ===================================================================
    // ESTADISTICAS
    // ===================================================================

    /**
     * Cuenta alertas por tipo de un usuario
     */
    long countByUserIdAndAlertType(String userId, Alert.AlertType alertType);

    /**
     * Cuenta alertas disparadas de un usuario
     */
    @Query(value = "{ 'userId': ?0 }", count = true)
    long countTriggeredAlertsByUserId(String userId);
}
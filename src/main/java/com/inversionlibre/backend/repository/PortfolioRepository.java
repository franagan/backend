package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.Portfolio;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Portfolio
 * Proporciona operaciones CRUD y consultas personalizadas para carteras de inversión
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Repository
public interface PortfolioRepository extends MongoRepository<Portfolio, String> {

    // ===================================================================
    // CONSULTAS POR USUARIO
    // ===================================================================

    /**
     * Encuentra todos los portfolios de un usuario
     */
    List<Portfolio> findByUserId(String userId);

    /**
     * Encuentra portfolios de un usuario con paginación
     */
    Page<Portfolio> findByUserId(String userId, Pageable pageable);

    /**
     * Encuentra portfolios activos de un usuario
     */
    List<Portfolio> findByUserIdAndIsActiveTrue(String userId);

    /**
     * Encuentra portfolios públicos de un usuario
     */
    List<Portfolio> findByUserIdAndIsPublicTrue(String userId);

    /**
     * Cuenta portfolios de un usuario
     */
    long countByUserId(String userId);

    /**
     * Cuenta portfolios activos de un usuario
     */
    long countByUserIdAndIsActiveTrue(String userId);

    /**
     * Borra todos los portfolios de un usuario
     */
    void deleteByUserId(String userId);

    // ===================================================================
    // CONSULTAS POR TIPO Y ESTADO
    // ===================================================================

    /**
     * Encuentra portfolios por tipo
     */
    List<Portfolio> findByType(Portfolio.PortfolioType type);

    /**
     * Encuentra portfolios por nivel de riesgo
     */
    List<Portfolio> findByRiskLevel(Portfolio.RiskLevel riskLevel);

    /**
     * Encuentra portfolios activos
     */
    List<Portfolio> findByIsActiveTrue();

    /**
     * Encuentra portfolios públicos
     */
    List<Portfolio> findByIsPublicTrue();

    /**
     * Encuentra portfolios privados
     */
    List<Portfolio> findByIsPublicFalse();

    /**
     * Encuentra portfolios por tipo y usuario
     */
    List<Portfolio> findByUserIdAndType(String userId, Portfolio.PortfolioType type);

    /**
     * Encuentra portfolios por nivel de riesgo y usuario
     */
    List<Portfolio> findByUserIdAndRiskLevel(String userId, Portfolio.RiskLevel riskLevel);

    // ===================================================================
    // CONSULTAS POR VALOR Y RENDIMIENTO
    // ===================================================================

    /**
     * Encuentra portfolios con valor total mayor a una cantidad
     */
    List<Portfolio> findByTotalValueGreaterThan(BigDecimal value);

    /**
     * Encuentra portfolios con valor total entre dos cantidades
     */
    List<Portfolio> findByTotalValueBetween(BigDecimal minValue, BigDecimal maxValue);

    /**
     * Encuentra portfolios con ganancia positiva
     */
    @Query("{ 'totalGainLoss': { $gt: 0 } }")
    List<Portfolio> findProfitablePortfolios();

    /**
     * Encuentra portfolios con pérdidas
     */
    @Query("{ 'totalGainLoss': { $lt: 0 } }")
    List<Portfolio> findLosingPortfolios();

    /**
     * Encuentra portfolios con ganancia/pérdida mayor a un porcentaje
     */
    List<Portfolio> findByTotalGainLossPercentageGreaterThan(BigDecimal percentage);

    /**
     * Encuentra portfolios con ganancia/pérdida menor a un porcentaje
     */
    List<Portfolio> findByTotalGainLossPercentageLessThan(BigDecimal percentage);

    // ===================================================================
    // CONSULTAS POR MONEDA
    // ===================================================================

    /**
     * Encuentra portfolios por moneda
     */
    List<Portfolio> findByCurrency(String currency);

    /**
     * Encuentra portfolios de un usuario por moneda
     */
    List<Portfolio> findByUserIdAndCurrency(String userId, String currency);

    /**
     * Cuenta portfolios por moneda
     */
    long countByCurrency(String currency);

    // ===================================================================
    // CONSULTAS POR FECHAS
    // ===================================================================

    /**
     * Encuentra portfolios creados después de una fecha
     */
    List<Portfolio> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Encuentra portfolios creados entre dos fechas
     */
    List<Portfolio> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Encuentra portfolios actualizados recientemente
     */
    List<Portfolio> findByUpdatedAtAfter(LocalDateTime date);

    /**
     * Encuentra portfolios que necesitan rebalanceado
     */
    @Query("{ $or: [ " +
           "{ 'lastRebalanceAt': { $lt: ?0 } }, " +
           "{ 'lastRebalanceAt': null } " +
           "] }")
    List<Portfolio> findPortfoliosNeedingRebalance(LocalDateTime cutoffDate);

    /**
     * Encuentra portfolios no actualizados en X tiempo
     */
    @Query("{ 'lastUpdateAt': { $lt: ?0 } }")
    List<Portfolio> findPortfoliosNotUpdatedSince(LocalDateTime date);

    // ===================================================================
    // CONSULTAS POR INVERSIONES
    // ===================================================================

    /**
     * Encuentra portfolios que contienen una inversión específica
     */
    List<Portfolio> findByInvestmentIdsContaining(String investmentId);

    /**
     * Encuentra portfolios sin inversiones
     */
    @Query("{ $or: [ { 'investmentIds': { $size: 0 } }, { 'investmentIds': null } ] }")
    List<Portfolio> findEmptyPortfolios();

    /**
     * Encuentra portfolios con más de X inversiones
     */
    @Query("{ 'investmentIds': { $size: { $gte: ?0 } } }")
    List<Portfolio> findPortfoliosWithInvestmentsCountGreaterThan(int count);

    /**
     * Encuentra portfolios diversificados (>= 5 inversiones)
     */
    @Query("{ 'investmentIds': { $size: { $gte: 5 } } }")
    List<Portfolio> findDiversifiedPortfolios();

    // ===================================================================
    // CONSULTAS POR OBJETIVOS FINANCIEROS
    // ===================================================================

    /**
     * Encuentra portfolios con objetivos activos
     */
    @Query("{ 'goal.status': 'ACTIVE' }")
    List<Portfolio> findPortfoliosWithActiveGoals();

    /**
     * Encuentra portfolios con objetivos alcanzados
     */
    @Query("{ 'goal.status': 'ACHIEVED' }")
    List<Portfolio> findPortfoliosWithAchievedGoals();

    /**
     * Encuentra portfolios con objetivo de cantidad específica
     */
    @Query("{ 'goal.targetAmount': { $gte: ?0 } }")
    List<Portfolio> findPortfoliosWithTargetAmountGreaterThan(BigDecimal amount);

    /**
     * Encuentra portfolios con fecha objetivo próxima
     */
    @Query("{ 'goal.targetDate': { $lte: ?0 } }")
    List<Portfolio> findPortfoliosWithUpcomingTargetDate(LocalDateTime date);

    // ===================================================================
    // CONSULTAS DE BÚSQUEDA Y FILTRADO
    // ===================================================================

    /**
     * Busca portfolios por nombre (insensible a mayúsculas)
     */
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<Portfolio> findByNameContainingIgnoreCase(String name);

    /**
     * Busca portfolios por descripción
     */
    @Query("{ 'description': { $regex: ?0, $options: 'i' } }")
    List<Portfolio> findByDescriptionContainingIgnoreCase(String description);

    /**
     * Busca portfolios por nombre o descripción
     */
    @Query("{ $or: [ " +
           "{ 'name': { $regex: ?0, $options: 'i' } }, " +
           "{ 'description': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Portfolio> findByNameOrDescriptionContaining(String searchTerm);

    // ===================================================================
    // CONSULTAS DE RENDIMIENTO Y ANÁLISIS
    // ===================================================================

    /**
     * Encuentra portfolios ordenados por mejor rendimiento
     */
    List<Portfolio> findByIsActiveTrueOrderByTotalGainLossPercentageDesc();

    /**
     * Encuentra portfolios ordenados por valor total
     */
    List<Portfolio> findByIsActiveTrueOrderByTotalValueDesc();

    /**
     * Encuentra portfolios recientes
     */
    List<Portfolio> findTop10ByIsActiveTrueOrderByCreatedAtDesc();

    /**
     * Encuentra portfolios con mejor Sharpe ratio
     */
    @Query(value = "{ 'analytics.sharpeRatio': { $exists: true } }", 
           sort = "{ 'analytics.sharpeRatio': -1 }")
    List<Portfolio> findPortfoliosOrderedBySharpeRatio();

    /**
     * Encuentra portfolios con menor volatilidad
     */
    @Query(value = "{ 'analytics.volatility': { $exists: true } }", 
           sort = "{ 'analytics.volatility': 1 }")
    List<Portfolio> findPortfoliosOrderedByVolatility();

    // ===================================================================
    // CONSULTAS DE ESTADÍSTICAS Y REPORTING
    // ===================================================================

    /**
     * Cuenta portfolios por tipo
     */
    long countByType(Portfolio.PortfolioType type);

    /**
     * Cuenta portfolios por nivel de riesgo
     */
    long countByRiskLevel(Portfolio.RiskLevel riskLevel);

    /**
     * Cuenta portfolios activos
     */
    long countByIsActiveTrue();

    /**
     * Cuenta portfolios públicos
     */
    long countByIsPublicTrue();

    // ===================================================================
    // AGREGACIONES COMPLEJAS
    // ===================================================================

    /**
     * Suma total de valores de portfolios por usuario
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'userId': ?0, 'isActive': true } }",
    //     "{ '$group': { '_id': null, 'totalValue': { '$sum': '$totalValue' } } }"
    // })
    // Optional<BigDecimal> getTotalPortfolioValueByUser(String userId);

    /**
     * Obtiene estadísticas de portfolios por tipo
     */
    @Aggregation(pipeline = {
        "{ '$match': { 'isActive': true } }",
        "{ '$group': { " +
            "'_id': '$type', " +
            "'count': { '$sum': 1 }, " +
            "'totalValue': { '$sum': '$totalValue' }, " +
            "'avgValue': { '$avg': '$totalValue' } " +
        "} }"
    })
    List<Object> getPortfolioStatsByType();

    /**
     * Encuentra portfolios similares por tipo y nivel de riesgo
     */
    @Query("{ 'type': ?0, 'riskLevel': ?1, 'isActive': true, '_id': { $ne: ?2 } }")
    List<Portfolio> findSimilarPortfolios(Portfolio.PortfolioType type, 
                                         Portfolio.RiskLevel riskLevel, 
                                         String excludePortfolioId);

    /**
     * Búsqueda avanzada con múltiples filtros
     */
    @Query("{ $and: [ " +
           "{ 'userId': ?0 }, " +
           "{ $or: [ { 'type': { $in: ?1 } }, { 'type': { $exists: false } } ] }, " +
           "{ $or: [ { 'riskLevel': { $in: ?2 } }, { 'riskLevel': { $exists: false } } ] }, " +
           "{ 'isActive': ?3 }, " +
           "{ 'totalValue': { $gte: ?4, $lte: ?5 } } " +
           "] }")
    Page<Portfolio> findByAdvancedCriteria(String userId,
                                          List<Portfolio.PortfolioType> types,
                                          List<Portfolio.RiskLevel> riskLevels,
                                          Boolean isActive,
                                          BigDecimal minValue,
                                          BigDecimal maxValue,
                                          Pageable pageable);
}
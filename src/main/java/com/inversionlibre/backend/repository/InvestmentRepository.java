package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.Investment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Investment
 * Proporciona operaciones CRUD y consultas personalizadas para inversiones
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Repository
public interface InvestmentRepository extends MongoRepository<Investment, String> {

    // ===================================================================
    // CONSULTAS POR PORTFOLIO Y USUARIO
    // ===================================================================

    /**
     * Encuentra todas las inversiones de un portfolio
     */
    List<Investment> findByPortfolioId(String portfolioId);

    /**
     * Encuentra inversiones de un portfolio con paginación
     */
    Page<Investment> findByPortfolioId(String portfolioId, Pageable pageable);

    /**
     * Encuentra inversiones activas de un portfolio
     */
    List<Investment> findByPortfolioIdAndStatus(String portfolioId, Investment.InvestmentStatus status);

    /**
     * Encuentra inversiones de un portfolio por estado ordenadas por ganancia/pérdida ascendente
     */
    List<Investment> findByPortfolioIdAndStatusOrderByGainLossPercentageAsc(String portfolioId, Investment.InvestmentStatus status);

    /**
     * Cuenta inversiones en un portfolio
     */
    long countByPortfolioId(String portfolioId);

    /**
     * Cuenta inversiones activas en un portfolio
     */
    long countByPortfolioIdAndStatus(String portfolioId, Investment.InvestmentStatus status);

    // ===================================================================
    // CONSULTAS POR STOCK
    // ===================================================================

    /**
     * Encuentra inversiones de un stock específico
     */
    List<Investment> findByStockId(String stockId);

    /**
     * Encuentra inversiones por símbolo de stock
     */
    List<Investment> findByStockSymbol(String stockSymbol);

    /**
     * Encuentra una inversión específica por portfolio y stock ID
     */
    Optional<Investment> findByPortfolioIdAndStockId(String portfolioId, String stockId);

    /**
     * Encuentra una inversión específica por portfolio y símbolo de stock
     */
    Optional<Investment> findByPortfolioIdAndStockSymbol(String portfolioId, String stockSymbol);

    /**
     * Verifica si existe una inversión para un portfolio y stock ID
     */
    boolean existsByPortfolioIdAndStockId(String portfolioId, String stockId);

    /**
     * Verifica si existe una inversión para un portfolio y símbolo de stock
     */
    boolean existsByPortfolioIdAndStockSymbol(String portfolioId, String stockSymbol);

    // ===================================================================
    // CONSULTAS POR ESTRATEGIA Y ESTADO
    // ===================================================================

    /**
     * Encuentra inversiones por estrategia
     */
    List<Investment> findByStrategy(Investment.InvestmentStrategy strategy);

    /**
     * Encuentra inversiones por estado
     */
    List<Investment> findByStatus(Investment.InvestmentStatus status);

    /**
     * Encuentra inversiones activas
     */
    List<Investment> findByStatusNot(Investment.InvestmentStatus status);

    /**
     * Encuentra inversiones de un portfolio por estrategia
     */
    List<Investment> findByPortfolioIdAndStrategy(String portfolioId, Investment.InvestmentStrategy strategy);

    // ===================================================================
    // CONSULTAS POR VALOR Y RENDIMIENTO
    // ===================================================================

    /**
     * Encuentra inversiones con ganancia positiva
     */
    @Query("{ 'gainLoss': { $gt: 0 } }")
    List<Investment> findProfitableInvestments();

    /**
     * Encuentra inversiones con pérdidas
     */
    @Query("{ 'gainLoss': { $lt: 0 } }")
    List<Investment> findLosingInvestments();

    /**
     * Encuentra inversiones de un portfolio con ganancia
     */
    @Query("{ 'portfolioId': ?0, 'gainLoss': { $gt: 0 } }")
    List<Investment> findProfitableInvestmentsByPortfolio(String portfolioId);

    /**
     * Encuentra inversiones de un portfolio con pérdidas
     */
    @Query("{ 'portfolioId': ?0, 'gainLoss': { $lt: 0 } }")
    List<Investment> findLosingInvestmentsByPortfolio(String portfolioId);

    /**
     * Encuentra inversiones con ganancia porcentual mayor a un umbral
     */
    List<Investment> findByGainLossPercentageGreaterThan(BigDecimal percentage);

    /**
     * Encuentra inversiones con pérdida porcentual menor a un umbral
     */
    List<Investment> findByGainLossPercentageLessThan(BigDecimal percentage);

    /**
     * Encuentra inversiones con valor actual mayor a una cantidad
     */
    List<Investment> findByCurrentValueGreaterThan(BigDecimal value);

    /**
     * Encuentra inversiones con valor actual entre dos cantidades
     */
    List<Investment> findByCurrentValueBetween(BigDecimal minValue, BigDecimal maxValue);

    /**
     * Encuentra inversiones ordenadas por mejor rendimiento
     */
    List<Investment> findByStatusOrderByGainLossPercentageDesc(Investment.InvestmentStatus status);

    /**
     * Encuentra inversiones ordenadas por peor rendimiento
     */
    List<Investment> findByStatusOrderByGainLossPercentageAsc(Investment.InvestmentStatus status);

    // ===================================================================
    // CONSULTAS POR CANTIDAD Y PRECIO
    // ===================================================================

    /**
     * Encuentra inversiones con cantidad mayor a un valor
     */
    List<Investment> findByQuantityGreaterThan(BigDecimal quantity);

    /**
     * Encuentra inversiones con precio promedio mayor a un valor
     */
    List<Investment> findByAveragePriceGreaterThan(BigDecimal price);

    /**
     * Encuentra inversiones con precio promedio entre dos valores
     */
    List<Investment> findByAveragePriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Encuentra inversiones con valor total invertido mayor a una cantidad
     */
    List<Investment> findByTotalInvestedGreaterThan(BigDecimal amount);

    // ===================================================================
    // CONSULTAS POR ALERTAS Y OBJETIVOS
    // ===================================================================

    /**
     * Encuentra inversiones con alertas habilitadas
     */
    @Query("{ 'alerts.alertsEnabled': true }")
    List<Investment> findInvestmentsWithAlertsEnabled();

    /**
     * Encuentra inversiones que requieren alerta de stop loss
     */
    @Query("{ $and: [ " +
           "{ 'alerts.alertsEnabled': true }, " +
           "{ 'alerts.stopLoss': { $exists: true } }, " +
           "{ $expr: { $lte: ['$currentPrice', '$alerts.stopLoss'] } } " +
           "] }")
    List<Investment> findInvestmentsTriggeredStopLoss();

    /**
     * Encuentra inversiones que requieren alerta de take profit
     */
    @Query("{ $and: [ " +
           "{ 'alerts.alertsEnabled': true }, " +
           "{ 'alerts.takeProfit': { $exists: true } }, " +
           "{ $expr: { $gte: ['$currentPrice', '$alerts.takeProfit'] } } " +
           "] }")
    List<Investment> findInvestmentsTriggeredTakeProfit();

    /**
     * Encuentra inversiones con objetivos activos
     */
    @Query("{ 'goals.status': 'ACTIVE' }")
    List<Investment> findInvestmentsWithActiveGoals();

    /**
     * Encuentra inversiones con objetivos alcanzados
     */
    @Query("{ 'goals.status': 'ACHIEVED' }")
    List<Investment> findInvestmentsWithAchievedGoals();

    /**
     * Encuentra inversiones próximas a alcanzar objetivos
     */
    @Query("{ $and: [ " +
           "{ 'goals.status': 'ACTIVE' }, " +
           "{ 'goals.targetValue': { $exists: true } }, " +
           "{ $expr: { $gte: ['$currentValue', { $multiply: ['$goals.targetValue', 0.9] }] } } " +
           "] }")
    List<Investment> findInvestmentsNearGoalTarget();

    // ===================================================================
    // CONSULTAS POR FECHAS Y TRANSACCIONES
    // ===================================================================

    /**
     * Encuentra inversiones creadas después de una fecha
     */
    List<Investment> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Encuentra inversiones con primera compra después de una fecha
     */
    List<Investment> findByFirstPurchaseDateAfter(LocalDateTime date);

    /**
     * Encuentra inversiones con última transacción después de una fecha
     */
    List<Investment> findByLastTransactionDateAfter(LocalDateTime date);

    /**
     * Encuentra inversiones no actualizadas en X tiempo
     */
    @Query("{ 'lastPriceUpdate': { $lt: ?0 } }")
    List<Investment> findInvestmentsNotUpdatedSince(LocalDateTime date);

    /**
     * Encuentra inversiones con más de X transacciones
     */
    List<Investment> findByTotalTransactionsGreaterThan(Integer count);

    /**
     * Encuentra inversiones que contienen una transacción específica
     */
    List<Investment> findByTransactionIdsContaining(String transactionId);

    // ===================================================================
    // CONSULTAS DE ANÁLISIS Y RENDIMIENTO
    // ===================================================================

    /**
     * Encuentra inversiones con métricas de rendimiento calculadas
     */
    @Query("{ 'performance': { $exists: true, $ne: null } }")
    List<Investment> findInvestmentsWithPerformanceMetrics();

    /**
     * Encuentra inversiones con rentabilidad anualizada mayor a un porcentaje
     */
    @Query("{ 'performance.annualizedReturn': { $gte: ?0 } }")
    List<Investment> findByAnnualizedReturnGreaterThan(BigDecimal return_);

    /**
     * Encuentra inversiones con Sharpe ratio mayor a un valor
     */
    @Query("{ 'performance.sharpeRatio': { $gte: ?0 } }")
    List<Investment> findBySharpeRatioGreaterThan(BigDecimal ratio);

    /**
     * Encuentra inversiones con baja volatilidad
     */
    @Query("{ 'performance.volatility': { $lte: ?0 } }")
    List<Investment> findByVolatilityLessThan(BigDecimal volatility);

    /**
     * Encuentra inversiones con largo período de tenencia
     */
    @Query("{ 'performance.holdingPeriodDays': { $gte: ?0 } }")
    List<Investment> findByHoldingPeriodGreaterThan(Integer days);

    // ===================================================================
    // CONSULTAS DE DIVERSIFICACIÓN
    // ===================================================================

    /**
     * Encuentra inversiones agrupadas por stock symbol para análisis de concentración
     */
    @Aggregation(pipeline = {
        "{ '$match': { 'status': 'ACTIVE' } }",
        "{ '$group': { " +
            "'_id': '$stockSymbol', " +
            "'totalValue': { '$sum': '$currentValue' }, " +
            "'investmentCount': { '$sum': 1 }, " +
            "'portfolios': { '$addToSet': '$portfolioId' } " +
        "} }",
        "{ '$sort': { 'totalValue': -1 } }"
    })
    List<Object> getInvestmentConcentrationByStock();

    /**
     * Obtiene la distribución de inversiones por portfolio
     */
    @Aggregation(pipeline = {
        "{ '$match': { 'status': 'ACTIVE' } }",
        "{ '$group': { " +
            "'_id': '$portfolioId', " +
            "'totalValue': { '$sum': '$currentValue' }, " +
            "'investmentCount': { '$sum': 1 }, " +
            "'totalInvested': { '$sum': '$totalInvested' }, " +
            "'totalGainLoss': { '$sum': '$gainLoss' } " +
        "} }"
    })
    List<Object> getPortfolioInvestmentSummary();

    // ===================================================================
    // CONSULTAS AVANZADAS Y SCREENING
    // ===================================================================

    /**
     * Encuentra inversiones con potencial de rebalanceado
     * (Diferencia significativa entre precio actual y promedio)
     */
    @Query("{ $expr: { " +
           "$or: [ " +
             "{ $gte: ['$currentPrice', { $multiply: ['$averagePrice', 1.2] }] }, " +
             "{ $lte: ['$currentPrice', { $multiply: ['$averagePrice', 0.8] }] } " +
           "] " +
           "} }")
    List<Investment> findInvestmentsForRebalancing();

    /**
     * Encuentra inversiones underperforming en un portfolio
     */
    @Query("{ 'portfolioId': ?0, 'gainLossPercentage': { $lt: ?1 } }")
    List<Investment> findUnderperformingInvestments(String portfolioId, BigDecimal threshold);

    /**
     * Encuentra inversiones outperforming en un portfolio
     */
    @Query("{ 'portfolioId': ?0, 'gainLossPercentage': { $gt: ?1 } }")
    List<Investment> findOutperformingInvestments(String portfolioId, BigDecimal threshold);

    /**
     * Búsqueda avanzada con múltiples filtros
     */
    @Query("{ $and: [ " +
           "{ 'portfolioId': ?0 }, " +
           "{ $or: [ { 'strategy': { $in: ?1 } }, { 'strategy': { $exists: false } } ] }, " +
           "{ $or: [ { 'status': { $in: ?2 } }, { 'status': { $exists: false } } ] }, " +
           "{ 'currentValue': { $gte: ?3, $lte: ?4 } }, " +
           "{ 'gainLossPercentage': { $gte: ?5 } } " +
           "] }")
    Page<Investment> findByAdvancedCriteria(String portfolioId,
                                          List<Investment.InvestmentStrategy> strategies,
                                          List<Investment.InvestmentStatus> statuses,
                                          BigDecimal minValue,
                                          BigDecimal maxValue,
                                          BigDecimal minGainLoss,
                                          Pageable pageable);

    // ===================================================================
    // ESTADÍSTICAS Y REPORTING
    // ===================================================================

    /**
     * Cuenta inversiones por estrategia
     */
    long countByStrategy(Investment.InvestmentStrategy strategy);

    /**
     * Cuenta inversiones por estado
     */
    long countByStatus(Investment.InvestmentStatus status);

    /**
     * Obtiene valor total de inversiones en un portfolio
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'portfolioId': ?0, 'status': 'ACTIVE' } }",
    //     "{ '$group': { '_id': null, 'totalValue': { '$sum': '$currentValue' } } }"
    // })
    // Optional<BigDecimal> getTotalValueByPortfolio(String portfolioId);

    /**
     * Obtiene ganancia/pérdida total de inversiones en un portfolio
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'portfolioId': ?0, 'status': 'ACTIVE' } }",
    //     "{ '$group': { '_id': null, 'totalGainLoss': { '$sum': '$gainLoss' } } }"
    // })
    // Optional<BigDecimal> getTotalGainLossByPortfolio(String portfolioId);

    /**
     * Top inversiones por rendimiento en un portfolio
     */
    List<Investment> findTop10ByPortfolioIdAndStatusOrderByGainLossPercentageDesc(
        String portfolioId, Investment.InvestmentStatus status);

    /**
     * Inversiones más grandes por valor en un portfolio
     */
    List<Investment> findTop10ByPortfolioIdAndStatusOrderByCurrentValueDesc(
        String portfolioId, Investment.InvestmentStatus status);

    /**
     * Estadísticas de inversiones por estrategia
     */
    @Aggregation(pipeline = {
        "{ '$match': { 'status': 'ACTIVE' } }",
        "{ '$group': { " +
            "'_id': '$strategy', " +
            "'count': { '$sum': 1 }, " +
            "'totalValue': { '$sum': '$currentValue' }, " +
            "'avgGainLoss': { '$avg': '$gainLossPercentage' } " +
        "} }"
    })
    List<Object> getInvestmentStatsByStrategy();

    /**
     * Encuentra inversiones duplicadas (mismo portfolio y stock)
     */
    @Aggregation(pipeline = {
        "{ '$group': { " +
            "'_id': { 'portfolioId': '$portfolioId', 'stockId': '$stockId' }, " +
            "'count': { '$sum': 1 }, " +
            "'investments': { '$push': '$_id' } " +
        "} }",
        "{ '$match': { 'count': { '$gt': 1 } } }"
    })
    List<Object> findDuplicateInvestments();
}
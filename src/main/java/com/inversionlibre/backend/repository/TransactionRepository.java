package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.Transaction;
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
 * Repositorio para la entidad Transaction
 * Proporciona operaciones CRUD y consultas personalizadas para transacciones
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    // ===================================================================
    // CONSULTAS POR USUARIO Y PORTFOLIO
    // ===================================================================

    /**
     * Encuentra todas las transacciones de un usuario
     */
    List<Transaction> findByUserId(String userId);

    /**
     * Encuentra transacciones de un usuario con paginación
     */
    Page<Transaction> findByUserId(String userId, Pageable pageable);

    /**
     * Encuentra todas las transacciones de un portfolio
     */
    List<Transaction> findByPortfolioId(String portfolioId);

    /**
     * Encuentra transacciones de un portfolio con paginación
     */
    Page<Transaction> findByPortfolioId(String portfolioId, Pageable pageable);

    /**
     * Encuentra transacciones de una inversión específica
     */
    List<Transaction> findByInvestmentId(String investmentId);

    /**
     * Encuentra transacciones de un usuario y portfolio
     */
    List<Transaction> findByUserIdAndPortfolioId(String userId, String portfolioId);

    /**
     * Cuenta transacciones de un usuario
     */
    long countByUserId(String userId);

    /**
     * Cuenta transacciones de un portfolio
     */
    long countByPortfolioId(String portfolioId);

    // ===================================================================
    // CONSULTAS POR STOCK
    // ===================================================================

    /**
     * Encuentra transacciones de un stock específico
     */
    List<Transaction> findByStockId(String stockId);

    /**
     * Encuentra transacciones por símbolo de stock
     */
    List<Transaction> findByStockSymbol(String stockSymbol);

    /**
     * Encuentra transacciones de un stock en un portfolio
     */
    List<Transaction> findByPortfolioIdAndStockId(String portfolioId, String stockId);

    /**
     * Encuentra transacciones de un stock por usuario
     */
    List<Transaction> findByUserIdAndStockId(String userId, String stockId);

    // ===================================================================
    // CONSULTAS POR TIPO Y ESTADO
    // ===================================================================

    /**
     * Encuentra transacciones por tipo
     */
    List<Transaction> findByType(Transaction.TransactionType type);

    /**
     * Encuentra transacciones por estado
     */
    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    /**
     * Encuentra transacciones ejecutadas
     */
    List<Transaction> findByStatusIn(List<Transaction.TransactionStatus> statuses);

    /**
     * Encuentra transacciones pendientes
     */
    List<Transaction> findByStatus(Transaction.TransactionStatus status, Pageable pageable);

    /**
     * Encuentra compras de un portfolio
     */
    List<Transaction> findByPortfolioIdAndType(String portfolioId, Transaction.TransactionType type);

    /**
     * Encuentra ventas de un portfolio
     */
    @Query("{ 'portfolioId': ?0, 'type': 'SELL' }")
    List<Transaction> findSellTransactionsByPortfolio(String portfolioId);

    /**
     * Encuentra compras de un portfolio
     */
    @Query("{ 'portfolioId': ?0, 'type': 'BUY' }")
    List<Transaction> findBuyTransactionsByPortfolio(String portfolioId);

    /**
     * Encuentra dividendos recibidos por un portfolio
     */
    @Query("{ 'portfolioId': ?0, 'type': { $in: ['DIVIDEND', 'DIVIDEND_REINVEST'] } }")
    List<Transaction> findDividendTransactionsByPortfolio(String portfolioId);

    // ===================================================================
    // CONSULTAS POR FECHAS
    // ===================================================================

    /**
     * Encuentra transacciones ejecutadas después de una fecha
     */
    List<Transaction> findByExecutedAtAfter(LocalDateTime date);

    /**
     * Encuentra transacciones ejecutadas entre dos fechas
     */
    List<Transaction> findByExecutedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Encuentra transacciones creadas recientemente
     */
    List<Transaction> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Encuentra transacciones de un usuario en un período
     */
    List<Transaction> findByUserIdAndExecutedAtBetween(String userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Encuentra transacciones de un portfolio en un período
     */
    List<Transaction> findByPortfolioIdAndExecutedAtBetween(String portfolioId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Encuentra transacciones del día actual
     */
    @Query("{ 'executedAt': { " +
           "$gte: ?0, " +
           "$lt: ?1 " +
           "} }")
    List<Transaction> findTransactionsForDate(LocalDateTime startOfDay, LocalDateTime endOfDay);

    /**
     * Encuentra transacciones del mes actual
     */
    @Query("{ 'executedAt': { " +
           "$gte: ?0, " +
           "$lt: ?1 " +
           "} }")
    List<Transaction> findTransactionsForMonth(LocalDateTime startOfMonth, LocalDateTime endOfMonth);

    // ===================================================================
    // CONSULTAS POR VALOR Y CANTIDAD
    // ===================================================================

    /**
     * Encuentra transacciones con valor neto mayor a una cantidad
     */
    List<Transaction> findByNetAmountGreaterThan(BigDecimal amount);

    /**
     * Encuentra transacciones con valor neto entre dos cantidades
     */
    List<Transaction> findByNetAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);

    /**
     * Encuentra transacciones con cantidad mayor a un valor
     */
    List<Transaction> findByQuantityGreaterThan(BigDecimal quantity);

    /**
     * Encuentra transacciones grandes (por valor)
     */
    @Query("{ 'netAmount': { $gte: ?0 } }")
    List<Transaction> findLargeTransactions(BigDecimal threshold);

    /**
     * Encuentra transacciones con comisiones altas
     */
    @Query("{ 'commission': { $gte: ?0 } }")
    List<Transaction> findTransactionsWithHighCommissions(BigDecimal threshold);

    // ===================================================================
    // CONSULTAS POR MONEDA
    // ===================================================================

    /**
     * Encuentra transacciones por moneda
     */
    List<Transaction> findByCurrency(String currency);

    /**
     * Encuentra transacciones de un portfolio por moneda
     */
    List<Transaction> findByPortfolioIdAndCurrency(String portfolioId, String currency);

    /**
     * Cuenta transacciones por moneda
     */
    long countByCurrency(String currency);

    // ===================================================================
    // CONSULTAS POR BROKER
    // ===================================================================

    /**
     * Encuentra transacciones por broker
     */
    List<Transaction> findByBrokerId(String brokerId);

    /**
     * Encuentra transacciones por nombre de broker
     */
    List<Transaction> findByBrokerName(String brokerName);

    /**
     * Encuentra transacciones por referencia de broker
     */
    Optional<Transaction> findByBrokerOrderId(String brokerOrderId);

    /**
     * Cuenta transacciones por broker
     */
    long countByBrokerId(String brokerId);

    // ===================================================================
    // CONSULTAS POR FUENTE Y ORIGEN
    // ===================================================================

    /**
     * Encuentra transacciones por fuente (Manual, API, Import)
     */
    List<Transaction> findBySource(String source);

    /**
     * Encuentra transacciones manuales
     */
    @Query("{ 'source': 'Manual' }")
    List<Transaction> findManualTransactions();

    /**
     * Encuentra transacciones importadas
     */
    @Query("{ 'source': { $in: ['Import', 'CSV_Import', 'API'] } }")
    List<Transaction> findImportedTransactions();

    // ===================================================================
    // CONSULTAS DE ANÁLISIS DE ACTIVIDAD
    // ===================================================================

    /**
     * Encuentra transacciones más recientes de un usuario
     */
    List<Transaction> findTop10ByUserIdOrderByExecutedAtDesc(String userId);

    /**
     * Encuentra transacciones más recientes de un portfolio
     */
    List<Transaction> findTop10ByPortfolioIdOrderByExecutedAtDesc(String portfolioId);

    /**
     * Encuentra transacciones ordenadas por valor
     */
    List<Transaction> findByUserIdOrderByNetAmountDesc(String userId);

    /**
     * Encuentra actividad de trading reciente
     */
    @Query("{ 'type': { $in: ['BUY', 'SELL'] }, 'executedAt': { $gte: ?0 } }")
    List<Transaction> findRecentTradingActivity(LocalDateTime since);

    /**
     * Encuentra dividendos recientes
     */
    @Query("{ 'type': { $in: ['DIVIDEND', 'DIVIDEND_REINVEST'] }, 'executedAt': { $gte: ?0 } }")
    List<Transaction> findRecentDividends(LocalDateTime since);

    // ===================================================================
    // CONSULTAS ESPECIALES POR TIPO DE TRANSACCIÓN
    // ===================================================================

    /**
     * Encuentra acciones corporativas
     */
    @Query("{ 'type': { $in: ['STOCK_SPLIT', 'STOCK_MERGE', 'BONUS_SHARES', 'RIGHTS_ISSUE', 'CORPORATE_ACTION'] } }")
    List<Transaction> findCorporateActions();

    /**
     * Encuentra transferencias
     */
    @Query("{ 'type': { $in: ['TRANSFER_IN', 'TRANSFER_OUT'] } }")
    List<Transaction> findTransfers();

    /**
     * Encuentra transacciones de dividendos de un stock
     */
    @Query("{ 'stockId': ?0, 'type': { $in: ['DIVIDEND', 'DIVIDEND_REINVEST'] } }")
    List<Transaction> findDividendTransactionsByStock(String stockId);

    /**
     * Encuentra splits de un stock
     */
    @Query("{ 'stockId': ?0, 'type': { $in: ['STOCK_SPLIT', 'STOCK_MERGE'] } }")
    List<Transaction> findSplitTransactionsByStock(String stockId);

    // ===================================================================
    // AGREGACIONES Y ESTADÍSTICAS
    // ===================================================================

    /**
     * Suma total de compras por portfolio
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'portfolioId': ?0, 'type': 'BUY', 'status': { $in: ['EXECUTED', 'SETTLED'] } } }",
    //     "{ '$group': { '_id': null, 'totalPurchases': { '$sum': '$netAmount' } } }"
    // })
    // Optional<BigDecimal> getTotalPurchasesByPortfolio(String portfolioId);

    /**
     * Suma total de ventas por portfolio
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'portfolioId': ?0, 'type': 'SELL', 'status': { $in: ['EXECUTED', 'SETTLED'] } } }",
    //     "{ '$group': { '_id': null, 'totalSales': { '$sum': '$netAmount' } } }"
    // })
    // Optional<BigDecimal> getTotalSalesByPortfolio(String portfolioId);

    /**
     * Suma total de dividendos recibidos por portfolio
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'portfolioId': ?0, 'type': { $in: ['DIVIDEND', 'DIVIDEND_REINVEST'] }, 'status': { $in: ['EXECUTED', 'SETTLED'] } } }",
    //     "{ '$group': { '_id': null, 'totalDividends': { '$sum': '$netAmount' } } }"
    // })
    // Optional<BigDecimal> getTotalDividendsByPortfolio(String portfolioId);

    /**
     * Estadísticas de transacciones por tipo
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'status': { $in: ['EXECUTED', 'SETTLED'] } } }",
    //     "{ '$group': { " +
    //         "'_id': '$type', " +
    //         "'count': { '$sum': 1 }, " +
    //         "'totalAmount': { '$sum': '$netAmount' }, " +
    //         "'avgAmount': { '$avg': '$netAmount' } " +
    //     "} }"
    // })
    // List<Object> getTransactionStatsByType();

    /**
     * Volumen de trading mensual por usuario
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'userId': ?0, 'type': { $in: ['BUY', 'SELL'] }, 'executedAt': { $gte: ?1, $lt: ?2 } } }",
    //     "{ '$group': { " +
    //         "'_id': { " +
    //             "'year': { '$year': '$executedAt' }, " +
    //             "'month': { '$month': '$executedAt' } " +
    //         "}, " +
    //         "'totalVolume': { '$sum': '$grossAmount' }, " +
    //         "'transactionCount': { '$sum': 1 } " +
    //     "} }",
    //     "{ '$sort': { '_id.year': 1, '_id.month': 1 } }"
    // })
    // List<Object> getMonthlyTradingVolume(String userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Actividad de trading por stock
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'portfolioId': ?0, 'type': { $in: ['BUY', 'SELL'] } } }",
    //     "{ '$group': { " +
    //         "'_id': '$stockSymbol', " +
    //         "'buyCount': { '$sum': { '$cond': [{ '$eq': ['$type', 'BUY'] }, 1, 0] } }, " +
    //         "'sellCount': { '$sum': { '$cond': [{ '$eq': ['$type', 'SELL'] }, 1, 0] } }, " +
    //         "'totalVolume': { '$sum': '$grossAmount' } " +
    //     "} }",
    //     "{ '$sort': { 'totalVolume': -1 } }"
    // })
    // List<Object> getTradingActivityByStock(String portfolioId);

    /**
     * Comisiones totales pagadas por portfolio
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'portfolioId': ?0, 'status': { $in: ['EXECUTED', 'SETTLED'] } } }",
    //     "{ '$group': { " +
    //         "'_id': null, " +
    //         "'totalCommissions': { '$sum': '$commission' }, " +
    //         "'totalFees': { '$sum': '$fees' }, " +
    //         "'totalTaxes': { '$sum': '$taxes' } " +
    //     "} }"
    // })
    // Optional<Object> getTotalCostsByPortfolio(String portfolioId);

    // ===================================================================
    // CONSULTAS DE REPORTING Y ANÁLISIS
    // ===================================================================

    /**
     * Historial de transacciones para calcular precio promedio
     */
    @Query("{ 'investmentId': ?0, 'type': { $in: ['BUY', 'DIVIDEND_REINVEST'] }, 'status': { $in: ['EXECUTED', 'SETTLED'] } }")
    List<Transaction> findPurchaseTransactionsForInvestment(String investmentId);

    /**
     * Últimas N transacciones de un portfolio
     */
    Page<Transaction> findByPortfolioIdOrderByExecutedAtDesc(String portfolioId, Pageable pageable);

    /**
     * Transacciones para auditoría
     */
    @Query("{ 'userId': ?0, 'executedAt': { $gte: ?1, $lte: ?2 } }")
    List<Transaction> findTransactionsForAudit(String userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Encuentra transacciones sospechosas (alto valor, fuera de horario, etc.)
     */
    @Query("{ $or: [ " +
           "{ 'netAmount': { $gte: ?0 } }, " +
           "{ 'executedAt': { $lt: ?1 } }, " +
           "{ 'executedAt': { $gt: ?2 } } " +
           "] }")
    List<Transaction> findSuspiciousTransactions(BigDecimal highValueThreshold, 
                                               LocalDateTime marketOpen, 
                                               LocalDateTime marketClose);

    /**
     * Búsqueda avanzada con múltiples filtros
     */
    @Query("{ $and: [ " +
           "{ 'userId': ?0 }, " +
           "{ $or: [ { 'portfolioId': { $in: ?1 } }, { 'portfolioId': { $exists: false } } ] }, " +
           "{ $or: [ { 'type': { $in: ?2 } }, { 'type': { $exists: false } } ] }, " +
           "{ $or: [ { 'status': { $in: ?3 } }, { 'status': { $exists: false } } ] }, " +
           "{ 'executedAt': { $gte: ?4, $lte: ?5 } }, " +
           "{ 'netAmount': { $gte: ?6, $lte: ?7 } } " +
           "] }")
    Page<Transaction> findByAdvancedCriteria(String userId,
                                           List<String> portfolioIds,
                                           List<Transaction.TransactionType> types,
                                           List<Transaction.TransactionStatus> statuses,
                                           LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           BigDecimal minAmount,
                                           BigDecimal maxAmount,
                                           Pageable pageable);

    /**
     * Estadísticas generales de un usuario
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // @Aggregation(pipeline = {
    //     "{ '$match': { 'userId': ?0 } }",
    //     "{ '$group': { " +
    //         "'_id': null, " +
    //         "'totalTransactions': { '$sum': 1 }, " +
    //         "'totalVolume': { '$sum': '$grossAmount' }, " +
    //         "'totalCommissions': { '$sum': '$commission' }, " +
    //         "'firstTransaction': { '$min': '$executedAt' }, " +
    //         "'lastTransaction': { '$max': '$executedAt' } " +
    //     "} }"
    // })
    // Optional<Object> getUserTransactionStats(String userId);
}
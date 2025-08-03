package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.Stock;
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
 * Repositorio para la entidad Stock
 * Proporciona operaciones CRUD y consultas personalizadas para activos financieros
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Repository
public interface StockRepository extends MongoRepository<Stock, String> {

    // ===================================================================
    // CONSULTAS BÁSICAS POR IDENTIFICADORES
    // ===================================================================

    /**
     * Busca un stock por símbolo (único)
     */
    Optional<Stock> findBySymbol(String symbol);

    /**
     * Verifica si existe un stock con el símbolo dado
     */
    boolean existsBySymbol(String symbol);

    /**
     * Busca stocks por nombre de empresa (insensible a mayúsculas)
     */
    @Query("{ 'companyName': { $regex: ?0, $options: 'i' } }")
    List<Stock> findByCompanyNameContainingIgnoreCase(String companyName);

    /**
     * Busca stocks por símbolo que contenga el texto
     */
    @Query("{ 'symbol': { $regex: ?0, $options: 'i' } }")
    List<Stock> findBySymbolContainingIgnoreCase(String symbolPart);

    // ===================================================================
    // CONSULTAS POR CLASIFICACIÓN
    // ===================================================================

    /**
     * Encuentra stocks por tipo de activo
     */
    List<Stock> findByAssetType(Stock.AssetType assetType);

    /**
     * Encuentra stocks por sector
     */
    List<Stock> findBySector(String sector);

    /**
     * Encuentra stocks por industria
     */
    List<Stock> findByIndustry(String industry);

    /**
     * Encuentra stocks por país
     */
    List<Stock> findByCountry(String country);

    /**
     * Encuentra stocks por intercambio/bolsa
     */
    List<Stock> findByExchange(String exchange);

    /**
     * Encuentra stocks por moneda
     */
    List<Stock> findByCurrency(String currency);

    /**
     * Encuentra stocks por sector y país
     */
    List<Stock> findBySectorAndCountry(String sector, String country);

    /**
     * Encuentra stocks por tipo y intercambio
     */
    List<Stock> findByAssetTypeAndExchange(Stock.AssetType assetType, String exchange);

    // ===================================================================
    // CONSULTAS POR PRECIO Y CAPITALIZACIÓN
    // ===================================================================

    /**
     * Encuentra stocks con precio mayor a una cantidad
     */
    List<Stock> findByCurrentPriceGreaterThan(BigDecimal price);

    /**
     * Encuentra stocks con precio entre dos valores
     */
    List<Stock> findByCurrentPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Encuentra stocks con capitalización mayor a una cantidad
     */
    List<Stock> findByMarketCapGreaterThan(BigDecimal marketCap);

    /**
     * Encuentra stocks con capitalización entre dos valores
     */
    List<Stock> findByMarketCapBetween(BigDecimal minMarketCap, BigDecimal maxMarketCap);

    /**
     * Encuentra stocks de gran capitalización (>= 10B)
     */
    @Query("{ 'marketCap': { $gte: 10000000000 } }")
    List<Stock> findLargeCapStocks();

    /**
     * Encuentra stocks de mediana capitalización (2B - 10B)
     */
    @Query("{ 'marketCap': { $gte: 2000000000, $lt: 10000000000 } }")
    List<Stock> findMidCapStocks();

    /**
     * Encuentra stocks de pequeña capitalización (< 2B)
     */
    @Query("{ 'marketCap': { $lt: 2000000000 } }")
    List<Stock> findSmallCapStocks();

    // ===================================================================
    // CONSULTAS POR RENDIMIENTO Y CAMBIOS
    // ===================================================================

    /**
     * Encuentra stocks con ganancia positiva en el día
     */
    @Query("{ 'change': { $gt: 0 } }")
    List<Stock> findStocksWithPositiveChange();

    /**
     * Encuentra stocks con pérdida en el día
     */
    @Query("{ 'change': { $lt: 0 } }")
    List<Stock> findStocksWithNegativeChange();

    /**
     * Encuentra stocks con cambio porcentual mayor a un umbral
     */
    List<Stock> findByChangePercentGreaterThan(BigDecimal percentage);

    /**
     * Encuentra stocks con cambio porcentual menor a un umbral
     */
    List<Stock> findByChangePercentLessThan(BigDecimal percentage);

    /**
     * Encuentra stocks ordenados por mejor rendimiento del día
     */
    List<Stock> findByIsActiveTrueOrderByChangePercentDesc();

    /**
     * Encuentra stocks ordenados por peor rendimiento del día
     */
    List<Stock> findByIsActiveTrueOrderByChangePercentAsc();

    // ===================================================================
    // CONSULTAS POR VOLUMEN
    // ===================================================================

    /**
     * Encuentra stocks con volumen mayor a una cantidad
     */
    List<Stock> findByVolumeGreaterThan(Long volume);

    /**
     * Encuentra stocks con volumen alto (mayor al promedio)
     */
    @Query("{ $expr: { $gt: ['$volume', '$avgVolume'] } }")
    List<Stock> findStocksWithHighVolume();

    /**
     * Encuentra stocks ordenados por mayor volumen
     */
    List<Stock> findByIsActiveTrueOrderByVolumeDesc();

    // ===================================================================
    // CONSULTAS POR ESTADO Y ACTIVIDAD
    // ===================================================================

    /**
     * Encuentra stocks activos
     */
    List<Stock> findByIsActiveTrue();

    /**
     * Encuentra stocks comercializables
     */
    List<Stock> findByIsTradeableTrue();

    /**
     * Encuentra stocks activos y comercializables
     */
    List<Stock> findByIsActiveTrueAndIsTradeableTrue();

    /**
     * Encuentra stocks por estado del mercado
     */
    List<Stock> findByMarketStatus(Stock.MarketStatus marketStatus);

    /**
     * Encuentra stocks con mercado abierto
     */
    List<Stock> findByMarketStatusAndIsActiveTrueAndIsTradeableTrue(Stock.MarketStatus marketStatus);

    // ===================================================================
    // CONSULTAS POR DIVIDENDOS
    // ===================================================================

    /**
     * Encuentra stocks que pagan dividendos
     */
    @Query("{ 'dividendInfo.paysDividends': true }")
    List<Stock> findDividendPayingStocks();

    /**
     * Encuentra stocks con rentabilidad por dividendo mayor a un porcentaje
     */
    @Query("{ 'dividendInfo.dividendYield': { $gte: ?0 } }")
    List<Stock> findByDividendYieldGreaterThan(BigDecimal yield);

    /**
     * Encuentra stocks ordenados por mayor rentabilidad por dividendo
     */
    @Query(value = "{ 'dividendInfo.paysDividends': true }", 
           sort = "{ 'dividendInfo.dividendYield': -1 }")
    List<Stock> findDividendStocksOrderedByYield();

    // ===================================================================
    // CONSULTAS POR MÉTRICAS FUNDAMENTALES
    // ===================================================================

    /**
     * Encuentra stocks con P/E ratio menor a un valor (value investing)
     */
    @Query("{ 'fundamentals.peRatio': { $lte: ?0, $gt: 0 } }")
    List<Stock> findByPeRatioLessThan(BigDecimal peRatio);

    /**
     * Encuentra stocks con P/B ratio menor a un valor
     */
    @Query("{ 'fundamentals.pbRatio': { $lte: ?0, $gt: 0 } }")
    List<Stock> findByPbRatioLessThan(BigDecimal pbRatio);

    /**
     * Encuentra stocks con ROE mayor a un porcentaje
     */
    @Query("{ 'fundamentals.roe': { $gte: ?0 } }")
    List<Stock> findByRoeGreaterThan(BigDecimal roe);

    /**
     * Encuentra stocks con margen neto mayor a un porcentaje
     */
    @Query("{ 'fundamentals.netMargin': { $gte: ?0 } }")
    List<Stock> findByNetMarginGreaterThan(BigDecimal margin);

    // ===================================================================
    // CONSULTAS POR ANÁLISIS TÉCNICO
    // ===================================================================

    /**
     * Encuentra stocks con precio por encima de media móvil 50
     */
    @Query("{ $expr: { $gt: ['$currentPrice', '$technicals.sma50'] } }")
    List<Stock> findStocksAboveSma50();

    /**
     * Encuentra stocks con precio por encima de media móvil 200
     */
    @Query("{ $expr: { $gt: ['$currentPrice', '$technicals.sma200'] } }")
    List<Stock> findStocksAboveSma200();

    /**
     * Encuentra stocks con RSI sobreventa (< 30)
     */
    @Query("{ 'technicals.rsi': { $lt: 30 } }")
    List<Stock> findOversoldStocks();

    /**
     * Encuentra stocks con RSI sobrecompra (> 70)
     */
    @Query("{ 'technicals.rsi': { $gt: 70 } }")
    List<Stock> findOverboughtStocks();

    /**
     * Encuentra stocks cerca de máximos de 52 semanas
     */
    @Query("{ $expr: { $gte: ['$currentPrice', { $multiply: ['$technicals.week52High', 0.95] }] } }")
    List<Stock> findStocksNear52WeekHigh();

    /**
     * Encuentra stocks cerca de mínimos de 52 semanas
     */
    @Query("{ $expr: { $lte: ['$currentPrice', { $multiply: ['$technicals.week52Low', 1.05] }] } }")
    List<Stock> findStocksNear52WeekLow();

    // ===================================================================
    // CONSULTAS POR FECHAS Y ACTUALIZACIONES
    // ===================================================================

    /**
     * Encuentra stocks que necesitan actualización de precio
     */
    @Query("{ $or: [ " +
           "{ 'lastPriceUpdate': { $lt: ?0 } }, " +
           "{ 'lastPriceUpdate': null } " +
           "] }")
    List<Stock> findStocksNeedingPriceUpdate(LocalDateTime cutoffTime);

    /**
     * Encuentra stocks actualizados recientemente
     */
    List<Stock> findByLastPriceUpdateAfter(LocalDateTime dateTime);

    /**
     * Encuentra stocks añadidos recientemente
     */
    List<Stock> findByCreatedAtAfter(LocalDateTime dateTime);

    // ===================================================================
    // BÚSQUEDAS Y FILTRADOS
    // ===================================================================

    /**
     * Búsqueda general por símbolo o nombre de empresa
     */
    @Query("{ $or: [ " +
           "{ 'symbol': { $regex: ?0, $options: 'i' } }, " +
           "{ 'companyName': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Stock> findBySymbolOrCompanyNameContaining(String searchTerm);

    /**
     * Búsqueda avanzada con múltiples filtros
     */
    @Query("{ $and: [ " +
           "{ $or: [ { 'assetType': { $in: ?0 } }, { 'assetType': { $exists: false } } ] }, " +
           "{ $or: [ { 'sector': { $in: ?1 } }, { 'sector': { $exists: false } } ] }, " +
           "{ $or: [ { 'country': { $in: ?2 } }, { 'country': { $exists: false } } ] }, " +
           "{ 'currentPrice': { $gte: ?3, $lte: ?4 } }, " +
           "{ 'isActive': ?5 } " +
           "] }")
    Page<Stock> findByAdvancedCriteria(List<Stock.AssetType> assetTypes,
                                      List<String> sectors,
                                      List<String> countries,
                                      BigDecimal minPrice,
                                      BigDecimal maxPrice,
                                      Boolean isActive,
                                      Pageable pageable);

    // ===================================================================
    // SCREENING Y ANÁLISIS
    // ===================================================================

    /**
     * Screening de valor (P/E bajo, P/B bajo, dividendo alto)
     */
    @Query("{ $and: [ " +
           "{ 'fundamentals.peRatio': { $lte: ?0, $gt: 0 } }, " +
           "{ 'fundamentals.pbRatio': { $lte: ?1, $gt: 0 } }, " +
           "{ 'dividendInfo.dividendYield': { $gte: ?2 } }, " +
           "{ 'isActive': true } " +
           "] }")
    List<Stock> findValueStocks(BigDecimal maxPe, BigDecimal maxPb, BigDecimal minDividendYield);

    /**
     * Screening de crecimiento (ROE alto, margen alto, bajo P/E)
     */
    @Query("{ $and: [ " +
           "{ 'fundamentals.roe': { $gte: ?0 } }, " +
           "{ 'fundamentals.netMargin': { $gte: ?1 } }, " +
           "{ 'fundamentals.peRatio': { $lte: ?2, $gt: 0 } }, " +
           "{ 'isActive': true } " +
           "] }")
    List<Stock> findGrowthStocks(BigDecimal minRoe, BigDecimal minMargin, BigDecimal maxPe);

    /**
     * Encuentra stocks con momentum positivo
     */
    @Query("{ $and: [ " +
           "{ $expr: { $gt: ['$currentPrice', '$technicals.sma50'] } }, " +
           "{ $expr: { $gt: ['$technicals.sma50', '$technicals.sma200'] } }, " +
           "{ 'changePercent': { $gt: 0 } }, " +
           "{ 'isActive': true } " +
           "] }")
    List<Stock> findMomentumStocks();

    // ===================================================================
    // ESTADÍSTICAS Y AGREGACIONES
    // ===================================================================

    /**
     * Cuenta stocks por tipo de activo
     */
    long countByAssetType(Stock.AssetType assetType);

    /**
     * Cuenta stocks por sector
     */
    long countBySector(String sector);

    /**
     * Cuenta stocks activos
     */
    long countByIsActiveTrue();

    /**
     * Obtiene sectores únicos
     */
    @Aggregation(pipeline = {
        "{ '$group': { '_id': '$sector' } }",
        "{ '$sort': { '_id': 1 } }"
    })
    List<String> findDistinctSectors();

    /**
     * Obtiene países únicos
     */
    @Aggregation(pipeline = {
        "{ '$group': { '_id': '$country' } }",
        "{ '$sort': { '_id': 1 } }"
    })
    List<String> findDistinctCountries();

    /**
     * Obtiene intercambios únicos
     */
    @Aggregation(pipeline = {
        "{ '$group': { '_id': '$exchange' } }",
        "{ '$sort': { '_id': 1 } }"
    })
    List<String> findDistinctExchanges();

    /**
     * Estadísticas de mercado por sector
     */
    @Aggregation(pipeline = {
        "{ '$match': { 'isActive': true } }",
        "{ '$group': { " +
            "'_id': '$sector', " +
            "'count': { '$sum': 1 }, " +
            "'avgPrice': { '$avg': '$currentPrice' }, " +
            "'totalMarketCap': { '$sum': '$marketCap' } " +
        "} }",
        "{ '$sort': { 'totalMarketCap': -1 } }"
    })
    List<Object> getMarketStatsBySector();

    /**
     * Top performers del día
     */
    List<Stock> findTop10ByIsActiveTrueOrderByChangePercentDesc();

    /**
     * Stocks más activos por volumen
     */
    List<Stock> findTop20ByIsActiveTrueOrderByVolumeDesc();
}
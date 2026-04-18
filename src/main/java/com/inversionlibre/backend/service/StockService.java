package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.Stock;
import com.inversionlibre.backend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de stocks/activos financieros
 * Contiene la lógica de negocio para operaciones relacionadas con activos
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;

    // ===================================================================
    // OPERACIONES CRUD BÁSICAS
    // ===================================================================

    /**
     * Busca un stock por ID
     */
    @Cacheable(value = "stocks", key = "#id")
    public Optional<Stock> findById(String id) {
        log.debug("Buscando stock por ID: {}", id);
        return stockRepository.findById(id);
    }

    /**
     * Busca un stock por símbolo
     */
    @Cacheable(value = "stocks", key = "#symbol")
    public Optional<Stock> findBySymbol(String symbol) {
        log.debug("Buscando stock por símbolo: {}", symbol);
        return stockRepository.findBySymbol(symbol.toUpperCase());
    }

    /**
     * Obtiene todos los stocks activos con paginación
     */
    public Page<Stock> findActiveStocks(Pageable pageable) {
        log.debug("Obteniendo stocks activos con paginación");
        return stockRepository.findAll(pageable);
    }

    /**
     * Crea un nuevo stock
     */
    @Transactional
    @CacheEvict(value = "stocks", allEntries = true)
    public Stock createStock(Stock stock) {
        log.info("Creando nuevo stock: {}", stock.getSymbol());
        
        // Verificar que el símbolo no exista
        if (stockRepository.existsBySymbol(stock.getSymbol().toUpperCase())) {
            throw new IllegalArgumentException("Ya existe un stock con este símbolo: " + stock.getSymbol());
        }

        // Normalizar símbolo a mayúsculas
        stock.setSymbol(stock.getSymbol().toUpperCase());
        
        // Establecer valores por defecto
        if (stock.getIsActive() == null) {
            stock.setIsActive(true);
        }
        if (stock.getIsTradeable() == null) {
            stock.setIsTradeable(true);
        }
        if (stock.getMarketStatus() == null) {
            stock.setMarketStatus(Stock.MarketStatus.CLOSED);
        }

        // Calcular cambios iniciales si es necesario
        calculatePriceChanges(stock);

        Stock savedStock = stockRepository.save(stock);
        log.info("Stock creado exitosamente: {}", savedStock.getSymbol());
        
        return savedStock;
    }

    /**
     * Actualiza un stock existente
     */
    @Transactional
    @CacheEvict(value = "stocks", key = "#stock.symbol")
    public Stock updateStock(Stock stock) {
        log.info("Actualizando stock: {}", stock.getSymbol());
        
        Optional<Stock> existingStock = stockRepository.findBySymbol(stock.getSymbol());
        if (existingStock.isEmpty()) {
            throw new IllegalArgumentException("Stock no encontrado: " + stock.getSymbol());
        }

        Stock stockToUpdate = existingStock.get();
        
        // Actualizar información básica
        stockToUpdate.setCompanyName(stock.getCompanyName());
        stockToUpdate.setDescription(stock.getDescription());
        stockToUpdate.setSector(stock.getSector());
        stockToUpdate.setIndustry(stock.getIndustry());
        stockToUpdate.setCountry(stock.getCountry());
        stockToUpdate.setExchange(stock.getExchange());
        
        // Actualizar datos fundamentales y técnicos si están presentes
        if (stock.getFundamentals() != null) {
            stockToUpdate.setFundamentals(stock.getFundamentals());
        }
        if (stock.getTechnicals() != null) {
            stockToUpdate.setTechnicals(stock.getTechnicals());
        }
        if (stock.getDividendInfo() != null) {
            stockToUpdate.setDividendInfo(stock.getDividendInfo());
        }

        Stock updatedStock = stockRepository.save(stockToUpdate);
        log.info("Stock actualizado exitosamente: {}", updatedStock.getSymbol());
        
        return updatedStock;
    }

    /**
     * Asegura que un stock exista, creándolo si es necesario
     */
    @Transactional
    @CacheEvict(value = "stocks", key = "#stock.symbol")
    public Stock getOrCreateStock(Stock stock) {
        log.info("Asegurando existencia de stock: {}", stock.getSymbol());
        Optional<Stock> existing = stockRepository.findBySymbol(stock.getSymbol().toUpperCase());
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Si no existe, lo creamos
        return createStock(stock);
    }

    /**
     * Elimina un stock (desactiva)
     */
    @Transactional
    @CacheEvict(value = "stocks", key = "#symbol")
    public void deleteStock(String symbol) {
        log.info("Desactivando stock: {}", symbol);
        
        Optional<Stock> stock = stockRepository.findBySymbol(symbol.toUpperCase());
        if (stock.isEmpty()) {
            throw new IllegalArgumentException("Stock no encontrado: " + symbol);
        }

        Stock stockToDelete = stock.get();
        stockToDelete.setIsActive(false);
        stockToDelete.setIsTradeable(false);
        
        stockRepository.save(stockToDelete);
        log.info("Stock desactivado exitosamente: {}", symbol);
    }

    // ===================================================================
    // ACTUALIZACIÓN DE PRECIOS
    // ===================================================================

    /**
     * Actualiza el precio de un stock
     */
    @Transactional
    @CacheEvict(value = "stocks", key = "#symbol")
    public Stock updateStockPrice(String symbol, BigDecimal newPrice, Long volume, Stock.MarketStatus marketStatus) {
        log.debug("Actualizando precio del stock {}: {}", symbol, newPrice);
        
        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol.toUpperCase());
        if (stockOpt.isEmpty()) {
            throw new IllegalArgumentException("Stock no encontrado: " + symbol);
        }

        Stock stock = stockOpt.get();
        
        // Guardar precio anterior
        if (stock.getCurrentPrice() != null) {
            stock.setPreviousClose(stock.getCurrentPrice());
        }

        // Actualizar nuevo precio
        stock.updatePrice(newPrice);
        
        // Actualizar volumen si se proporciona
        if (volume != null) {
            stock.setVolume(volume);
        }

        // Actualizar estado del mercado
        if (marketStatus != null) {
            stock.setMarketStatus(marketStatus);
        }

        Stock updatedStock = stockRepository.save(stock);
        log.debug("Precio actualizado para {}: {} ({:+.2f}%)", 
                symbol, newPrice, stock.getChangePercent());

        return updatedStock;
    }

    /**
     * Actualiza precios de múltiples stocks
     */
    @Transactional
    @CacheEvict(value = "stocks", allEntries = true)
    public void updateMultipleStockPrices(List<StockPriceUpdate> priceUpdates) {
        log.info("Actualizando precios de {} stocks", priceUpdates.size());
        
        for (StockPriceUpdate update : priceUpdates) {
            try {
                updateStockPrice(update.getSymbol(), update.getPrice(), update.getVolume(), update.getMarketStatus());
            } catch (Exception e) {
                log.error("Error actualizando precio para {}: {}", update.getSymbol(), e.getMessage());
            }
        }
        
        log.info("Actualización masiva de precios completada");
    }

    /**
     * Encuentra stocks que necesitan actualización de precio
     */
    public List<Stock> findStocksNeedingPriceUpdate() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        return stockRepository.findStocksNeedingPriceUpdate(cutoffTime);
    }

    // ===================================================================
    // ANÁLISIS TÉCNICO Y FUNDAMENTAL
    // ===================================================================

    /**
     * Actualiza datos técnicos de un stock
     */
    @Transactional
    @CacheEvict(value = "stocks", key = "#symbol")
    public Stock updateTechnicalData(String symbol, Stock.TechnicalData technicalData) {
        log.debug("Actualizando datos técnicos para stock: {}", symbol);
        
        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol.toUpperCase());
        if (stockOpt.isEmpty()) {
            throw new IllegalArgumentException("Stock no encontrado: " + symbol);
        }

        Stock stock = stockOpt.get();
        stock.setTechnicals(technicalData);

        return stockRepository.save(stock);
    }

    /**
     * Actualiza datos fundamentales de un stock
     */
    @Transactional
    @CacheEvict(value = "stocks", key = "#symbol")
    public Stock updateFundamentalData(String symbol, Stock.FundamentalData fundamentalData) {
        log.debug("Actualizando datos fundamentales para stock: {}", symbol);
        
        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol.toUpperCase());
        if (stockOpt.isEmpty()) {
            throw new IllegalArgumentException("Stock no encontrado: " + symbol);
        }

        Stock stock = stockOpt.get();
        stock.setFundamentals(fundamentalData);

        return stockRepository.save(stock);
    }

    /**
     * Calcula el score de valor de un stock (value investing)
     */
    public BigDecimal calculateValueScore(String symbol) {
        log.debug("Calculando score de valor para stock: {}", symbol);
        
        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol.toUpperCase());
        if (stockOpt.isEmpty() || stockOpt.get().getFundamentals() == null) {
            return BigDecimal.ZERO;
        }

        Stock.FundamentalData fundamentals = stockOpt.get().getFundamentals();
        BigDecimal score = BigDecimal.ZERO;
        int criteria = 0;

        // P/E ratio bajo (< 15)
        if (fundamentals.getPeRatio() != null && fundamentals.getPeRatio().compareTo(BigDecimal.valueOf(15)) < 0) {
            score = score.add(BigDecimal.valueOf(20));
            criteria++;
        }

        // P/B ratio bajo (< 1.5)
        if (fundamentals.getPbRatio() != null && fundamentals.getPbRatio().compareTo(BigDecimal.valueOf(1.5)) < 0) {
            score = score.add(BigDecimal.valueOf(20));
            criteria++;
        }

        // ROE alto (> 15%)
        if (fundamentals.getRoe() != null && fundamentals.getRoe().compareTo(BigDecimal.valueOf(15)) > 0) {
            score = score.add(BigDecimal.valueOf(20));
            criteria++;
        }

        // Margen neto alto (> 10%)
        if (fundamentals.getNetMargin() != null && fundamentals.getNetMargin().compareTo(BigDecimal.valueOf(10)) > 0) {
            score = score.add(BigDecimal.valueOf(20));
            criteria++;
        }

        // Deuda/Patrimonio bajo (< 0.5)
        if (fundamentals.getDebtToEquity() != null && fundamentals.getDebtToEquity().compareTo(BigDecimal.valueOf(0.5)) < 0) {
            score = score.add(BigDecimal.valueOf(20));
            criteria++;
        }

        return criteria > 0 ? score : BigDecimal.ZERO;
    }

    /**
     * Calcula el score técnico de un stock
     */
    public BigDecimal calculateTechnicalScore(String symbol) {
        log.debug("Calculando score técnico para stock: {}", symbol);
        
        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol.toUpperCase());
        if (stockOpt.isEmpty() || stockOpt.get().getTechnicals() == null) {
            return BigDecimal.ZERO;
        }

        Stock stock = stockOpt.get();
        Stock.TechnicalData technicals = stock.getTechnicals();
        BigDecimal score = BigDecimal.ZERO;

        // Precio por encima de SMA 50
        if (technicals.getSma50() != null && stock.getCurrentPrice().compareTo(technicals.getSma50()) > 0) {
            score = score.add(BigDecimal.valueOf(25));
        }

        // SMA 50 por encima de SMA 200 (tendencia alcista)
        if (technicals.getSma50() != null && technicals.getSma200() != null && 
            technicals.getSma50().compareTo(technicals.getSma200()) > 0) {
            score = score.add(BigDecimal.valueOf(25));
        }

        // RSI en zona neutral (30-70)
        if (technicals.getRsi() != null && 
            technicals.getRsi().compareTo(BigDecimal.valueOf(30)) >= 0 && 
            technicals.getRsi().compareTo(BigDecimal.valueOf(70)) <= 0) {
            score = score.add(BigDecimal.valueOf(25));
        }

        // MACD positivo
        if (technicals.getMacd() != null && technicals.getMacd().compareTo(BigDecimal.ZERO) > 0) {
            score = score.add(BigDecimal.valueOf(25));
        }

        return score;
    }

    // ===================================================================
    // SCREENING Y BÚSQUEDAS
    // ===================================================================

    /**
     * Busca stocks por múltiples criterios
     */
    public List<Stock> searchStocks(String searchTerm) {
        log.debug("Buscando stocks con término: {}", searchTerm);
        return stockRepository.findBySymbolOrCompanyNameContaining(searchTerm);
    }

    /**
     * Obtiene stocks de valor (value investing)
     */
    public List<Stock> findValueStocks(BigDecimal maxPe, BigDecimal maxPb, BigDecimal minDividendYield) {
        log.debug("Buscando stocks de valor con P/E<{}, P/B<{}, Dividendo>{}", maxPe, maxPb, minDividendYield);
        return stockRepository.findValueStocks(maxPe, maxPb, minDividendYield);
    }

    /**
     * Obtiene stocks de crecimiento
     */
    public List<Stock> findGrowthStocks(BigDecimal minRoe, BigDecimal minMargin, BigDecimal maxPe) {
        log.debug("Buscando stocks de crecimiento con ROE>{}, Margen>{}, P/E<{}", minRoe, minMargin, maxPe);
        return stockRepository.findGrowthStocks(minRoe, minMargin, maxPe);
    }

    /**
     * Obtiene stocks con momentum positivo
     */
    public List<Stock> findMomentumStocks() {
        log.debug("Buscando stocks con momentum positivo");
        return stockRepository.findMomentumStocks();
    }

    /**
     * Obtiene stocks que pagan dividendos
     */
    public List<Stock> findDividendStocks(BigDecimal minYield) {
        log.debug("Buscando stocks con dividendo mínimo de {}%", minYield);
        return stockRepository.findByDividendYieldGreaterThan(minYield);
    }

    /**
     * Obtiene top performers del día
     */
    public List<Stock> getTopPerformers(int limit) {
        log.debug("Obteniendo top {} performers del día", limit);
        return stockRepository.findTop10ByIsActiveTrueOrderByChangePercentDesc()
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Obtiene worst performers del día
     */
    public List<Stock> getWorstPerformers(int limit) {
        log.debug("Obteniendo worst {} performers del día", limit);
        return stockRepository.findByIsActiveTrueOrderByChangePercentAsc()
                .stream()
                .limit(limit)
                .toList();
    }

    // ===================================================================
    // GESTIÓN DE DIVIDENDOS
    // ===================================================================

    /**
     * Actualiza información de dividendos
     */
    @Transactional
    @CacheEvict(value = "stocks", key = "#symbol")
    public Stock updateDividendInfo(String symbol, Stock.DividendInfo dividendInfo) {
        log.debug("Actualizando información de dividendos para: {}", symbol);
        
        Optional<Stock> stockOpt = stockRepository.findBySymbol(symbol.toUpperCase());
        if (stockOpt.isEmpty()) {
            throw new IllegalArgumentException("Stock no encontrado: " + symbol);
        }

        Stock stock = stockOpt.get();
        stock.setDividendInfo(dividendInfo);

        return stockRepository.save(stock);
    }

    /**
     * Obtiene stocks con ex-dividend date próxima
     */
    public List<Stock> findUpcomingDividends(int daysAhead) {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(daysAhead);
        // Esta consulta necesitaría ser implementada en el repositorio
        // Por ahora, retornamos stocks que pagan dividendos
        return stockRepository.findDividendPayingStocks();
    }

    // ===================================================================
    // ANÁLISIS DE MERCADO
    // ===================================================================

    /**
     * Obtiene sectores únicos
     */
    @Cacheable(value = "stocks", key = "'sectors'")
    public List<String> getUniqueSectors() {
        log.debug("Obteniendo sectores únicos");
        return stockRepository.findDistinctSectors();
    }

    /**
     * Obtiene países únicos
     */
    @Cacheable(value = "stocks", key = "'countries'")
    public List<String> getUniqueCountries() {
        log.debug("Obteniendo países únicos");
        return stockRepository.findDistinctCountries();
    }

    /**
     * Obtiene intercambios únicos
     */
    @Cacheable(value = "stocks", key = "'exchanges'")
    public List<String> getUniqueExchanges() {
        log.debug("Obteniendo intercambios únicos");
        return stockRepository.findDistinctExchanges();
    }

    /**
     * Obtiene estadísticas de mercado por sector
     */
    public List<Object> getMarketStatsBySector() {
        log.debug("Obteniendo estadísticas de mercado por sector");
        return stockRepository.getMarketStatsBySector();
    }

    /**
     * Calcula la volatilidad del mercado (promedio de volatilidades individuales)
     */
    public BigDecimal calculateMarketVolatility() {
        log.debug("Calculando volatilidad del mercado");
        
        List<Stock> activeStocks = stockRepository.findByIsActiveTrue();
        
        BigDecimal totalVolatility = activeStocks.stream()
            .filter(stock -> stock.getTechnicals() != null && stock.getTechnicals().getVolatility() != null)
            .map(stock -> stock.getTechnicals().getVolatility())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long stocksWithVolatility = activeStocks.stream()
            .filter(stock -> stock.getTechnicals() != null && stock.getTechnicals().getVolatility() != null)
            .count();

        if (stocksWithVolatility > 0) {
            return totalVolatility.divide(BigDecimal.valueOf(stocksWithVolatility), 4, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    // ===================================================================
    // UTILIDADES Y VALIDACIONES
    // ===================================================================

    /**
     * Calcula cambios de precio
     */
    private void calculatePriceChanges(Stock stock) {
        if (stock.getCurrentPrice() != null && stock.getPreviousClose() != null) {
            BigDecimal change = stock.getCurrentPrice().subtract(stock.getPreviousClose());
            stock.setChange(change);
            
            if (stock.getPreviousClose().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal changePercent = change
                    .divide(stock.getPreviousClose(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                stock.setChangePercent(changePercent);
            }
        }
    }

    /**
     * Verifica si un símbolo está disponible
     */
    public boolean isSymbolAvailable(String symbol) {
        return !stockRepository.existsBySymbol(symbol.toUpperCase());
    }

    /**
     * Verifica si un stock existe y está activo
     */
    public boolean existsAndIsActive(String symbol) {
        Optional<Stock> stock = stockRepository.findBySymbol(symbol.toUpperCase());
        return stock.isPresent() && stock.get().getIsActive();
    }

    /**
     * Cuenta stocks por tipo de activo
     */
    public long countByAssetType(Stock.AssetType assetType) {
        return stockRepository.countByAssetType(assetType);
    }

    /**
     * Cuenta stocks activos
     */
    public long countActiveStocks() {
        return stockRepository.countByIsActiveTrue();
    }

    // ===================================================================
    // CLASES AUXILIARES
    // ===================================================================

    /**
     * Clase para actualización de precios
     */
    public static class StockPriceUpdate {
        private String symbol;
        private BigDecimal price;
        private Long volume;
        private Stock.MarketStatus marketStatus;

        // Constructores
        public StockPriceUpdate() {}

        public StockPriceUpdate(String symbol, BigDecimal price) {
            this.symbol = symbol;
            this.price = price;
        }

        public StockPriceUpdate(String symbol, BigDecimal price, Long volume, Stock.MarketStatus marketStatus) {
            this.symbol = symbol;
            this.price = price;
            this.volume = volume;
            this.marketStatus = marketStatus;
        }

        // Getters y Setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Long getVolume() { return volume; }
        public void setVolume(Long volume) { this.volume = volume; }
        public Stock.MarketStatus getMarketStatus() { return marketStatus; }
        public void setMarketStatus(Stock.MarketStatus marketStatus) { this.marketStatus = marketStatus; }
    }
}
package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.Investment;
import com.inversionlibre.backend.model.Portfolio;
import com.inversionlibre.backend.model.Stock;
import com.inversionlibre.backend.model.Transaction;
import com.inversionlibre.backend.repository.InvestmentRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de inversiones
 * Contiene la lógica de negocio para operaciones relacionadas con inversiones específicas
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final PortfolioService portfolioService;
    private final StockService stockService;

    // ===================================================================
    // OPERACIONES CRUD BÁSICAS
    // ===================================================================

    /**
     * Busca una inversión por ID
     */
    @Cacheable(value = "investments", key = "#id")
    public Optional<Investment> findById(String id) {
        log.debug("Buscando inversión por ID: {}", id);
        return investmentRepository.findById(id);
    }

    /**
     * Obtiene todas las inversiones de un portfolio
     */
    public List<Investment> findByPortfolioId(String portfolioId) {
        log.debug("Obteniendo inversiones del portfolio: {}", portfolioId);
        return investmentRepository.findByPortfolioId(portfolioId);
    }

    /**
     * Obtiene inversiones activas de un portfolio
     */
    public List<Investment> findActiveInvestmentsByPortfolio(String portfolioId) {
        log.debug("Obteniendo inversiones activas del portfolio: {}", portfolioId);
        return investmentRepository.findByPortfolioIdAndStatus(portfolioId, Investment.InvestmentStatus.ACTIVE);
    }

    /**
     * Obtiene inversiones de un portfolio con paginación
     */
    public Page<Investment> findByPortfolioId(String portfolioId, Pageable pageable) {
        log.debug("Obteniendo inversiones del portfolio {} (paginado)", portfolioId);
        return investmentRepository.findByPortfolioId(portfolioId, pageable);
    }

    /**
     * Busca una inversión específica por portfolio y stock
     */
    public Optional<Investment> findByPortfolioAndStock(String portfolioId, String stockId) {
        log.debug("Buscando inversión en portfolio {} para stock {}", portfolioId, stockId);
        return investmentRepository.findByPortfolioIdAndStockId(portfolioId, stockId);
    }

    /**
     * Crea una nueva inversión
     */
    @Transactional
    @CacheEvict(value = "investments", allEntries = true)
    public Investment createInvestment(Investment investment) {
        log.info("Creando nueva inversión: {} en portfolio {}", 
                investment.getStockSymbol(), investment.getPortfolioId());
        
        // Verificar que el portfolio existe y está activo
        if (!portfolioService.existsAndIsActive(investment.getPortfolioId())) {
            throw new IllegalArgumentException("Portfolio no encontrado o inactivo: " + investment.getPortfolioId());
        }

        // Verificar si el stock existe, si no, crearlo
        Stock stock;
        Optional<Stock> stockOpt = stockService.findBySymbol(investment.getStockSymbol());
        if (stockOpt.isEmpty()) {
            log.info("Stock {} no existe, creándolo automáticamente", investment.getStockSymbol());
            // Crear el stock con la información básica proporcionada
            stock = Stock.builder()
                    .symbol(investment.getStockSymbol())
                    .companyName(investment.getStockName())
                    .currentPrice(investment.getCurrentPrice() != null ? investment.getCurrentPrice() : investment.getAveragePrice())
                    .currency("USD") // Default currency
                    .exchange("UNKNOWN") // Will be updated later
                    .isActive(true)
                    .build();
            stock = stockService.createStock(stock);
        } else {
            stock = stockOpt.get();
            if (!stock.getIsActive()) {
                throw new IllegalArgumentException("Stock no está activo: " + investment.getStockSymbol());
            }
        }

        // Verificar que no existe ya una inversión para este stock en el portfolio
        if (investmentRepository.existsByPortfolioIdAndStockId(investment.getPortfolioId(), investment.getStockId())) {
            throw new IllegalArgumentException("Ya existe una inversión para este stock en el portfolio");
        }

        // Actualizar datos de la inversión con información del stock
        investment.setStockName(stock.getCompanyName());
        if (investment.getCurrentPrice() == null) {
            investment.setCurrentPrice(stock.getCurrentPrice());
        }

        // Establecer valores por defecto
        if (investment.getStatus() == null) {
            investment.setStatus(Investment.InvestmentStatus.ACTIVE);
        }
        if (investment.getStrategy() == null) {
            investment.setStrategy(Investment.InvestmentStrategy.BUY_AND_HOLD);
        }

        // Calcular valores iniciales
        investment.updateCalculatedValues();
        investment.setFirstPurchaseDate(LocalDateTime.now());

        Investment savedInvestment = investmentRepository.save(investment);
        
        // Añadir la inversión al portfolio
        addInvestmentToPortfolio(savedInvestment.getPortfolioId(), savedInvestment.getId());
        
        log.info("Inversión creada exitosamente con ID: {}", savedInvestment.getId());
        return savedInvestment;
    }

    /**
     * Actualiza una inversión existente
     */
    @Transactional
    @CacheEvict(value = "investments", key = "#investment.id")
    public Investment updateInvestment(Investment investment) {
        log.info("Actualizando inversión: {}", investment.getId());
        
        Optional<Investment> existingInvestment = investmentRepository.findById(investment.getId());
        if (existingInvestment.isEmpty()) {
            throw new IllegalArgumentException("Inversión no encontrada: " + investment.getId());
        }

        Investment investmentToUpdate = existingInvestment.get();
        
        // Actualizar campos permitidos (no cantidad ni precio promedio directamente)
        investmentToUpdate.setStrategy(investment.getStrategy());
        investmentToUpdate.setStatus(investment.getStatus());
        investmentToUpdate.setAlerts(investment.getAlerts());
        investmentToUpdate.setGoals(investment.getGoals());

        // Recalcular valores
        investmentToUpdate.updateCalculatedValues();

        Investment updatedInvestment = investmentRepository.save(investmentToUpdate);
        log.info("Inversión actualizada exitosamente: {}", updatedInvestment.getId());
        
        return updatedInvestment;
    }

    /**
     * Cierra una inversión (cambiar estado a CLOSED)
     */
    @Transactional
    @CacheEvict(value = "investments", key = "#id")
    public Investment closeInvestment(String id) {
        log.info("Cerrando inversión: {}", id);
        
        Optional<Investment> investmentOpt = investmentRepository.findById(id);
        if (investmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Inversión no encontrada: " + id);
        }

        Investment investment = investmentOpt.get();
        
        // Verificar que la cantidad sea cero antes de cerrar
        if (investment.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("No se puede cerrar una inversión con cantidad pendiente");
        }

        investment.setStatus(Investment.InvestmentStatus.CLOSED);
        
        Investment closedInvestment = investmentRepository.save(investment);
        
        // Remover del portfolio
        removeInvestmentFromPortfolio(investment.getPortfolioId(), id);
        
        log.info("Inversión cerrada exitosamente: {}", id);
        return closedInvestment;
    }

    /**
     * Elimina una inversión (hard delete)
     * Solo se permite si no tiene transacciones asociadas
     */
    @Transactional
    @CacheEvict(value = "investments", key = "#id")
    public void deleteInvestment(String id) {
        log.info("Eliminando inversión: {}", id);
        
        Optional<Investment> investmentOpt = investmentRepository.findById(id);
        if (investmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Inversión no encontrada: " + id);
        }

        Investment investment = investmentOpt.get();
        
        // Verificar que no tiene transacciones asociadas
        if (investment.getTotalTransactions() != null && investment.getTotalTransactions() > 0) {
            throw new IllegalStateException("No se puede eliminar una inversión con transacciones asociadas. Use closeInvestment en su lugar.");
        }

        // Remover del portfolio
        removeInvestmentFromPortfolio(investment.getPortfolioId(), id);
        
        // Eliminar la inversión
        investmentRepository.deleteById(id);
        
        log.info("Inversión eliminada exitosamente: {}", id);
    }

    // ===================================================================
    // PROCESAMIENTO DE TRANSACCIONES
    // ===================================================================

    /**
     * Procesa una compra y actualiza la inversión
     */
    @Transactional
    @CacheEvict(value = "investments", key = "#investmentId")
    public Investment processPurchase(String investmentId, BigDecimal quantity, BigDecimal price, String transactionId) {
        log.info("Procesando compra en inversión {}: {} x {}", investmentId, quantity, price);
        
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        if (investmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Inversión no encontrada: " + investmentId);
        }

        Investment investment = investmentOpt.get();
        
        // Calcular nuevo precio promedio ponderado
        BigDecimal currentTotalValue = investment.getQuantity().multiply(investment.getAveragePrice());
        BigDecimal newTotalValue = quantity.multiply(price);
        BigDecimal totalQuantity = investment.getQuantity().add(quantity);
        
        BigDecimal newAveragePrice = currentTotalValue.add(newTotalValue)
            .divide(totalQuantity, 4, RoundingMode.HALF_UP);

        // Actualizar inversión
        investment.setQuantity(totalQuantity);
        investment.setAveragePrice(newAveragePrice);
        investment.addTransactionId(transactionId);
        investment.setLastTransactionDate(LocalDateTime.now());
        
        // Si es la primera compra, establecer fecha
        if (investment.getFirstPurchaseDate() == null) {
            investment.setFirstPurchaseDate(LocalDateTime.now());
        }

        // Recalcular valores
        investment.updateCalculatedValues();

        Investment updatedInvestment = investmentRepository.save(investment);
        log.info("Compra procesada. Nueva cantidad: {}, Nuevo precio promedio: {}", 
                totalQuantity, newAveragePrice);

        return updatedInvestment;
    }

    /**
     * Procesa una venta y actualiza la inversión
     */
    @Transactional
    @CacheEvict(value = "investments", key = "#investmentId")
    public Investment processSale(String investmentId, BigDecimal quantity, BigDecimal price, String transactionId) {
        log.info("Procesando venta en inversión {}: {} x {}", investmentId, quantity, price);
        
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        if (investmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Inversión no encontrada: " + investmentId);
        }

        Investment investment = investmentOpt.get();
        
        // Verificar que hay suficiente cantidad para vender
        if (investment.getQuantity().compareTo(quantity) < 0) {
            throw new IllegalStateException("Cantidad insuficiente para venta");
        }

        // Actualizar cantidad (el precio promedio se mantiene)
        BigDecimal newQuantity = investment.getQuantity().subtract(quantity);
        investment.setQuantity(newQuantity);
        investment.addTransactionId(transactionId);
        investment.setLastTransactionDate(LocalDateTime.now());

        // Si se vendió todo, cambiar a salida parcial o cerrar
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            investment.setStatus(Investment.InvestmentStatus.CLOSED);
        } else {
            investment.setStatus(Investment.InvestmentStatus.PARTIAL_EXIT);
        }

        // Recalcular valores
        investment.updateCalculatedValues();

        Investment updatedInvestment = investmentRepository.save(investment);
        log.info("Venta procesada. Nueva cantidad: {}", newQuantity);

        return updatedInvestment;
    }

    /**
     * Procesa un dividendo recibido
     */
    @Transactional
    @CacheEvict(value = "investments", key = "#investmentId")
    public Investment processDividend(String investmentId, BigDecimal dividendAmount, String transactionId) {
        log.info("Procesando dividendo en inversión {}: {}", investmentId, dividendAmount);
        
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        if (investmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Inversión no encontrada: " + investmentId);
        }

        Investment investment = investmentOpt.get();
        investment.addTransactionId(transactionId);
        
        // Actualizar métricas de rendimiento si existen
        if (investment.getPerformance() != null) {
            BigDecimal currentDividends = investment.getPerformance().getDividendsReceived() != null ? 
                investment.getPerformance().getDividendsReceived() : BigDecimal.ZERO;
            investment.getPerformance().setDividendsReceived(currentDividends.add(dividendAmount));
        }

        Investment updatedInvestment = investmentRepository.save(investment);
        log.info("Dividendo procesado: {}", dividendAmount);

        return updatedInvestment;
    }

    // ===================================================================
    // ACTUALIZACIÓN DE PRECIOS
    // ===================================================================

    /**
     * Actualiza el precio actual de una inversión
     */
    @Transactional
    @CacheEvict(value = "investments", key = "#investmentId")
    public Investment updateCurrentPrice(String investmentId, BigDecimal newPrice) {
        log.debug("Actualizando precio actual de inversión {}: {}", investmentId, newPrice);
        
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        if (investmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Inversión no encontrada: " + investmentId);
        }

        Investment investment = investmentOpt.get();
        investment.updateCurrentPrice(newPrice);

        return investmentRepository.save(investment);
    }

    /**
     * Actualiza precios de todas las inversiones de un stock
     */
    @Transactional
    @CacheEvict(value = "investments", allEntries = true)
    public void updatePricesForStock(String stockSymbol, BigDecimal newPrice) {
        log.debug("Actualizando precios para todas las inversiones de {}: {}", stockSymbol, newPrice);
        
        List<Investment> investments = investmentRepository.findByStockSymbol(stockSymbol);
        
        for (Investment investment : investments) {
            investment.updateCurrentPrice(newPrice);
            investmentRepository.save(investment);
        }
        
        log.debug("Actualizados {} inversiones para stock {}", investments.size(), stockSymbol);
    }

    // ===================================================================
    // SISTEMA DE ALERTAS
    // ===================================================================

    /**
     * Configura alertas para una inversión
     */
    @Transactional
    @CacheEvict(value = "investments", key = "#investmentId")
    public Investment configureAlerts(String investmentId, Investment.InvestmentAlerts alerts) {
        log.info("Configurando alertas para inversión: {}", investmentId);
        
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        if (investmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Inversión no encontrada: " + investmentId);
        }

        Investment investment = investmentOpt.get();
        investment.setAlerts(alerts);

        Investment updatedInvestment = investmentRepository.save(investment);
        log.info("Alertas configuradas para inversión: {}", investmentId);

        return updatedInvestment;
    }

    /**
     * Verifica alertas activadas y retorna las que se han disparado
     */
    public List<Investment> checkTriggeredAlerts() {
        log.debug("Verificando alertas disparadas");
        
        List<Investment> stopLossTriggered = investmentRepository.findInvestmentsTriggeredStopLoss();
        List<Investment> takeProfitTriggered = investmentRepository.findInvestmentsTriggeredTakeProfit();
        
        List<Investment> allTriggered = new java.util.ArrayList<>(stopLossTriggered);
        allTriggered.addAll(takeProfitTriggered);
        
        log.info("Encontradas {} alertas disparadas", allTriggered.size());
        return allTriggered;
    }

    // ===================================================================
    // GESTIÓN DE OBJETIVOS
    // ===================================================================

    /**
     * Actualiza los objetivos de una inversión
     */
    @Transactional
    @CacheEvict(value = "investments", key = "#investmentId")
    public Investment updateGoals(String investmentId, Investment.InvestmentGoals goals) {
        log.info("Actualizando objetivos para inversión: {}", investmentId);
        
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        if (investmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Inversión no encontrada: " + investmentId);
        }

        Investment investment = investmentOpt.get();
        investment.setGoals(goals);

        Investment updatedInvestment = investmentRepository.save(investment);
        log.info("Objetivos actualizados para inversión: {}", investmentId);

        return updatedInvestment;
    }

    /**
     * Verifica el progreso hacia los objetivos
     */
    public BigDecimal calculateGoalProgress(String investmentId) {
        log.debug("Calculando progreso del objetivo para inversión: {}", investmentId);
        
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        if (investmentOpt.isEmpty() || investmentOpt.get().getGoals() == null) {
            return BigDecimal.ZERO;
        }

        Investment investment = investmentOpt.get();
        Investment.InvestmentGoals goals = investment.getGoals();

        if (goals.getTargetValue() != null && goals.getTargetValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentValue = investment.getCurrentValue() != null ? investment.getCurrentValue() : BigDecimal.ZERO;
            return currentValue.divide(goals.getTargetValue(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .min(BigDecimal.valueOf(100));
        }

        return BigDecimal.ZERO;
    }

    // ===================================================================
    // ANÁLISIS Y MÉTRICAS
    // ===================================================================

    /**
     * Calcula métricas de rendimiento para una inversión
     */
    @Transactional
    @CacheEvict(value = "investments", key = "#investmentId")
    public Investment calculatePerformanceMetrics(String investmentId) {
        log.debug("Calculando métricas de rendimiento para inversión: {}", investmentId);
        
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        if (investmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Inversión no encontrada: " + investmentId);
        }

        Investment investment = investmentOpt.get();
        
        // Calcular días de tenencia
        int holdingDays = (int) ChronoUnit.DAYS.between(
            investment.getFirstPurchaseDate() != null ? investment.getFirstPurchaseDate() : LocalDateTime.now(),
            LocalDateTime.now()
        );

        // Calcular rentabilidad anualizada
        BigDecimal annualizedReturn = BigDecimal.ZERO;
        if (holdingDays > 0 && investment.getGainLossPercentage() != null) {
            double dailyReturn = investment.getGainLossPercentage().doubleValue() / holdingDays;
            double annualizedReturnDouble = Math.pow(1 + dailyReturn / 100, 365) - 1;
            annualizedReturn = BigDecimal.valueOf(annualizedReturnDouble * 100);
        }

        // Crear o actualizar métricas de rendimiento
        Investment.PerformanceMetrics performance = Investment.PerformanceMetrics.builder()
            .holdingPeriodDays(holdingDays)
            .annualizedReturn(annualizedReturn)
            .totalReturn(investment.getGainLossPercentage())
            .lastCalculated(LocalDateTime.now())
            .build();

        investment.setPerformance(performance);

        Investment updatedInvestment = investmentRepository.save(investment);
        log.debug("Métricas calculadas para inversión: {} días, {}% anualizado", 
                holdingDays, annualizedReturn);

        return updatedInvestment;
    }

    /**
     * Obtiene top inversiones por rendimiento en un portfolio
     */
    public List<Investment> getTopPerformingInvestments(String portfolioId, int limit) {
        log.debug("Obteniendo top {} inversiones por rendimiento en portfolio {}", limit, portfolioId);
        
        return investmentRepository.findTop10ByPortfolioIdAndStatusOrderByGainLossPercentageDesc(
            portfolioId, Investment.InvestmentStatus.ACTIVE)
            .stream()
            .limit(limit)
            .toList();
    }

    /**
     * Obtiene inversiones con peor rendimiento en un portfolio
     */
    public List<Investment> getWorstPerformingInvestments(String portfolioId, int limit) {
        log.debug("Obteniendo worst {} inversiones en portfolio {}", limit, portfolioId);
        
        return investmentRepository.findByPortfolioIdAndStatusOrderByGainLossPercentageAsc(
            portfolioId, Investment.InvestmentStatus.ACTIVE)
            .stream()
            .limit(limit)
            .toList();
    }

    // ===================================================================
    // CONSULTAS Y BÚSQUEDAS
    // ===================================================================

    /**
     * Busca inversiones rentables
     */
    public List<Investment> findProfitableInvestments() {
        log.debug("Buscando inversiones rentables");
        return investmentRepository.findProfitableInvestments();
    }

    /**
     * Busca inversiones con pérdidas
     */
    public List<Investment> findLosingInvestments() {
        log.debug("Buscando inversiones con pérdidas");
        return investmentRepository.findLosingInvestments();
    }

    /**
     * Obtiene inversiones por estrategia
     */
    public List<Investment> findByStrategy(Investment.InvestmentStrategy strategy) {
        log.debug("Obteniendo inversiones con estrategia: {}", strategy);
        return investmentRepository.findByStrategy(strategy);
    }

    /**
     * Obtiene inversiones con alertas habilitadas
     */
    public List<Investment> findInvestmentsWithAlerts() {
        log.debug("Obteniendo inversiones con alertas habilitadas");
        return investmentRepository.findInvestmentsWithAlertsEnabled();
    }

    /**
     * Obtiene inversiones que necesitan rebalanceado
     */
    public List<Investment> findInvestmentsForRebalancing() {
        log.debug("Obteniendo inversiones que necesitan rebalanceado");
        return investmentRepository.findInvestmentsForRebalancing();
    }

    // ===================================================================
    // UTILIDADES Y VALIDACIONES
    // ===================================================================

    /**
     * Añade una inversión a un portfolio
     */
    private void addInvestmentToPortfolio(String portfolioId, String investmentId) {
        Optional<Portfolio> portfolioOpt = portfolioService.findById(portfolioId);
        if (portfolioOpt.isPresent()) {
            Portfolio portfolio = portfolioOpt.get();
            portfolio.addInvestmentId(investmentId);
            portfolioService.updatePortfolio(portfolio);
        }
    }

    /**
     * Remueve una inversión de un portfolio
     */
    private void removeInvestmentFromPortfolio(String portfolioId, String investmentId) {
        Optional<Portfolio> portfolioOpt = portfolioService.findById(portfolioId);
        if (portfolioOpt.isPresent()) {
            Portfolio portfolio = portfolioOpt.get();
            portfolio.removeInvestmentId(investmentId);
            portfolioService.updatePortfolio(portfolio);
        }
    }

    /**
     * Verifica si una inversión existe
     */
    public boolean existsById(String investmentId) {
        return investmentRepository.existsById(investmentId);
    }

    /**
     * Cuenta inversiones activas en un portfolio
     */
    public long countActiveInvestmentsByPortfolio(String portfolioId) {
        return investmentRepository.countByPortfolioIdAndStatus(portfolioId, Investment.InvestmentStatus.ACTIVE);
    }

    /**
     * Calcula el valor total de inversiones en un portfolio
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // public BigDecimal getTotalValueByPortfolio(String portfolioId) {
    //     Optional<BigDecimal> totalValue = investmentRepository.getTotalValueByPortfolio(portfolioId);
    //     return totalValue.orElse(BigDecimal.ZERO);
    // }

    /**
     * Calcula la ganancia/pérdida total de inversiones en un portfolio
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // public BigDecimal getTotalGainLossByPortfolio(String portfolioId) {
    //     Optional<BigDecimal> totalGainLoss = investmentRepository.getTotalGainLossByPortfolio(portfolioId);
    //     return totalGainLoss.orElse(BigDecimal.ZERO);
    // }
}
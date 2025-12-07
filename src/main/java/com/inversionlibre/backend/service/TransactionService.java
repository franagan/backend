package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.Transaction;
import com.inversionlibre.backend.model.Investment;
import com.inversionlibre.backend.model.Portfolio;
import com.inversionlibre.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de transacciones
 * Contiene la lógica de negocio para operaciones relacionadas con transacciones financieras
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final InvestmentService investmentService;
    private final PortfolioService portfolioService;
    private final StockService stockService;

    // ===================================================================
    // OPERACIONES CRUD BÁSICAS
    // ===================================================================

    /**
     * Busca una transacción por ID
     */
    @Cacheable(value = "transactions", key = "#id")
    public Optional<Transaction> findById(String id) {
        log.debug("Buscando transacción por ID: {}", id);
        return transactionRepository.findById(id);
    }

    /**
     * Obtiene todas las transacciones de un usuario
     */
    public List<Transaction> findByUserId(String userId) {
        log.debug("Obteniendo transacciones del usuario: {}", userId);
        return transactionRepository.findByUserId(userId);
    }

    /**
     * Obtiene transacciones de un usuario con paginación
     */
    public Page<Transaction> findByUserId(String userId, Pageable pageable) {
        log.debug("Obteniendo transacciones del usuario {} (paginado)", userId);
        return transactionRepository.findByUserId(userId, pageable);
    }

    /**
     * Obtiene todas las transacciones de un portfolio
     */
    public List<Transaction> findByPortfolioId(String portfolioId) {
        log.debug("Obteniendo transacciones del portfolio: {}", portfolioId);
        return transactionRepository.findByPortfolioId(portfolioId);
    }

    /**
     * Obtiene transacciones de una inversión específica
     */
    public List<Transaction> findByInvestmentId(String investmentId) {
        log.debug("Obteniendo transacciones de la inversión: {}", investmentId);
        return transactionRepository.findByInvestmentId(investmentId);
    }

    // ===================================================================
    // PROCESAMIENTO DE TRANSACCIONES DE COMPRA/VENTA
    // ===================================================================

    /**
     * Procesa una transacción de compra
     */
    @Transactional
    @CacheEvict(value = {"transactions", "investments", "portfolios"}, allEntries = true)
    public Transaction processBuyTransaction(Transaction transaction) {
        log.info("Procesando transacción de compra: {} {} a {}", 
                transaction.getQuantity(), transaction.getStockSymbol(), transaction.getUnitPrice());
        
        // Validaciones previas
        validateTransaction(transaction);
        validateBuyTransaction(transaction);

        // Establecer tipo y estado
        transaction.setType(Transaction.TransactionType.BUY);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        transaction.setExecutedAt(LocalDateTime.now());

        // Calcular montos
        transaction.calculateAmounts();

        // Guardar transacción
        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            // Buscar o crear inversión
            Optional<Investment> existingInvestment = investmentService.findByPortfolioAndStock(
                transaction.getPortfolioId(), transaction.getStockId());

            Investment investment;
            if (existingInvestment.isPresent()) {
                // Actualizar inversión existente
                investment = investmentService.processPurchase(
                    existingInvestment.get().getId(),
                    transaction.getQuantity(),
                    transaction.getUnitPrice(),
                    savedTransaction.getId()
                );
            } else {
                // Crear nueva inversión
                investment = createNewInvestmentFromTransaction(savedTransaction);
            }

            // Marcar transacción como ejecutada
            savedTransaction.setStatus(Transaction.TransactionStatus.EXECUTED);
            savedTransaction = transactionRepository.save(savedTransaction);

            // Recalcular valores del portfolio
            portfolioService.recalculatePortfolioValues(transaction.getPortfolioId());

            log.info("Transacción de compra procesada exitosamente: {}", savedTransaction.getId());
            return savedTransaction;

        } catch (Exception e) {
            // Marcar transacción como fallida
            savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
            transactionRepository.save(savedTransaction);
            
            log.error("Error procesando transacción de compra: {}", e.getMessage());
            throw new RuntimeException("Error procesando compra: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa una transacción de venta
     */
    @Transactional
    @CacheEvict(value = {"transactions", "investments", "portfolios"}, allEntries = true)
    public Transaction processSellTransaction(Transaction transaction) {
        log.info("Procesando transacción de venta: {} {} a {}", 
                transaction.getQuantity(), transaction.getStockSymbol(), transaction.getUnitPrice());
        
        // Validaciones previas
        validateTransaction(transaction);
        validateSellTransaction(transaction);

        // Establecer tipo y estado
        transaction.setType(Transaction.TransactionType.SELL);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        transaction.setExecutedAt(LocalDateTime.now());

        // Calcular montos
        transaction.calculateAmounts();

        // Guardar transacción
        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            // Buscar inversión existente
            Optional<Investment> investmentOpt = investmentService.findByPortfolioAndStock(
                transaction.getPortfolioId(), transaction.getStockId());

            if (investmentOpt.isEmpty()) {
                throw new IllegalStateException("No se encontró inversión para vender");
            }

            // Procesar venta
            Investment investment = investmentService.processSale(
                investmentOpt.get().getId(),
                transaction.getQuantity(),
                transaction.getUnitPrice(),
                savedTransaction.getId()
            );

            // Marcar transacción como ejecutada
            savedTransaction.setStatus(Transaction.TransactionStatus.EXECUTED);
            savedTransaction = transactionRepository.save(savedTransaction);

            // Recalcular valores del portfolio
            portfolioService.recalculatePortfolioValues(transaction.getPortfolioId());

            log.info("Transacción de venta procesada exitosamente: {}", savedTransaction.getId());
            return savedTransaction;

        } catch (Exception e) {
            // Marcar transacción como fallida
            savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
            transactionRepository.save(savedTransaction);
            
            log.error("Error procesando transacción de venta: {}", e.getMessage());
            throw new RuntimeException("Error procesando venta: " + e.getMessage(), e);
        }
    }

    // ===================================================================
    // PROCESAMIENTO DE DIVIDENDOS
    // ===================================================================

    /**
     * Procesa una transacción de dividendo
     */
    @Transactional
    @CacheEvict(value = {"transactions", "investments"}, allEntries = true)
    public Transaction processDividendTransaction(Transaction transaction) {
        log.info("Procesando dividendo: {} para {}", 
                transaction.getNetAmount(), transaction.getStockSymbol());
        
        // Validaciones
        validateTransaction(transaction);
        
        if (transaction.getType() == null) {
            transaction.setType(Transaction.TransactionType.DIVIDEND);
        }
        
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        transaction.setExecutedAt(LocalDateTime.now());

        // Para dividendos, el gross amount es igual al net amount inicialmente
        if (transaction.getGrossAmount() == null) {
            transaction.setGrossAmount(transaction.getNetAmount());
        }

        transaction.calculateAmounts();

        // Guardar transacción
        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            // Buscar inversión existente
            Optional<Investment> investmentOpt = investmentService.findByPortfolioAndStock(
                transaction.getPortfolioId(), transaction.getStockId());

            if (investmentOpt.isPresent()) {
                investmentService.processDividend(
                    investmentOpt.get().getId(),
                    transaction.getNetAmount(),
                    savedTransaction.getId()
                );
            }

            // Marcar como ejecutada
            savedTransaction.setStatus(Transaction.TransactionStatus.EXECUTED);
            savedTransaction = transactionRepository.save(savedTransaction);

            log.info("Dividendo procesado exitosamente: {}", savedTransaction.getId());
            return savedTransaction;

        } catch (Exception e) {
            savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
            transactionRepository.save(savedTransaction);
            
            log.error("Error procesando dividendo: {}", e.getMessage());
            throw new RuntimeException("Error procesando dividendo: " + e.getMessage(), e);
        }
    }

    // ===================================================================
    // TRANSACCIONES MANUALES Y AJUSTES
    // ===================================================================

    /**
     * Crea una transacción manual/ajuste
     */
    @Transactional
    @CacheEvict(value = "transactions", allEntries = true)
    public Transaction createManualTransaction(Transaction transaction) {
        log.info("Creando transacción manual: {} {}", 
                transaction.getType(), transaction.getStockSymbol());
        
        validateTransaction(transaction);
        
        // Establecer como manual
        transaction.setSource("Manual");
        transaction.setExecutedAt(LocalDateTime.now());
        
        if (transaction.getStatus() == null) {
            transaction.setStatus(Transaction.TransactionStatus.EXECUTED);
        }

        // Calcular montos
        transaction.calculateAmounts();

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transacción manual creada: {}", savedTransaction.getId());
        
        return savedTransaction;
    }

    /**
     * Actualiza una transacción existente
     */
    @Transactional
    @CacheEvict(value = "transactions", key = "#transaction.id")
    public Transaction updateTransaction(Transaction transaction) {
        log.info("Actualizando transacción: {}", transaction.getId());
        
        Optional<Transaction> existingTransaction = transactionRepository.findById(transaction.getId());
        if (existingTransaction.isEmpty()) {
            throw new IllegalArgumentException("Transacción no encontrada: " + transaction.getId());
        }

        Transaction transactionToUpdate = existingTransaction.get();
        
        // Solo permitir actualizar ciertas propiedades después de creada
        transactionToUpdate.setNotes(transaction.getNotes());
        transactionToUpdate.setBrokerReference(transaction.getBrokerReference());
        
        // Recalcular montos si es necesario
        transactionToUpdate.calculateAmounts();

        Transaction updatedTransaction = transactionRepository.save(transactionToUpdate);
        log.info("Transacción actualizada: {}", updatedTransaction.getId());
        
        return updatedTransaction;
    }

    /**
     * Cancela una transacción pendiente
     */
    @Transactional
    @CacheEvict(value = "transactions", key = "#id")
    public Transaction cancelTransaction(String id) {
        log.info("Cancelando transacción: {}", id);
        
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new IllegalArgumentException("Transacción no encontrada: " + id);
        }

        Transaction transaction = transactionOpt.get();
        
        // Solo se pueden cancelar transacciones pendientes
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden cancelar transacciones pendientes");
        }

        transaction.setStatus(Transaction.TransactionStatus.CANCELLED);
        
        Transaction cancelledTransaction = transactionRepository.save(transaction);
        log.info("Transacción cancelada: {}", id);
        
        return cancelledTransaction;
    }

    // ===================================================================
    // CONSULTAS Y BÚSQUEDAS
    // ===================================================================

    /**
     * Obtiene transacciones por tipo
     */
    public List<Transaction> findByType(Transaction.TransactionType type) {
        log.debug("Obteniendo transacciones de tipo: {}", type);
        return transactionRepository.findByType(type);
    }

    /**
     * Obtiene transacciones por estado
     */
    public List<Transaction> findByStatus(Transaction.TransactionStatus status) {
        log.debug("Obteniendo transacciones con estado: {}", status);
        return transactionRepository.findByStatus(status);
    }

    /**
     * Obtiene transacciones en un rango de fechas
     */
    public List<Transaction> findByDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Obteniendo transacciones del usuario {} entre {} y {}", userId, startDate, endDate);
        return transactionRepository.findByUserIdAndExecutedAtBetween(userId, startDate, endDate);
    }

    /**
     * Obtiene transacciones recientes de un usuario
     */
    public List<Transaction> findRecentTransactions(String userId, int limit) {
        log.debug("Obteniendo {} transacciones recientes del usuario {}", limit, userId);
        return transactionRepository.findTop10ByUserIdOrderByExecutedAtDesc(userId)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Obtiene actividad de trading reciente
     */
    public List<Transaction> findRecentTradingActivity(LocalDateTime since) {
        log.debug("Obteniendo actividad de trading desde: {}", since);
        return transactionRepository.findRecentTradingActivity(since);
    }

    // ===================================================================
    // ESTADÍSTICAS Y REPORTING
    // ===================================================================

    /**
     * Calcula el total de compras de un portfolio
     */
    public BigDecimal getTotalPurchasesByPortfolio(String portfolioId) {
        log.debug("Calculando total de compras del portfolio: {}", portfolioId);
        
        // Método alternativo sin agregación para evitar problemas con Java 17+
        List<Transaction> allTransactions = transactionRepository.findByPortfolioIdAndType(
            portfolioId, 
            Transaction.TransactionType.BUY
        );
        
        return allTransactions.stream()
            .filter(t -> t.getStatus() == Transaction.TransactionStatus.EXECUTED || 
                        t.getStatus() == Transaction.TransactionStatus.SETTLED)
            .map(Transaction::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el total de ventas de un portfolio
     */
    public BigDecimal getTotalSalesByPortfolio(String portfolioId) {
        log.debug("Calculando total de ventas del portfolio: {}", portfolioId);
        
        // Método alternativo sin agregación para evitar problemas con Java 17+
        List<Transaction> allTransactions = transactionRepository.findByPortfolioIdAndType(
            portfolioId, 
            Transaction.TransactionType.SELL
        );
        
        return allTransactions.stream()
            .filter(t -> t.getStatus() == Transaction.TransactionStatus.EXECUTED || 
                        t.getStatus() == Transaction.TransactionStatus.SETTLED)
            .map(Transaction::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el total de dividendos recibidos por portfolio
     */
    public BigDecimal getTotalDividendsByPortfolio(String portfolioId) {
        log.debug("Calculando total de dividendos del portfolio: {}", portfolioId);
        
        // Método alternativo sin agregación para evitar problemas con Java 17+
        List<Transaction> allTransactions = transactionRepository.findByPortfolioId(portfolioId);
        
        return allTransactions.stream()
            .filter(t -> (t.getType() == Transaction.TransactionType.DIVIDEND || 
                         t.getType() == Transaction.TransactionType.DIVIDEND_REINVEST) &&
                        (t.getStatus() == Transaction.TransactionStatus.EXECUTED || 
                         t.getStatus() == Transaction.TransactionStatus.SETTLED))
            .map(Transaction::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Obtiene estadísticas de transacciones por tipo
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // public List<Object> getTransactionStatsByType() {
    //     log.debug("Obteniendo estadísticas de transacciones por tipo");
    //     return transactionRepository.getTransactionStatsByType();
    // }

    /**
     * Obtiene estadísticas generales de un usuario
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // public Optional<Object> getUserTransactionStats(String userId) {
    //     log.debug("Obteniendo estadísticas de transacciones del usuario: {}", userId);
    //     return transactionRepository.getUserTransactionStats(userId);
    // }

    /**
     * Calcula costos totales de un portfolio
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // public Optional<Object> getTotalCostsByPortfolio(String portfolioId) {
    //     log.debug("Calculando costos totales del portfolio: {}", portfolioId);
    //     return transactionRepository.getTotalCostsByPortfolio(portfolioId);
    // }

    // ===================================================================
    // VALIDACIONES PRIVADAS
    // ===================================================================

    /**
     * Validaciones generales para transacciones
     */
    private void validateTransaction(Transaction transaction) {
        if (transaction.getUserId() == null || transaction.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("ID de usuario es obligatorio");
        }

        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().trim().isEmpty()) {
            throw new IllegalArgumentException("ID de portfolio es obligatorio");
        }

        if (transaction.getStockId() == null || transaction.getStockId().trim().isEmpty()) {
            throw new IllegalArgumentException("ID de stock es obligatorio");
        }

        if (transaction.getStockSymbol() == null || transaction.getStockSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Símbolo de stock es obligatorio");
        }

        // Verificar que el portfolio existe y pertenece al usuario
        Optional<Portfolio> portfolio = portfolioService.findById(transaction.getPortfolioId());
        if (portfolio.isEmpty()) {
            throw new IllegalArgumentException("Portfolio no encontrado");
        }

        if (!portfolio.get().getUserId().equals(transaction.getUserId())) {
            throw new IllegalArgumentException("El portfolio no pertenece al usuario");
        }

        // Verificar que el stock existe
        if (!stockService.existsAndIsActive(transaction.getStockSymbol())) {
            throw new IllegalArgumentException("Stock no encontrado o inactivo: " + transaction.getStockSymbol());
        }
    }

    /**
     * Validaciones específicas para transacciones de compra
     */
    private void validateBuyTransaction(Transaction transaction) {
        if (transaction.getQuantity() == null || transaction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cantidad debe ser mayor a cero para compras");
        }

        if (transaction.getUnitPrice() == null || transaction.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Precio unitario debe ser mayor a cero");
        }
    }

    /**
     * Validaciones específicas para transacciones de venta
     */
    private void validateSellTransaction(Transaction transaction) {
        if (transaction.getQuantity() == null || transaction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cantidad debe ser mayor a cero para ventas");
        }

        if (transaction.getUnitPrice() == null || transaction.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Precio unitario debe ser mayor a cero");
        }

        // Verificar que hay suficiente cantidad para vender
        Optional<Investment> investment = investmentService.findByPortfolioAndStock(
            transaction.getPortfolioId(), transaction.getStockId());

        if (investment.isEmpty()) {
            throw new IllegalStateException("No hay inversión activa para este stock");
        }

        if (investment.get().getQuantity().compareTo(transaction.getQuantity()) < 0) {
            throw new IllegalStateException("Cantidad insuficiente para venta");
        }
    }

    // ===================================================================
    // MÉTODOS AUXILIARES
    // ===================================================================

    /**
     * Crea una nueva inversión a partir de una transacción de compra
     */
    private Investment createNewInvestmentFromTransaction(Transaction transaction) {
        Investment investment = Investment.builder()
            .portfolioId(transaction.getPortfolioId())
            .stockId(transaction.getStockId())
            .stockSymbol(transaction.getStockSymbol())
            .stockName(transaction.getStockName())
            .quantity(transaction.getQuantity())
            .averagePrice(transaction.getUnitPrice())
            .currentPrice(transaction.getUnitPrice())
            .strategy(Investment.InvestmentStrategy.BUY_AND_HOLD)
            .status(Investment.InvestmentStatus.ACTIVE)
            .build();

        investment.updateCalculatedValues();
        investment.addTransactionId(transaction.getId());

        return investmentService.createInvestment(investment);
    }

    /**
     * Verifica si una transacción existe
     */
    public boolean existsById(String transactionId) {
        return transactionRepository.existsById(transactionId);
    }

    /**
     * Cuenta transacciones de un usuario
     */
    public long countByUserId(String userId) {
        return transactionRepository.countByUserId(userId);
    }

    /**
     * Cuenta transacciones de un portfolio
     */
    public long countByPortfolioId(String portfolioId) {
        return transactionRepository.countByPortfolioId(portfolioId);
    }
}
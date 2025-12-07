package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.Portfolio;
import com.inversionlibre.backend.model.Investment;
import com.inversionlibre.backend.repository.PortfolioRepository;
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
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de portfolios/carteras de inversión
 * Contiene la lógica de negocio para operaciones relacionadas con carteras
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final InvestmentRepository investmentRepository;
    private final UserService userService;

    // ===================================================================
    // OPERACIONES CRUD BÁSICAS
    // ===================================================================

    /**
     * Busca un portfolio por ID
     */
    @Cacheable(value = "portfolios", key = "#id")
    public Optional<Portfolio> findById(String id) {
        log.debug("Buscando portfolio por ID: {}", id);
        return portfolioRepository.findById(id);
    }

    /**
     * Obtiene todos los portfolios de un usuario
     */
    public List<Portfolio> findByUserId(String userId) {
        log.debug("Obteniendo portfolios del usuario: {}", userId);
        return portfolioRepository.findByUserId(userId);
    }

    /**
     * Obtiene portfolios activos de un usuario
     */
    public List<Portfolio> findActivePortfoliosByUserId(String userId) {
        log.debug("Obteniendo portfolios activos del usuario: {}", userId);
        return portfolioRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Obtiene portfolios de un usuario con paginación
     */
    public Page<Portfolio> findByUserId(String userId, Pageable pageable) {
        log.debug("Obteniendo portfolios del usuario {} (paginado)", userId);
        return portfolioRepository.findByUserId(userId, pageable);
    }

    /**
     * Crea un nuevo portfolio
     */
    @Transactional
    @CacheEvict(value = "portfolios", allEntries = true)
    public Portfolio createPortfolio(Portfolio portfolio) {
        log.info("Creando nuevo portfolio: {} para usuario: {}", portfolio.getName(), portfolio.getUserId());
        
        // Verificar que el usuario existe
        if (!userService.existsById(portfolio.getUserId())) {
            throw new IllegalArgumentException("Usuario no encontrado: " + portfolio.getUserId());
        }

        // Validar límites de portfolios por usuario según el rol
        validatePortfolioLimits(portfolio.getUserId());

        // Establecer valores por defecto
        portfolio.setIsActive(true);
        portfolio.setIsPublic(false);
        portfolio.setTotalValue(BigDecimal.ZERO);
        portfolio.setTotalInvested(BigDecimal.ZERO);
        portfolio.setTotalGainLoss(BigDecimal.ZERO);
        portfolio.setTotalGainLossPercentage(BigDecimal.ZERO);

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        
        // Añadir el portfolio al usuario
        userService.addPortfolioToUser(portfolio.getUserId(), savedPortfolio.getId());
        
        log.info("Portfolio creado exitosamente con ID: {}", savedPortfolio.getId());
        return savedPortfolio;
    }

    /**
     * Actualiza un portfolio existente
     */
    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolio.id")
    public Portfolio updatePortfolio(Portfolio portfolio) {
        log.info("Actualizando portfolio: {}", portfolio.getId());
        
        Optional<Portfolio> existingPortfolio = portfolioRepository.findById(portfolio.getId());
        if (existingPortfolio.isEmpty()) {
            throw new IllegalArgumentException("Portfolio no encontrado: " + portfolio.getId());
        }

        Portfolio portfolioToUpdate = existingPortfolio.get();
        
        // Actualizar campos permitidos
        portfolioToUpdate.setName(portfolio.getName());
        portfolioToUpdate.setDescription(portfolio.getDescription());
        portfolioToUpdate.setType(portfolio.getType());
        portfolioToUpdate.setRiskLevel(portfolio.getRiskLevel());
        portfolioToUpdate.setCurrency(portfolio.getCurrency());
        portfolioToUpdate.setIsPublic(portfolio.getIsPublic());
        portfolioToUpdate.setGoal(portfolio.getGoal());

        Portfolio updatedPortfolio = portfolioRepository.save(portfolioToUpdate);
        log.info("Portfolio actualizado exitosamente: {}", updatedPortfolio.getId());
        
        return updatedPortfolio;
    }

    /**
     * Elimina un portfolio (soft delete)
     */
    @Transactional
    @CacheEvict(value = "portfolios", key = "#id")
    public void deletePortfolio(String id) {
        log.info("Desactivando portfolio: {}", id);
        
        Optional<Portfolio> portfolio = portfolioRepository.findById(id);
        if (portfolio.isEmpty()) {
            throw new IllegalArgumentException("Portfolio no encontrado: " + id);
        }

        Portfolio portfolioToDelete = portfolio.get();
        
        // Verificar que no tiene inversiones activas
        long activeInvestments = investmentRepository.countByPortfolioIdAndStatus(id, Investment.InvestmentStatus.ACTIVE);
        if (activeInvestments > 0) {
            throw new IllegalStateException("No se puede eliminar un portfolio con inversiones activas");
        }

        portfolioToDelete.setIsActive(false);
        portfolioRepository.save(portfolioToDelete);
        
        // Remover del usuario
        userService.removePortfolioFromUser(portfolioToDelete.getUserId(), id);
        
        log.info("Portfolio desactivado exitosamente: {}", id);
    }

    // ===================================================================
    // CÁLCULOS DE VALOR Y RENDIMIENTO
    // ===================================================================

    /**
     * Recalcula y actualiza todos los valores de un portfolio
     */
    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolioId")
    public Portfolio recalculatePortfolioValues(String portfolioId) {
        log.info("Recalculando valores del portfolio: {}", portfolioId);
        
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(portfolioId);
        if (portfolioOpt.isEmpty()) {
            throw new IllegalArgumentException("Portfolio no encontrado: " + portfolioId);
        }

        Portfolio portfolio = portfolioOpt.get();
        
        // Obtener todas las inversiones activas del portfolio
        List<Investment> activeInvestments = investmentRepository.findByPortfolioIdAndStatus(
            portfolioId, Investment.InvestmentStatus.ACTIVE);

        // Calcular valores totales
        BigDecimal totalValue = activeInvestments.stream()
            .map(inv -> inv.getCurrentValue() != null ? inv.getCurrentValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInvested = activeInvestments.stream()
            .map(inv -> inv.getTotalInvested() != null ? inv.getTotalInvested() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Actualizar valores del portfolio
        portfolio.setTotalValue(totalValue);
        portfolio.setTotalInvested(totalInvested);
        portfolio.updateCalculatedValues();

        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
        log.info("Valores recalculados para portfolio: {} - Valor: {}, Invertido: {}, Ganancia: {}", 
                portfolioId, totalValue, totalInvested, portfolio.getTotalGainLoss());

        return updatedPortfolio;
    }

    /**
     * Calcula el porcentaje de diversificación de un portfolio
     */
    public BigDecimal calculateDiversificationScore(String portfolioId) {
        log.debug("Calculando score de diversificación para portfolio: {}", portfolioId);
        
        List<Investment> investments = investmentRepository.findByPortfolioIdAndStatus(
            portfolioId, Investment.InvestmentStatus.ACTIVE);

        if (investments.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Score basado en número de inversiones (máximo 100 para 20+ inversiones)
        int investmentCount = investments.size();
        BigDecimal diversificationScore = BigDecimal.valueOf(Math.min(investmentCount * 5, 100));

        // Penalizar concentración excesiva en una sola inversión
        BigDecimal totalValue = investments.stream()
            .map(inv -> inv.getCurrentValue() != null ? inv.getCurrentValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxPercentage = investments.stream()
                .map(inv -> {
                    BigDecimal currentValue = inv.getCurrentValue() != null ? inv.getCurrentValue() : BigDecimal.ZERO;
                    return currentValue.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                })
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

            // Si una inversión representa más del 50%, penalizar
            if (maxPercentage.compareTo(BigDecimal.valueOf(50)) > 0) {
                diversificationScore = diversificationScore.multiply(BigDecimal.valueOf(0.7));
            }
        }

        return diversificationScore.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula métricas de rendimiento del portfolio
     */
    public Portfolio.PortfolioAnalytics calculatePortfolioAnalytics(String portfolioId) {
        log.debug("Calculando analytics del portfolio: {}", portfolioId);
        
        List<Investment> investments = investmentRepository.findByPortfolioIdAndStatus(
            portfolioId, Investment.InvestmentStatus.ACTIVE);

        if (investments.isEmpty()) {
            return Portfolio.PortfolioAnalytics.builder()
                .lastAnalysisAt(LocalDateTime.now())
                .build();
        }

        // Calcular volatilidad promedio ponderada
        BigDecimal totalValue = investments.stream()
            .map(inv -> inv.getCurrentValue() != null ? inv.getCurrentValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weightedVolatility = BigDecimal.ZERO;
        BigDecimal weightedBeta = BigDecimal.ZERO;
        int totalTransactions = 0;

        for (Investment investment : investments) {
            BigDecimal weight = BigDecimal.ZERO;
            if (totalValue.compareTo(BigDecimal.ZERO) > 0 && investment.getCurrentValue() != null) {
                weight = investment.getCurrentValue().divide(totalValue, 4, RoundingMode.HALF_UP);
            }

            // Sumar volatilidad ponderada (si está disponible en performance)
            if (investment.getPerformance() != null && investment.getPerformance().getVolatility() != null) {
                weightedVolatility = weightedVolatility.add(weight.multiply(investment.getPerformance().getVolatility()));
            }

            totalTransactions += investment.getTotalTransactions() != null ? investment.getTotalTransactions() : 0;
        }

        // Calcular retorno promedio del portfolio
        BigDecimal averageReturn = investments.stream()
            .map(inv -> inv.getGainLossPercentage() != null ? inv.getGainLossPercentage() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(investments.size()), 4, RoundingMode.HALF_UP);

        return Portfolio.PortfolioAnalytics.builder()
            .volatility(weightedVolatility)
            .beta(weightedBeta)
            .averageReturn(averageReturn)
            .totalTransactions(totalTransactions)
            .lastAnalysisAt(LocalDateTime.now())
            .build();
    }

    // ===================================================================
    // GESTIÓN DE OBJETIVOS FINANCIEROS
    // ===================================================================

    /**
     * Actualiza el objetivo financiero de un portfolio
     */
    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolioId")
    public Portfolio updateFinancialGoal(String portfolioId, Portfolio.FinancialGoal goal) {
        log.info("Actualizando objetivo financiero del portfolio: {}", portfolioId);
        
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(portfolioId);
        if (portfolioOpt.isEmpty()) {
            throw new IllegalArgumentException("Portfolio no encontrado: " + portfolioId);
        }

        Portfolio portfolio = portfolioOpt.get();
        portfolio.setGoal(goal);

        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
        log.info("Objetivo financiero actualizado para portfolio: {}", portfolioId);

        return updatedPortfolio;
    }

    /**
     * Verifica el progreso hacia los objetivos financieros
     */
    public BigDecimal calculateGoalProgress(String portfolioId) {
        log.debug("Calculando progreso del objetivo para portfolio: {}", portfolioId);
        
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(portfolioId);
        if (portfolioOpt.isEmpty() || portfolioOpt.get().getGoal() == null) {
            return BigDecimal.ZERO;
        }

        Portfolio portfolio = portfolioOpt.get();
        Portfolio.FinancialGoal goal = portfolio.getGoal();

        if (goal.getTargetAmount() == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentValue = portfolio.getTotalValue() != null ? portfolio.getTotalValue() : BigDecimal.ZERO;
        BigDecimal progress = currentValue.divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        return progress.min(BigDecimal.valueOf(100)); // Máximo 100%
    }

    // ===================================================================
    // ANÁLISIS Y REBALANCEADO
    // ===================================================================

    /**
     * Verifica si un portfolio necesita rebalanceado
     */
    public boolean needsRebalancing(String portfolioId) {
        log.debug("Verificando si portfolio {} necesita rebalanceado", portfolioId);
        
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(portfolioId);
        if (portfolioOpt.isEmpty()) {
            return false;
        }

        Portfolio portfolio = portfolioOpt.get();
        return portfolio.needsRebalancing();
    }

    /**
     * Obtiene sugerencias de rebalanceado para un portfolio
     */
    public List<String> getRebalancingSuggestions(String portfolioId) {
        log.debug("Obteniendo sugerencias de rebalanceado para portfolio: {}", portfolioId);
        
        List<String> suggestions = new java.util.ArrayList<>();
        
        List<Investment> investments = investmentRepository.findByPortfolioIdAndStatus(
            portfolioId, Investment.InvestmentStatus.ACTIVE);

        if (investments.isEmpty()) {
            suggestions.add("El portfolio no tiene inversiones activas");
            return suggestions;
        }

        // Analizar concentración
        BigDecimal totalValue = investments.stream()
            .map(inv -> inv.getCurrentValue() != null ? inv.getCurrentValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Investment investment : investments) {
            if (investment.getCurrentValue() != null && totalValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = investment.getCurrentValue()
                    .divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

                if (percentage.compareTo(BigDecimal.valueOf(40)) > 0) {
                    suggestions.add(String.format("Considerar reducir exposición a %s (%.1f%% del portfolio)", 
                        investment.getStockSymbol(), percentage));
                }
            }
        }

        // Sugerir diversificación si hay pocas inversiones
        if (investments.size() < 5) {
            suggestions.add("Considerar añadir más inversiones para mejorar diversificación");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("El portfolio está bien balanceado");
        }

        return suggestions;
    }

    /**
     * Marca un portfolio como rebalanceado
     */
    @Transactional
    @CacheEvict(value = "portfolios", key = "#portfolioId")
    public void markAsRebalanced(String portfolioId) {
        log.info("Marcando portfolio como rebalanceado: {}", portfolioId);
        
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(portfolioId);
        if (portfolioOpt.isPresent()) {
            Portfolio portfolio = portfolioOpt.get();
            portfolio.setLastRebalanceAt(LocalDateTime.now());
            portfolioRepository.save(portfolio);
        }
    }

    // ===================================================================
    // CONSULTAS Y BÚSQUEDAS
    // ===================================================================

    /**
     * Busca portfolios por nombre
     */
    public List<Portfolio> searchPortfoliosByName(String name) {
        log.debug("Buscando portfolios por nombre: {}", name);
        return portfolioRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Obtiene portfolios por tipo
     */
    public List<Portfolio> findByType(Portfolio.PortfolioType type) {
        log.debug("Obteniendo portfolios de tipo: {}", type);
        return portfolioRepository.findByType(type);
    }

    /**
     * Obtiene portfolios por nivel de riesgo
     */
    public List<Portfolio> findByRiskLevel(Portfolio.RiskLevel riskLevel) {
        log.debug("Obteniendo portfolios con nivel de riesgo: {}", riskLevel);
        return portfolioRepository.findByRiskLevel(riskLevel);
    }

    /**
     * Obtiene portfolios públicos
     */
    public List<Portfolio> findPublicPortfolios() {
        log.debug("Obteniendo portfolios públicos");
        return portfolioRepository.findByIsPublicTrue();
    }

    /**
     * Obtiene top portfolios por rendimiento
     */
    public List<Portfolio> findTopPerformingPortfolios() {
        log.debug("Obteniendo portfolios con mejor rendimiento");
        return portfolioRepository.findByIsActiveTrueOrderByTotalGainLossPercentageDesc();
    }

    // ===================================================================
    // VALIDACIONES Y UTILIDADES
    // ===================================================================

    /**
     * Valida límites de portfolios por usuario según su rol
     */
    private void validatePortfolioLimits(String userId) {
        long currentPortfolios = portfolioRepository.countByUserIdAndIsActiveTrue(userId);
        
        // Los límites se pueden configurar por rol del usuario
        // Por ahora, límite básico de 10 portfolios por usuario
        if (currentPortfolios >= 10) {
            throw new IllegalStateException("Usuario ha alcanzado el límite máximo de portfolios");
        }
    }

    /**
     * Verifica si un usuario es propietario de un portfolio
     */
    public boolean validateOwnership(String portfolioId, String userId) {
        Optional<Portfolio> portfolio = portfolioRepository.findById(portfolioId);
        return portfolio.isPresent() && portfolio.get().getUserId().equals(userId);
    }

    /**
     * Obtiene el valor total de todos los portfolios de un usuario
     * TEMPORALMENTE COMENTADO - Problema con Java 17+ y BigDecimal
     */
    // public BigDecimal getUserTotalPortfolioValue(String userId) {
    //     log.debug("Calculando valor total de portfolios del usuario: {}", userId);
    //     
    //     Optional<BigDecimal> totalValue = portfolioRepository.getTotalPortfolioValueByUser(userId);
    //     return totalValue.orElse(BigDecimal.ZERO);
    // }

    /**
     * Cuenta portfolios activos de un usuario
     */
    public long countActivePortfoliosByUser(String userId) {
        return portfolioRepository.countByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Verifica si un portfolio existe y está activo
     */
    public boolean existsAndIsActive(String portfolioId) {
        Optional<Portfolio> portfolio = portfolioRepository.findById(portfolioId);
        return portfolio.isPresent() && portfolio.get().getIsActive();
    }
}
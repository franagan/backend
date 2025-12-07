package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.dto.investment.*;
import com.inversionlibre.backend.model.Investment;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.InvestmentService;
import com.inversionlibre.backend.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de inversiones
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Investment", description = "Endpoints para gestión de inversiones")
@SecurityRequirement(name = "Bearer Authentication")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final PortfolioService portfolioService;

    /**
     * Obtiene todas las inversiones de un portfolio
     */
    @GetMapping("/portfolio/{portfolioId}")
    @Operation(summary = "Obtener inversiones del portfolio", description = "Retorna todas las inversiones de un portfolio específico")
    public ResponseEntity<ApiResponse<List<InvestmentResponse>>> getPortfolioInvestments(
            @PathVariable String portfolioId,
            @AuthenticationPrincipal User user) {
        
        log.info("Obteniendo inversiones del portfolio: {}", portfolioId);
        
        // Verificar ownership del portfolio
        if (!portfolioService.validateOwnership(portfolioId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para acceder a este portfolio"));
        }
        
        List<Investment> investments = investmentService.findByPortfolioId(portfolioId);
        List<InvestmentResponse> response = investments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Inversiones obtenidas exitosamente", response));
    }

    /**
     * Obtiene una inversión específica por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener inversión por ID", description = "Retorna los detalles de una inversión específica")
    public ResponseEntity<ApiResponse<InvestmentDetailResponse>> getInvestment(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        
        log.info("Obteniendo inversión: {}", id);
        
        Investment investment = investmentService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inversión no encontrada"));
        
        // Verificar ownership del portfolio
        if (!portfolioService.validateOwnership(investment.getPortfolioId(), user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para acceder a esta inversión"));
        }
        
        InvestmentDetailResponse response = convertToDetailResponse(investment);
        
        return ResponseEntity.ok(ApiResponse.success("Inversión obtenida exitosamente", response));
    }

    /**
     * Crea una nueva inversión
     */
    @PostMapping
    @Operation(summary = "Crear inversión", description = "Crea una nueva inversión en un portfolio")
    public ResponseEntity<ApiResponse<InvestmentResponse>> createInvestment(
            @Valid @RequestBody CreateInvestmentRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Creando nueva inversión en portfolio: {}", request.getPortfolioId());
        
        // Verificar ownership del portfolio
        if (!portfolioService.validateOwnership(request.getPortfolioId(), user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para modificar este portfolio"));
        }
        
        Investment investment = Investment.builder()
                .portfolioId(request.getPortfolioId())
                .stockId(request.getStockId())
                .stockSymbol(request.getStockSymbol())
                .stockName(request.getStockName())
                .quantity(request.getQuantity())
                .averagePrice(request.getAveragePrice())
                .currentPrice(request.getCurrentPrice())
                .build();
        
        Investment createdInvestment = investmentService.createInvestment(investment);
        InvestmentResponse response = convertToResponse(createdInvestment);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inversión creada exitosamente", response));
    }

    /**
     * Actualiza una inversión existente
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar inversión", description = "Actualiza la información de una inversión existente")
    public ResponseEntity<ApiResponse<InvestmentResponse>> updateInvestment(
            @PathVariable String id,
            @Valid @RequestBody UpdateInvestmentRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Actualizando inversión: {}", id);
        
        Investment investment = investmentService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inversión no encontrada"));
        
        // Verificar ownership del portfolio
        if (!portfolioService.validateOwnership(investment.getPortfolioId(), user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para modificar esta inversión"));
        }
        
        // Actualizar campos
        if (request.getQuantity() != null) investment.setQuantity(request.getQuantity());
        if (request.getAveragePrice() != null) investment.setAveragePrice(request.getAveragePrice());
        if (request.getCurrentPrice() != null) investment.setCurrentPrice(request.getCurrentPrice());
        if (request.getStrategy() != null) investment.setStrategy(request.getStrategy());
        if (request.getStatus() != null) investment.setStatus(request.getStatus());
        
        Investment updatedInvestment = investmentService.updateInvestment(investment);
        InvestmentResponse response = convertToResponse(updatedInvestment);
        
        return ResponseEntity.ok(ApiResponse.success("Inversión actualizada exitosamente", response));
    }

    /**
     * Elimina una inversión
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar inversión", description = "Elimina una inversión del portfolio")
    public ResponseEntity<ApiResponse<Void>> deleteInvestment(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        
        log.info("Eliminando inversión: {}", id);
        
        Investment investment = investmentService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inversión no encontrada"));
        
        // Verificar ownership del portfolio
        if (!portfolioService.validateOwnership(investment.getPortfolioId(), user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para eliminar esta inversión"));
        }
        
        investmentService.deleteInvestment(id);
        
        return ResponseEntity.ok(ApiResponse.success("Inversión eliminada exitosamente", null));
    }

    /**
     * Actualiza el precio actual de una inversión
     */
    @PatchMapping("/{id}/price")
    @Operation(summary = "Actualizar precio", description = "Actualiza el precio actual de una inversión")
    public ResponseEntity<ApiResponse<InvestmentResponse>> updatePrice(
            @PathVariable String id,
            @RequestParam java.math.BigDecimal newPrice,
            @AuthenticationPrincipal User user) {
        
        log.info("Actualizando precio de inversión: {} a {}", id, newPrice);
        
        Investment investment = investmentService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inversión no encontrada"));
        
        // Verificar ownership del portfolio
        if (!portfolioService.validateOwnership(investment.getPortfolioId(), user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para modificar esta inversión"));
        }
        
        Investment updatedInvestment = investmentService.updateCurrentPrice(id, newPrice);
        InvestmentResponse response = convertToResponse(updatedInvestment);
        
        return ResponseEntity.ok(ApiResponse.success("Precio actualizado exitosamente", response));
    }

    // ===================================================================
    // MÉTODOS DE CONVERSIÓN
    // ===================================================================

    private InvestmentResponse convertToResponse(Investment investment) {
        return InvestmentResponse.builder()
                .id(investment.getId())
                .portfolioId(investment.getPortfolioId())
                .stockId(investment.getStockId())
                .stockSymbol(investment.getStockSymbol())
                .stockName(investment.getStockName())
                .quantity(investment.getQuantity())
                .averagePrice(investment.getAveragePrice())
                .currentPrice(investment.getCurrentPrice())
                .totalInvested(investment.getTotalInvested())
                .currentValue(investment.getCurrentValue())
                .gainLoss(investment.getGainLoss())
                .gainLossPercentage(investment.getGainLossPercentage())
                .strategy(investment.getStrategy())
                .status(investment.getStatus())
                .createdAt(investment.getCreatedAt())
                .updatedAt(investment.getUpdatedAt())
                .build();
    }

    private InvestmentDetailResponse convertToDetailResponse(Investment investment) {
        return InvestmentDetailResponse.builder()
                .id(investment.getId())
                .portfolioId(investment.getPortfolioId())
                .stockId(investment.getStockId())
                .stockSymbol(investment.getStockSymbol())
                .stockName(investment.getStockName())
                .quantity(investment.getQuantity())
                .averagePrice(investment.getAveragePrice())
                .currentPrice(investment.getCurrentPrice())
                .totalInvested(investment.getTotalInvested())
                .currentValue(investment.getCurrentValue())
                .gainLoss(investment.getGainLoss())
                .gainLossPercentage(investment.getGainLossPercentage())
                .strategy(investment.getStrategy())
                .status(investment.getStatus())
                .alerts(investment.getAlerts())
                .goals(investment.getGoals())
                .performance(investment.getPerformance())
                .totalTransactions(investment.getTotalTransactions())
                .firstPurchaseDate(investment.getFirstPurchaseDate())
                .lastTransactionDate(investment.getLastTransactionDate())
                .createdAt(investment.getCreatedAt())
                .updatedAt(investment.getUpdatedAt())
                .build();
    }
}

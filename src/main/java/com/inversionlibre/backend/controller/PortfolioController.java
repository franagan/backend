package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.dto.portfolio.*;
import com.inversionlibre.backend.model.Portfolio;
import com.inversionlibre.backend.model.User;
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
 * Controlador REST para la gestión de portfolios/carteras de inversión
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Portfolio", description = "Endpoints para gestión de carteras de inversión")
@SecurityRequirement(name = "Bearer Authentication")
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Obtiene todos los portfolios del usuario autenticado
     */
    @GetMapping("/user")
    @Operation(summary = "Obtener portfolios del usuario", description = "Retorna todos los portfolios del usuario autenticado")
    public ResponseEntity<ApiResponse<List<PortfolioResponse>>> getUserPortfolios(
            @AuthenticationPrincipal User user) {
        
        log.info("Obteniendo portfolios del usuario: {}", user.getId());
        
        List<Portfolio> portfolios = portfolioService.findByUserId(user.getId());
        List<PortfolioResponse> response = portfolios.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Portfolios obtenidos exitosamente", response));
    }

    /**
     * Obtiene un portfolio específico por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener portfolio por ID", description = "Retorna los detalles de un portfolio específico")
    public ResponseEntity<ApiResponse<PortfolioDetailResponse>> getPortfolio(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        
        log.info("Obteniendo portfolio: {} para usuario: {}", id, user.getId());
        
        // Verificar ownership
        if (!portfolioService.validateOwnership(id, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para acceder a este portfolio"));
        }
        
        Portfolio portfolio = portfolioService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio no encontrado"));
        
        PortfolioDetailResponse response = convertToDetailResponse(portfolio);
        
        return ResponseEntity.ok(ApiResponse.success("Portfolio obtenido exitosamente", response));
    }

    /**
     * Obtiene el resumen de un portfolio
     */
    @GetMapping("/{id}/summary")
    @Operation(summary = "Obtener resumen del portfolio", description = "Retorna un resumen con métricas clave del portfolio")
    public ResponseEntity<ApiResponse<PortfolioSummaryResponse>> getPortfolioSummary(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        
        log.info("Obteniendo resumen del portfolio: {}", id);
        
        // Verificar ownership
        if (!portfolioService.validateOwnership(id, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para acceder a este portfolio"));
        }
        
        Portfolio portfolio = portfolioService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio no encontrado"));
        
        // Recalcular valores antes de retornar el resumen
        portfolio = portfolioService.recalculatePortfolioValues(id);
        
        PortfolioSummaryResponse response = convertToSummaryResponse(portfolio);
        
        return ResponseEntity.ok(ApiResponse.success("Resumen obtenido exitosamente", response));
    }

    /**
     * Crea un nuevo portfolio
     */
    @PostMapping
    @Operation(summary = "Crear portfolio", description = "Crea un nuevo portfolio para el usuario autenticado")
    public ResponseEntity<ApiResponse<PortfolioResponse>> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Creando nuevo portfolio para usuario: {}", user.getId());
        
        Portfolio portfolio = Portfolio.builder()
                .name(request.getName())
                .description(request.getDescription())
                .userId(user.getId())
                .type(request.getType() != null ? request.getType() : Portfolio.PortfolioType.PERSONAL)
                .riskLevel(request.getRiskLevel() != null ? request.getRiskLevel() : Portfolio.RiskLevel.MODERATE)
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .build();
        
        Portfolio createdPortfolio = portfolioService.createPortfolio(portfolio);
        PortfolioResponse response = convertToResponse(createdPortfolio);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Portfolio creado exitosamente", response));
    }

    /**
     * Actualiza un portfolio existente
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar portfolio", description = "Actualiza la información de un portfolio existente")
    public ResponseEntity<ApiResponse<PortfolioResponse>> updatePortfolio(
            @PathVariable String id,
            @Valid @RequestBody UpdatePortfolioRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("Actualizando portfolio: {}", id);
        
        // Verificar ownership
        if (!portfolioService.validateOwnership(id, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para modificar este portfolio"));
        }
        
        Portfolio portfolio = portfolioService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio no encontrado"));
        
        // Actualizar campos
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        if (request.getType() != null) portfolio.setType(request.getType());
        if (request.getRiskLevel() != null) portfolio.setRiskLevel(request.getRiskLevel());
        if (request.getCurrency() != null) portfolio.setCurrency(request.getCurrency());
        
        Portfolio updatedPortfolio = portfolioService.updatePortfolio(portfolio);
        PortfolioResponse response = convertToResponse(updatedPortfolio);
        
        return ResponseEntity.ok(ApiResponse.success("Portfolio actualizado exitosamente", response));
    }

    /**
     * Elimina un portfolio (soft delete)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar portfolio", description = "Desactiva un portfolio (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        
        log.info("Eliminando portfolio: {}", id);
        
        // Verificar ownership
        if (!portfolioService.validateOwnership(id, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para eliminar este portfolio"));
        }
        
        portfolioService.deletePortfolio(id);
        
        return ResponseEntity.ok(ApiResponse.success("Portfolio eliminado exitosamente", null));
    }

    /**
     * Recalcula los valores de un portfolio
     */
    @PostMapping("/{id}/recalculate")
    @Operation(summary = "Recalcular valores", description = "Recalcula todos los valores y métricas del portfolio")
    public ResponseEntity<ApiResponse<PortfolioResponse>> recalculatePortfolio(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        
        log.info("Recalculando valores del portfolio: {}", id);
        
        // Verificar ownership
        if (!portfolioService.validateOwnership(id, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permiso para acceder a este portfolio"));
        }
        
        Portfolio portfolio = portfolioService.recalculatePortfolioValues(id);
        PortfolioResponse response = convertToResponse(portfolio);
        
        return ResponseEntity.ok(ApiResponse.success("Valores recalculados exitosamente", response));
    }

    // ===================================================================
    // MÉTODOS DE CONVERSIÓN
    // ===================================================================

    private PortfolioResponse convertToResponse(Portfolio portfolio) {
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .type(portfolio.getType())
                .riskLevel(portfolio.getRiskLevel())
                .currency(portfolio.getCurrency())
                .totalValue(portfolio.getTotalValue())
                .totalInvested(portfolio.getTotalInvested())
                .totalGainLoss(portfolio.getTotalGainLoss())
                .totalGainLossPercentage(portfolio.getTotalGainLossPercentage())
                .totalInvestments(portfolio.getTotalInvestments())
                .isActive(portfolio.getIsActive())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .build();
    }

    private PortfolioDetailResponse convertToDetailResponse(Portfolio portfolio) {
        return PortfolioDetailResponse.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .type(portfolio.getType())
                .riskLevel(portfolio.getRiskLevel())
                .currency(portfolio.getCurrency())
                .totalValue(portfolio.getTotalValue())
                .totalInvested(portfolio.getTotalInvested())
                .totalGainLoss(portfolio.getTotalGainLoss())
                .totalGainLossPercentage(portfolio.getTotalGainLossPercentage())
                .totalInvestments(portfolio.getTotalInvestments())
                .isActive(portfolio.getIsActive())
                .isPublic(portfolio.getIsPublic())
                .goal(portfolio.getGoal())
                .analytics(portfolio.getAnalytics())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .lastRebalanceAt(portfolio.getLastRebalanceAt())
                .build();
    }

    private PortfolioSummaryResponse convertToSummaryResponse(Portfolio portfolio) {
        return PortfolioSummaryResponse.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .totalValue(portfolio.getTotalValue())
                .totalReturn(portfolio.getTotalGainLossPercentage())
                .positions(portfolio.getTotalInvestments())
                .build();
    }
}

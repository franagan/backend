package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.model.Transaction;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de transacciones
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction", description = "Endpoints para gestión de transacciones")
@SecurityRequirement(name = "Bearer Authentication")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Obtiene todas las transacciones del usuario (paginado)
     */
    @GetMapping
    @Operation(summary = "Obtener transacciones del usuario", description = "Retorna el historial de transacciones del usuario autenticado")
    public ResponseEntity<ApiResponse<Page<Transaction>>> getUserTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "executedAt,desc") String sort) {
        
        log.info("Obteniendo transacciones pal usuario: {}", user.getId());
        
        String[] sortParams = sort.split(",");
        Sort sortOrder = Sort.by(sortParams[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortParams[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        
        Page<Transaction> transactions = transactionService.findByUserId(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Transacciones obtenidas", transactions));
    }

    /**
     * Obtiene transacciones de un portfolio específico
     */
    @GetMapping("/portfolio/{portfolioId}")
    @Operation(summary = "Obtener transacciones de un portfolio", description = "Retorna el historial de transacciones de un portfolio")
    public ResponseEntity<ApiResponse<List<Transaction>>> getPortfolioTransactions(
            @PathVariable String portfolioId) {
        
        List<Transaction> transactions = transactionService.findByPortfolioId(portfolioId);
        return ResponseEntity.ok(ApiResponse.success("Transacciones del portfolio obtenidas", transactions));
    }

    /**
     * Procesa una compra de acciones
     */
    @PostMapping("/buy")
    @Operation(summary = "Procesar compra", description = "Registra una compra de acciones y actualiza la inversión")
    public ResponseEntity<ApiResponse<Transaction>> buyStock(
            @Valid @RequestBody Transaction transaction,
            @AuthenticationPrincipal User user) {
        
        log.info("Procesando compra de {} para usuario {}", transaction.getStockSymbol(), user.getId());
        transaction.setUserId(user.getId());
        transaction.setType(Transaction.TransactionType.BUY);
        
        Transaction processed = transactionService.processBuyTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Compra procesada con éxito", processed));
    }

    /**
     * Procesa una venta de acciones
     */
    @PostMapping("/sell")
    @Operation(summary = "Procesar venta", description = "Registra una venta de acciones y actualiza la inversión")
    public ResponseEntity<ApiResponse<Transaction>> sellStock(
            @Valid @RequestBody Transaction transaction,
            @AuthenticationPrincipal User user) {
        
        log.info("Procesando venta de {} para usuario {}", transaction.getStockSymbol(), user.getId());
        transaction.setUserId(user.getId());
        transaction.setType(Transaction.TransactionType.SELL);
        
        Transaction processed = transactionService.processSellTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Venta procesada con éxito", processed));
    }

    /**
     * Registra un dividendo
     */
    @PostMapping("/dividend")
    @Operation(summary = "Registrar dividendo", description = "Registra la recepción de un dividendo")
    public ResponseEntity<ApiResponse<Transaction>> collectDividend(
            @Valid @RequestBody Transaction transaction,
            @AuthenticationPrincipal User user) {
        
        log.info("Registrando dividendo de {} para usuario {}", transaction.getStockSymbol(), user.getId());
        transaction.setUserId(user.getId());
        transaction.setType(Transaction.TransactionType.DIVIDEND);
        
        Transaction processed = transactionService.processDividendTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Dividendo registrado con éxito", processed));
    }

    /**
     * Obtiene detalles de una transacción
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de transacción", description = "Retorna los detalles de una transacción específica")
    public ResponseEntity<ApiResponse<Transaction>> getTransaction(@PathVariable String id) {
        return transactionService.findById(id)
                .map(t -> ResponseEntity.ok(ApiResponse.success("Transacción encontrada", t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancela una transacción (si está pendiente)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar transacción", description = "Cancela una transacción que aún esté en estado pendiente")
    public ResponseEntity<ApiResponse<Transaction>> cancelTransaction(@PathVariable String id) {
        Transaction cancelled = transactionService.cancelTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Transacción cancelada", cancelled));
    }
}

package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.dto.stock.CompanyOverview;
import com.inversionlibre.backend.dto.stock.StockQuote;
import com.inversionlibre.backend.dto.stock.StockSearchResult;
import com.inversionlibre.backend.service.StockDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para datos de stocks con múltiples proveedores
 * Usa Finnhub como primario y Alpha Vantage como fallback
 * 
 * @author Francisco Palero
 * @version 2.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class StockDataController {

    private final StockDataService stockDataService;

    /**
     * Busca stocks por palabra clave
     * GET /api/stocks/search?keywords=apple
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockSearchResult>>> searchStocks(
            @RequestParam String keywords) {
        
        log.info("Buscando stocks con keywords: {}", keywords);
        
        if (keywords == null || keywords.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Keywords es requerido"));
        }
        
        List<StockSearchResult> results = stockDataService.searchStocks(keywords);
        
        if (results.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.success("No se encontraron resultados", results));
        }
        
        return ResponseEntity.ok(
                ApiResponse.success("Búsqueda exitosa", results));
    }

    /**
     * Obtiene cotización actual de un stock
     * GET /api/stocks/{symbol}/quote
     */
    @GetMapping("/{symbol}/quote")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockQuote>> getStockQuote(
            @PathVariable String symbol) {
        
        log.info("Obteniendo cotización para: {}", symbol);
        
        StockQuote quote = stockDataService.getStockQuote(symbol.toUpperCase());
        
        if (quote == null) {
            return ResponseEntity.ok(
                    ApiResponse.error("No se encontró cotización para: " + symbol));
        }
        
        return ResponseEntity.ok(
                ApiResponse.success("Cotización obtenida exitosamente", quote));
    }

    /**
     * Obtiene información general de una empresa
     * GET /api/stocks/{symbol}/overview
     */
    @GetMapping("/{symbol}/overview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CompanyOverview>> getCompanyOverview(
            @PathVariable String symbol) {
        
        log.info("Obteniendo información de empresa para: {}", symbol);
        
        CompanyOverview overview = stockDataService.getCompanyOverview(symbol.toUpperCase());
        
        if (overview == null) {
            return ResponseEntity.ok(
                    ApiResponse.error("No se encontró información para: " + symbol));
        }
        
        return ResponseEntity.ok(
                ApiResponse.success("Información obtenida exitosamente", overview));
    }

    /**
     * Obtiene solo el precio actual de un stock (endpoint ligero)
     * GET /api/stocks/{symbol}/price
     */
    @GetMapping("/{symbol}/price")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStockPrice(
            @PathVariable String symbol) {
        
        log.info("Obteniendo precio para: {}", symbol);
        
        BigDecimal price = stockDataService.getStockPrice(symbol.toUpperCase());
        
        if (price == null) {
            return ResponseEntity.ok(
                    ApiResponse.error("No se encontró precio para: " + symbol));
        }
        
        Map<String, Object> response = Map.of(
                "symbol", symbol.toUpperCase(),
                "price", price
        );
        
        return ResponseEntity.ok(
                ApiResponse.success("Precio obtenido exitosamente", response));
    }
}

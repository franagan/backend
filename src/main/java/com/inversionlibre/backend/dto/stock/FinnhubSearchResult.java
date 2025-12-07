package com.inversionlibre.backend.dto.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resultados de búsqueda de Finnhub API
 * Endpoint: /api/v1/search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinnhubSearchResult {
    
    @JsonProperty("description")
    private String description; // Company name
    
    @JsonProperty("displaySymbol")
    private String displaySymbol; // Display symbol
    
    @JsonProperty("symbol")
    private String symbol; // Unique symbol
    
    @JsonProperty("type")
    private String type; // Security type (Common Stock, ETF, etc.)
}

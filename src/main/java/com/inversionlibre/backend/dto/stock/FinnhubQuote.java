package com.inversionlibre.backend.dto.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cotización de Finnhub API
 * Endpoint: /api/v1/quote
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinnhubQuote {
    
    @JsonProperty("c")
    private Double currentPrice; // Current price
    
    @JsonProperty("d")
    private Double change; // Change
    
    @JsonProperty("dp")
    private Double percentChange; // Percent change
    
    @JsonProperty("h")
    private Double highPrice; // High price of the day
    
    @JsonProperty("l")
    private Double lowPrice; // Low price of the day
    
    @JsonProperty("o")
    private Double openPrice; // Open price of the day
    
    @JsonProperty("pc")
    private Double previousClose; // Previous close price
    
    @JsonProperty("t")
    private Long timestamp; // Timestamp
}

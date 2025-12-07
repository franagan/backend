package com.inversionlibre.backend.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para cotización de stock desde Alpha Vantage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockQuote {
    
    private String symbol;
    private BigDecimal price;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal previousClose;
    private BigDecimal change;
    private BigDecimal changePercent;
    private Long volume;
    private LocalDateTime lastUpdated;
}

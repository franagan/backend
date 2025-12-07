package com.inversionlibre.backend.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resultados de búsqueda de stocks desde Alpha Vantage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSearchResult {
    
    private String symbol;
    private String name;
    private String type;
    private String region;
    private String currency;
    private String matchScore;
    
    /**
     * Indica si esta acción tiene datos de precio en tiempo real disponibles
     * Si es false, el usuario deberá ingresar el precio manualmente
     */
    private boolean hasLiveData;
}

package com.inversionlibre.backend.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para información general de una empresa desde Alpha Vantage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyOverview {
    
    private String symbol;
    private String name;
    private String description;
    private String sector;
    private String industry;
    private String country;
    private String currency;
    private BigDecimal marketCap;
    private BigDecimal peRatio;
    private BigDecimal dividendYield;
    private BigDecimal eps;
    private BigDecimal beta;
    private BigDecimal week52High;
    private BigDecimal week52Low;
}

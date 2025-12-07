package com.inversionlibre.backend.dto.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para perfil de compañía de Finnhub API
 * Endpoint: /api/v1/stock/profile2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinnhubCompanyProfile {
    
    @JsonProperty("country")
    private String country;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("exchange")
    private String exchange;
    
    @JsonProperty("ipo")
    private String ipo; // IPO date
    
    @JsonProperty("marketCapitalization")
    private Double marketCapitalization;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("phone")
    private String phone;
    
    @JsonProperty("shareOutstanding")
    private Double shareOutstanding;
    
    @JsonProperty("ticker")
    private String ticker;
    
    @JsonProperty("weburl")
    private String weburl;
    
    @JsonProperty("logo")
    private String logo;
    
    @JsonProperty("finnhubIndustry")
    private String finnhubIndustry;
}

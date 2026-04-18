package com.inversionlibre.backend.dto.stock;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StockDetailsDTO {
    private CompanyOverview overview;
    private StockQuote quote;
    private JsonNode timeSeries;
    private JsonNode recommendations;
    private JsonNode news;
    private List<FinnhubSearchResult> relatedStocks;
    private java.util.Map<Integer, java.util.Map<Integer, Double>> monthlyReturns;
    private JsonNode dividendHistory;
    private Double exchangeRateToEUR;
    private UserStockPerformance userPerformance;
}

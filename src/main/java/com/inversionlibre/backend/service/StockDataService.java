package com.inversionlibre.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inversionlibre.backend.dto.stock.*;
import com.inversionlibre.backend.dto.stock.StockDetailsDTO;
import com.inversionlibre.backend.repository.TransactionRepository;
import com.inversionlibre.backend.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Servicio unificado para datos de acciones que combina múltiples proveedores
 * con fallback automático.
 */
@Slf4j
@Service
public class StockDataService {

    private final FinnhubService finnhubService;
    private final AlphaVantageService alphaVantageService;
    private final PythonFinanceService pythonFinanceService;
    private final TransactionRepository transactionRepository;
    private final boolean simulationMode = false; 

    public StockDataService(FinnhubService finnhubService, 
                           AlphaVantageService alphaVantageService,
                           PythonFinanceService pythonFinanceService,
                           TransactionRepository transactionRepository) {
        this.finnhubService = finnhubService;
        this.alphaVantageService = alphaVantageService;
        this.pythonFinanceService = pythonFinanceService;
        this.transactionRepository = transactionRepository;
    }

    public List<StockSearchResult> searchStocks(String keywords) {
        try {
            List<FinnhubSearchResult> res = finnhubService.searchSymbols(keywords);
            if (res != null && !res.isEmpty()) return convertFinnhubSearchResults(res);
        } catch (Exception e) { log.warn("Finnhub failed: {}", e.getMessage()); }
        try {
            return alphaVantageService.searchSymbols(keywords);
        } catch (Exception e) { return new ArrayList<>(); }
    }

    public StockQuote getStockQuote(String symbol) {
        try {
            FinnhubQuote quote = finnhubService.getQuote(symbol);
            if (quote != null) return convertFinnhubQuote(symbol, quote);
        } catch (Exception e) {}
        try {
            StockQuote q = alphaVantageService.getQuote(symbol);
            if (q != null) return q;
        } catch (Exception e) {}
        return simulationMode ? generateMockQuote(symbol) : null;
    }

    public CompanyOverview getCompanyOverview(String symbol) {
        // Prioritize Alpha Vantage for OVERVIEW as it has more fundamental data (P/E, EPS, etc.)
        try {
            CompanyOverview avOverview = alphaVantageService.getCompanyOverview(symbol);
            if (avOverview != null) return avOverview;
        } catch (Exception e) { log.debug("Alpha Vantage overview failed for {}: {}", symbol, e.getMessage()); }

        // Fallback to Finnhub profile (more limited)
        try {
            FinnhubCompanyProfile profile = finnhubService.getCompanyProfile(symbol);
            if (profile != null) return convertFinnhubProfile(symbol, profile);
        } catch (Exception e) {}
        
        return null;
    }

    public StockDetailsDTO getStockDetails(String symbol, String userId) {
        log.info("Obteniendo detalles completos para: {} (Usuario: {})", symbol, userId);
        
        // Intentar obtener enriquecimiento desde Yahoo (Python)
        JsonNode yahooData = pythonFinanceService.getStockData(symbol);
        
        CompanyOverview overview = getCompanyOverview(symbol);
        StockQuote quote = getStockQuote(symbol);
        JsonNode timeSeries = alphaVantageService.getTimeSeries(symbol);
        
        // Si Yahoo tiene datos, los usamos como complemento o reemplazo si falta algo
        if (yahooData != null) {
            if (overview == null && yahooData.has("overview")) {
                overview = convertYahooToOverview(yahooData.get("overview"));
            }
            if ((timeSeries == null || !timeSeries.has("Time Series (Daily)")) && yahooData.has("timeSeries")) {
                timeSeries = convertYahooToTimeSeries(yahooData.get("timeSeries"));
            }
            
            // Si la cotización es mock o nula, intentar usar la de Yahoo
            if (yahooData.has("overview") && yahooData.get("overview").has("price")) {
                BigDecimal yPrice = new BigDecimal(yahooData.get("overview").get("price").asText());
                if (quote == null || quote.getPrice().compareTo(BigDecimal.valueOf(50)) == 0 || simulationMode) {
                    if (quote == null) {
                        quote = StockQuote.builder()
                            .symbol(symbol)
                            .price(yPrice)
                            .change(BigDecimal.ZERO)
                            .changePercent(BigDecimal.ZERO)
                            .lastUpdated(LocalDateTime.now())
                            .build();
                    } else {
                        quote.setPrice(yPrice);
                        // Intentar sacar el yield del overview si viene bien
                        if (overview != null && overview.getDividendYield() != null) {
                             // Yahoo suele dar yield en ratio (0.04), dejarlo así para el front
                        }
                    }
                }
            }
        }

        // Simulation for chart if still empty
        if (simulationMode && (timeSeries == null || !timeSeries.has("Time Series (Daily)"))) {
            timeSeries = generateMockTimeSeries(symbol, 500);
        }

        StockDetailsDTO details = StockDetailsDTO.builder()
            .overview(overview)
            .quote(quote)
            .timeSeries(timeSeries)
            .recommendations(finnhubService.getRecommendationTrends(symbol))
            .news(finnhubService.getCompanyNews(symbol))
            .relatedStocks(finnhubService.searchSymbols(symbol.substring(0, Math.min(symbol.length(), 2))))
            .monthlyReturns(calculateHeatmapData(symbol))
            .dividendHistory(yahooData != null && yahooData.has("dividendHistory") && yahooData.get("dividendHistory").size() > 0
                ? convertYahooDividends(yahooData.get("dividendHistory"))
                : alphaVantageService.getDividends(symbol))
            .exchangeRateToEUR(yahooData != null && yahooData.has("exchangeRateToEUR") 
                ? yahooData.get("exchangeRateToEUR").asDouble() 
                : 1.0)
            .userPerformance(calculateUserPerformance(userId, symbol, quote))
            .build();

        // Si el yield es 0 pero tenemos historial, calculamos el real (TTM)
        if (details.getOverview() != null && 
           (details.getOverview().getDividendYield() == null || details.getOverview().getDividendYield().compareTo(BigDecimal.ZERO) == 0) &&
            details.getDividendHistory() != null && details.getDividendHistory().has("data")) {
            
            try {
                BigDecimal sum = BigDecimal.ZERO;
                JsonNode data = details.getDividendHistory().get("data");
                java.time.LocalDate oneYearAgo = java.time.LocalDate.now().minusYears(1);
                
                for (int i = 0; i < data.size(); i++) {
                    String exDateStr = data.get(i).get("ex_date").asText();
                    java.time.LocalDate exDate = java.time.LocalDate.parse(exDateStr);
                    if (exDate.isAfter(oneYearAgo)) {
                        sum = sum.add(new BigDecimal(data.get(i).get("amount").asText()));
                    }
                }
                
                if (details.getQuote() != null && details.getQuote().getPrice().compareTo(BigDecimal.ZERO) > 0 && sum.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal yield = sum.divide(details.getQuote().getPrice(), 4, RoundingMode.HALF_UP);
                    details.getOverview().setDividendYield(yield);
                }
            } catch (Exception e) {
                log.warn("Error calculando yield manual TTM: {}", e.getMessage());
            }
        }

        return details;
    }

    private UserStockPerformance calculateUserPerformance(String userId, String symbol, StockQuote quote) {
        try {
            List<Transaction> transactions = transactionRepository.findByUserIdAndStockSymbolAndStatusIn(
                userId, symbol, List.of(Transaction.TransactionStatus.EXECUTED, Transaction.TransactionStatus.SETTLED)
            );
            
            if (transactions.isEmpty()) return null;
            
            double totalQuantity = 0;
            BigDecimal investedAmount = BigDecimal.ZERO;
            BigDecimal totalDividends = BigDecimal.ZERO;
            BigDecimal realizedGain = BigDecimal.ZERO;
            
            for (Transaction t : transactions) {
                switch (t.getType()) {
                    case BUY:
                    case DIVIDEND_REINVEST:
                        totalQuantity += t.getQuantity().doubleValue();
                        investedAmount = investedAmount.add(t.getNetAmount());
                        break;
                    case SELL:
                        totalQuantity -= t.getQuantity().doubleValue();
                        // Simplistic realized gain for now
                        realizedGain = realizedGain.add(t.getNetAmount()); 
                        break;
                    case DIVIDEND:
                        totalDividends = totalDividends.add(t.getNetAmount());
                        break;
                    default:
                        break;
                }
            }
            
            BigDecimal currentPrice = (quote != null) ? quote.getPrice() : BigDecimal.ZERO;
            BigDecimal totalValue = currentPrice.multiply(BigDecimal.valueOf(totalQuantity));
            BigDecimal priceGain = totalValue.subtract(investedAmount);
            BigDecimal priceGainPercent = (investedAmount.compareTo(BigDecimal.ZERO) > 0) 
                ? priceGain.divide(investedAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
                
            return UserStockPerformance.builder()
                .totalValue(totalValue)
                .investedAmount(investedAmount)
                .priceGain(priceGain)
                .priceGainPercent(priceGainPercent)
                .realizedGain(realizedGain)
                .totalReturn(priceGain.add(totalDividends))
                .averageBuyPrice(totalQuantity > 0 ? investedAmount.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .quantity(totalQuantity)
                .totalDividends(totalDividends)
                .latestTransactions(transactions.size() > 10 ? transactions.subList(0, 10) : transactions)
                .build();
                
        } catch (Exception e) {
            log.warn("Error calculando performance para {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private JsonNode convertYahooDividends(JsonNode yDivs) {
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
        root.set("data", yDivs);
        return root;
    }

    private CompanyOverview convertYahooToOverview(JsonNode y) {
        return CompanyOverview.builder()
            .symbol(y.get("symbol").asText())
            .name(y.get("name").asText())
            .description(y.get("description").asText(""))
            .sector(y.get("sector").asText(""))
            .industry(y.get("industry").asText(""))
            .marketCap(safeBigDecimal(y.get("marketCap")))
            .peRatio(safeBigDecimal(y.get("peRatio")))
            .dividendYield(safeBigDecimal(y.get("dividendYield")))
            .week52High(safeBigDecimal(y.get("week52High")))
            .week52Low(safeBigDecimal(y.get("week52Low")))
            .currency(y.get("currency") != null ? y.get("currency").asText() : "USD")
            .build();
    }

    private JsonNode convertYahooToTimeSeries(JsonNode y) {
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
        root.set("Time Series (Daily)", y);
        return root;
    }

    /**
     * Genera datos para el Heatmap de rentabilidades mensuales
     */
    private Map<Integer, Map<Integer, Double>> calculateHeatmapData(String symbol) {
        Map<Integer, Map<Integer, Double>> heatmap = new TreeMap<>((a, b) -> b - a); // Descending years
        
        // Mocking for now to avoid massive API calls in TFG context, 
        // Real implementation would parse TIME_SERIES_MONTHLY
        int currentYear = LocalDateTime.now().getYear();
        for (int y = currentYear; y >= currentYear - 3; y--) {
            Map<Integer, Double> months = new TreeMap<>();
            for (int m = 1; m <= 12; m++) {
                // Random returns between -10% and +15% based on symbol hash
                double ret = ((Math.abs((symbol + y + m).hashCode()) % 250) / 10.0) - 10.0;
                months.put(m, ret);
            }
            heatmap.put(y, months);
        }
        return heatmap;
    }

    public BigDecimal getStockPrice(String symbol) {
        StockQuote q = getStockQuote(symbol);
        return q != null ? q.getPrice() : (simulationMode ? generateMockPrice(symbol) : null);
    }

    private List<StockSearchResult> convertFinnhubSearchResults(List<FinnhubSearchResult> finnhubResults) {
        List<StockSearchResult> results = new ArrayList<>();
        for (FinnhubSearchResult finnhub : finnhubResults) {
            StockSearchResult result = new StockSearchResult();
            result.setSymbol(finnhub.getSymbol());
            result.setName(finnhub.getDescription());
            result.setType(finnhub.getType());
            result.setRegion("US");
            results.add(result);
        }
        return results;
    }

    private StockQuote convertFinnhubQuote(String symbol, FinnhubQuote finnhub) {
        return StockQuote.builder()
            .symbol(symbol)
            .price(BigDecimal.valueOf(finnhub.getCurrentPrice()))
            .change(BigDecimal.valueOf(finnhub.getChange()))
            .changePercent(BigDecimal.valueOf(finnhub.getPercentChange()))
            .high(BigDecimal.valueOf(finnhub.getHighPrice()))
            .low(BigDecimal.valueOf(finnhub.getLowPrice()))
            .open(BigDecimal.valueOf(finnhub.getOpenPrice()))
            .previousClose(BigDecimal.valueOf(finnhub.getPreviousClose()))
            .volume(finnhub.getTimestamp())
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    private CompanyOverview convertFinnhubProfile(String symbol, FinnhubCompanyProfile finnhub) {
        CompanyOverview overview = new CompanyOverview();
        overview.setSymbol(symbol);
        overview.setName(finnhub.getName());
        overview.setDescription("Stock profile for " + finnhub.getName());
        overview.setCountry(finnhub.getCountry());
        overview.setCurrency(finnhub.getCurrency());
        overview.setSector(finnhub.getFinnhubIndustry());
        overview.setIndustry(finnhub.getFinnhubIndustry());
        if (finnhub.getMarketCapitalization() != null) {
            overview.setMarketCap(BigDecimal.valueOf(finnhub.getMarketCapitalization() * 1_000_000));
        }
        return overview;
    }

    private JsonNode generateMockTimeSeries(String symbol, int days) {
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
        com.fasterxml.jackson.databind.node.ObjectNode ts = mapper.createObjectNode();
        
        double currentPrice = generateMockPrice(symbol).doubleValue();
        java.time.LocalDate date = java.time.LocalDate.now();
        
        // Simular tendencia histórica (por ejemplo, Apple suele subir a largo plazo)
        double annualReturn = 0.15; // 15% anualizado
        double dailyReturn = annualReturn / 252;
        
        double walkingPrice = currentPrice;
        
        for (int i = 0; i < days; i++) {
            com.fasterxml.jackson.databind.node.ObjectNode values = mapper.createObjectNode();
            
            // Valor de cierre para este día
            values.put("4. close", String.format("%.2f", walkingPrice));
            ts.set(date.toString(), values);
            
            // Retroceder en el tiempo: el precio "anterior" solía ser menor (si la tendencia es alcista)
            double volatility = (Math.random() * 0.04 - 0.02); // +/- 2% diario
            walkingPrice = walkingPrice / (1 + dailyReturn + volatility);
            
            date = date.minusDays(1);
        }
        
        root.set("Time Series (Daily)", ts);
        return root;
    }

    private BigDecimal generateMockPrice(String symbol) {
        // Precios reales aproximados (Abril 2024) para las más comunes
        if (symbol.equalsIgnoreCase("AAPL")) return BigDecimal.valueOf(167.00);
        if (symbol.equalsIgnoreCase("GOOG") || symbol.equalsIgnoreCase("GOOGL")) return BigDecimal.valueOf(155.00);
        if (symbol.equalsIgnoreCase("MSFT")) return BigDecimal.valueOf(412.00);
        if (symbol.equalsIgnoreCase("AMZN")) return BigDecimal.valueOf(181.00);
        if (symbol.equalsIgnoreCase("TSLA")) return BigDecimal.valueOf(155.00);
        
        // Genérico si no es de las anteriores
        double base = (Math.abs(symbol.hashCode()) % 200) + 50.0;
        return BigDecimal.valueOf(base).setScale(2, RoundingMode.HALF_UP);
    }

    private StockQuote generateMockQuote(String symbol) {
        BigDecimal price = generateMockPrice(symbol);
        return StockQuote.builder()
            .symbol(symbol).price(price)
            .change(BigDecimal.valueOf(0.5)).changePercent(BigDecimal.valueOf(0.25))
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    private BigDecimal safeBigDecimal(JsonNode node) {
        if (node == null || node.isNull() || node.asText().equalsIgnoreCase("null") || node.asText().equalsIgnoreCase("None") || node.asText().trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(node.asText());
        } catch (Exception e) {
            return null;
        }
    }
}

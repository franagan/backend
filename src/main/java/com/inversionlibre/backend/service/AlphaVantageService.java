package com.inversionlibre.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inversionlibre.backend.dto.stock.CompanyOverview;
import com.inversionlibre.backend.dto.stock.StockQuote;
import com.inversionlibre.backend.dto.stock.StockSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para integración con Alpha Vantage API
 * Proporciona datos de stocks en tiempo real
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlphaVantageService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Value("${alphavantage.api.base-url}")
    private String baseUrl;

    /**
     * Busca símbolos de stocks por palabra clave
     */
    @Cacheable(value = "stockSearch", key = "#keywords")
    public List<StockSearchResult> searchSymbols(String keywords) {
        log.info("Buscando símbolos para: {}", keywords);
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("function", "SYMBOL_SEARCH")
                    .queryParam("keywords", keywords)
                    .queryParam("apikey", apiKey)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            
            // Verificación de seguridad: ¿Es HTML en lugar de JSON?
            if (response != null && (response.trim().startsWith("<") || response.trim().toLowerCase().startsWith("<!doctype"))) {
                log.warn("Alpha Vantage devolvió HTML en lugar de JSON en searchSymbols (posible límite de API alcanzado)");
                return new ArrayList<>();
            }
            
            JsonNode root = objectMapper.readTree(response);
            
            // Verificar si hay error
            if (root.has("Error Message")) {
                log.error("Error de Alpha Vantage: {}", root.get("Error Message").asText());
                return new ArrayList<>();
            }
            
            // Verificar rate limit
            if (root.has("Note")) {
                log.warn("Rate limit alcanzado: {}", root.get("Note").asText());
                return new ArrayList<>();
            }

            List<StockSearchResult> results = new ArrayList<>();
            JsonNode matches = root.get("bestMatches");
            
            if (matches != null && matches.isArray()) {
                for (JsonNode match : matches) {
                    StockSearchResult result = StockSearchResult.builder()
                            .symbol(match.get("1. symbol").asText())
                            .name(match.get("2. name").asText())
                            .type(match.get("3. type").asText())
                            .region(match.get("4. region").asText())
                            .currency(match.get("8. currency").asText())
                            .matchScore(match.get("9. matchScore").asText())
                            .build();
                    results.add(result);
                }
            }
            
            log.info("Encontrados {} resultados para '{}'", results.size(), keywords);
            return results;
            
        } catch (Exception e) {
            log.error("Error buscando símbolos: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene cotización actual de un stock
     */
    @Cacheable(value = "stockQuotes", key = "#symbol")
    public StockQuote getQuote(String symbol) {
        log.info("Obteniendo cotización para: {}", symbol);
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("function", "GLOBAL_QUOTE")
                    .queryParam("symbol", symbol)
                    .queryParam("apikey", apiKey)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            
            // Verificación de seguridad: ¿Es HTML en lugar de JSON?
            if (response != null && (response.trim().startsWith("<") || response.trim().toLowerCase().startsWith("<!doctype"))) {
                log.warn("Alpha Vantage devolvió HTML en lugar de JSON en getQuote (posible límite de API alcanzado)");
                return null;
            }
            
            JsonNode root = objectMapper.readTree(response);
            
            // Verificar si hay error
            if (root.has("Error Message")) {
                log.error("Error de Alpha Vantage: {}", root.get("Error Message").asText());
                return null;
            }
            
            // Verificar rate limit
            if (root.has("Note")) {
                log.warn("Rate limit alcanzado: {}", root.get("Note").asText());
                return null;
            }

            JsonNode quote = root.get("Global Quote");
            if (quote == null || quote.isEmpty()) {
                log.warn("No se encontró cotización para: {}", symbol);
                return null;
            }

            StockQuote stockQuote = StockQuote.builder()
                    .symbol(quote.get("01. symbol").asText())
                    .price(new BigDecimal(quote.get("05. price").asText()))
                    .open(new BigDecimal(quote.get("02. open").asText()))
                    .high(new BigDecimal(quote.get("03. high").asText()))
                    .low(new BigDecimal(quote.get("04. low").asText()))
                    .previousClose(new BigDecimal(quote.get("08. previous close").asText()))
                    .change(new BigDecimal(quote.get("09. change").asText()))
                    .changePercent(parsePercentage(quote.get("10. change percent").asText()))
                    .volume(Long.parseLong(quote.get("06. volume").asText()))
                    .lastUpdated(LocalDateTime.now())
                    .build();
            
            log.info("Cotización obtenida para {}: ${}", symbol, stockQuote.getPrice());
            return stockQuote;
            
        } catch (Exception e) {
            log.error("Error obteniendo cotización para {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtiene información general de una empresa
     */
    @Cacheable(value = "companyOverview", key = "#symbol")
    public CompanyOverview getCompanyOverview(String symbol) {
        log.info("Obteniendo información de empresa para: {}", symbol);
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("function", "OVERVIEW")
                    .queryParam("symbol", symbol)
                    .queryParam("apikey", apiKey)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            
            // Verificación de seguridad: ¿Es HTML en lugar de JSON?
            if (response != null && (response.trim().startsWith("<") || response.trim().toLowerCase().startsWith("<!doctype"))) {
                log.warn("Alpha Vantage devolvió HTML en lugar de JSON en getCompanyOverview (posible límite de API alcanzado)");
                return null;
            }
            
            JsonNode root = objectMapper.readTree(response);
            
            // Verificar si hay error
            if (root.has("Error Message")) {
                log.error("Error de Alpha Vantage: {}", root.get("Error Message").asText());
                return null;
            }
            
            // Verificar rate limit
            if (root.has("Note")) {
                log.warn("Rate limit alcanzado: {}", root.get("Note").asText());
                return null;
            }

            if (root.isEmpty() || !root.has("Symbol")) {
                log.warn("No se encontró información para: {}", symbol);
                return null;
            }

            CompanyOverview overview = CompanyOverview.builder()
                    .symbol(root.get("Symbol").asText())
                    .name(root.get("Name").asText())
                    .description(root.get("Description").asText(""))
                    .sector(root.get("Sector").asText(""))
                    .industry(root.get("Industry").asText(""))
                    .country(root.get("Country").asText(""))
                    .currency(root.get("Currency").asText(""))
                    .marketCap(parseBigDecimal(root.get("MarketCapitalization")))
                    .peRatio(parseBigDecimal(root.get("PERatio")))
                    .dividendYield(parseBigDecimal(root.get("DividendYield")))
                    .eps(parseBigDecimal(root.get("EPS")))
                    .beta(parseBigDecimal(root.get("Beta")))
                    .week52High(parseBigDecimal(root.get("52WeekHigh")))
                    .week52Low(parseBigDecimal(root.get("52WeekLow")))
                    .build();
            
            log.info("Información obtenida para {}: {}", symbol, overview.getName());
            return overview;
            
        } catch (Exception e) {
            log.error("Error obteniendo información de empresa para {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Obtiene solo el precio actual de un stock (método ligero)
     */
    @Cacheable(value = "stockPrices", key = "#symbol")
    public BigDecimal getStockPrice(String symbol) {
        StockQuote quote = getQuote(symbol);
        return quote != null ? quote.getPrice() : null;
    }

    // ===================================================================
    // MÉTODOS AUXILIARES
    // ===================================================================

    /**
     * Parsea un porcentaje eliminando el símbolo %
     */
    private BigDecimal parsePercentage(String percentStr) {
        if (percentStr == null || percentStr.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String cleaned = percentStr.replace("%", "").trim();
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Parsea un BigDecimal de forma segura
     */
    private BigDecimal parseBigDecimal(JsonNode node) {
        if (node == null || node.isNull() || node.asText().equals("None")) {
            return null;
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

package com.inversionlibre.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inversionlibre.backend.dto.stock.FinnhubCompanyProfile;
import com.inversionlibre.backend.dto.stock.FinnhubQuote;
import com.inversionlibre.backend.dto.stock.FinnhubSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para interactuar con Finnhub API
 * Documentación: https://finnhub.io/docs/api
 */
@Slf4j
@Service
public class FinnhubService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${finnhub.api.key}")
    private String apiKey;
    
    @Value("${finnhub.api.base-url}")
    private String baseUrl;

    public FinnhubService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Buscar símbolos de acciones
     * Endpoint: GET /search?q={query}
     */
    @Cacheable(value = "finnhubSearch", key = "#query")
    public List<FinnhubSearchResult> searchSymbols(String query) {
        log.info("Buscando símbolos en Finnhub para: {}", query);
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                    .queryParam("q", query)
                    .queryParam("token", apiKey)
                    .toUriString();

            log.debug("Finnhub search URL: {}", url.replace(apiKey, "***"));

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                
                // Check for rate limit or error
                if (root.has("error")) {
                    log.warn("Finnhub API error: {}", root.get("error").asText());
                    return new ArrayList<>();
                }
                
                JsonNode resultsNode = root.get("result");
                if (resultsNode != null && resultsNode.isArray()) {
                    log.debug("Finnhub devolvió {} resultados sin filtrar", resultsNode.size());
                    List<FinnhubSearchResult> results = new ArrayList<>();
                    int filteredOut = 0;
                    for (JsonNode node : resultsNode) {
                        FinnhubSearchResult result = objectMapper.treeToValue(node, FinnhubSearchResult.class);
                        // Include all stocks from worldwide markets
                        // Filter out only crypto and forex
                        String type = result.getType();
                        if (type != null && 
                            !type.toLowerCase().contains("crypto") && 
                            !type.toLowerCase().contains("forex")) {
                            results.add(result);
                            log.debug("Incluido: {} - {} ({})", result.getSymbol(), result.getDescription(), type);
                        } else {
                            filteredOut++;
                            log.debug("Filtrado: {} - {} ({})", result.getSymbol(), result.getDescription(), type);
                        }
                    }
                    log.info("Finnhub encontró {} resultados para '{}' ({} filtrados)", results.size(), query, filteredOut);
                    return results;
                }
            }
            
            return new ArrayList<>();
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Finnhub rate limit alcanzado");
            } else {
                log.error("Error HTTP al buscar en Finnhub: {}", e.getMessage());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error al buscar símbolos en Finnhub", e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtener cotización en tiempo real
     * Endpoint: GET /quote?symbol={symbol}
     */
    @Cacheable(value = "finnhubQuote", key = "#symbol")
    public FinnhubQuote getQuote(String symbol) {
        log.info("Obteniendo cotización de Finnhub para: {}", symbol);
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/quote")
                    .queryParam("symbol", symbol)
                    .queryParam("token", apiKey)
                    .toUriString();

            log.debug("Finnhub quote URL: {}", url.replace(apiKey, "***"));

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                
                // Check if quote has valid data (c > 0 means we got a price)
                if (root.has("c") && root.get("c").asDouble() > 0) {
                    FinnhubQuote quote = objectMapper.treeToValue(root, FinnhubQuote.class);
                    log.info("Cotización de Finnhub obtenida para {}: ${}", symbol, quote.getCurrentPrice());
                    return quote;
                } else {
                    log.warn("Finnhub no tiene datos para el símbolo: {}", symbol);
                    return null;
                }
            }
            
            return null;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Finnhub rate limit alcanzado");
            } else {
                log.error("Error HTTP al obtener cotización de Finnhub: {}", e.getMessage());
            }
            return null;
        } catch (Exception e) {
            log.error("Error al obtener cotización de Finnhub", e);
            return null;
        }
    }

    /**
     * Obtener perfil de compañía
     * Endpoint: GET /stock/profile2?symbol={symbol}
     */
    @Cacheable(value = "finnhubProfile", key = "#symbol")
    public FinnhubCompanyProfile getCompanyProfile(String symbol) {
        log.info("Obteniendo perfil de compañía de Finnhub para: {}", symbol);
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/stock/profile2")
                    .queryParam("symbol", symbol)
                    .queryParam("token", apiKey)
                    .toUriString();

            log.debug("Finnhub profile URL: {}", url.replace(apiKey, "***"));

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                
                // Check if we got valid company data
                if (root.has("name") && !root.get("name").asText().isEmpty()) {
                    FinnhubCompanyProfile profile = objectMapper.treeToValue(root, FinnhubCompanyProfile.class);
                    log.info("Perfil de compañía obtenido de Finnhub para {}: {}", symbol, profile.getName());
                    return profile;
                } else {
                    log.warn("Finnhub no tiene perfil para el símbolo: {}", symbol);
                    return null;
                }
            }
            
            return null;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Finnhub rate limit alcanzado");
            } else {
                log.error("Error HTTP al obtener perfil de Finnhub: {}", e.getMessage());
            }
            return null;
        } catch (Exception e) {
            log.error("Error al obtener perfil de compañía de Finnhub", e);
            return null;
        }
    }

    /**
     * Obtener recomendaciones de analistas
     * Endpoint: GET /stock/recommendation?symbol={symbol}
     */
    @Cacheable(value = "finnhubRecommendations", key = "#symbol")
    public JsonNode getRecommendationTrends(String symbol) {
        log.info("Obteniendo recomendaciones de Finnhub para: {}", symbol);
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/stock/recommendation")
                    .queryParam("symbol", symbol)
                    .queryParam("token", apiKey)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            }
            return null;
        } catch (Exception e) {
            log.error("Error al obtener recomendaciones de Finnhub", e);
            return null;
        }
    }

    /**
     * Obtener noticias de la compañía
     * Endpoint: GET /company-news?symbol={symbol}&from=...&to=...
     */
    @Cacheable(value = "finnhubNews", key = "#symbol")
    public JsonNode getCompanyNews(String symbol) {
        log.info("Obteniendo noticias de Finnhub para: {}", symbol);
        try {
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDate monthAgo = now.minusMonths(1);
            
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/company-news")
                    .queryParam("symbol", symbol)
                    .queryParam("from", monthAgo.toString())
                    .queryParam("to", now.toString())
                    .queryParam("token", apiKey)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readTree(response.getBody());
            }
            return null;
        } catch (Exception e) {
            log.error("Error al obtener noticias de Finnhub", e);
            return null;
        }
    }

    /**
     * Obtener solo el precio actual
     */
    public Double getStockPrice(String symbol) {
        FinnhubQuote quote = getQuote(symbol);
        return quote != null ? quote.getCurrentPrice() : null;
    }
}

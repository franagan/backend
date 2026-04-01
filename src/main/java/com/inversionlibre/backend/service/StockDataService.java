package com.inversionlibre.backend.service;

import com.inversionlibre.backend.dto.stock.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio unificado para datos de acciones que combina múltiples proveedores
 * con fallback automático.
 * 
 * Estrategia:
 * 1. Intenta Finnhub primero (60 req/min)
 * 2. Si falla, usa Alpha Vantage como fallback (25 req/día)
 * 3. Normaliza las respuestas a nuestros DTOs estándar
 */
@Slf4j
@Service
public class StockDataService {

    private final FinnhubService finnhubService;
    private final AlphaVantageService alphaVantageService;

    // Modo de simulación para evitar fallos si se agotan los tokens de las APIs (útil para TFG)
    private final boolean simulationMode = true; 

    public StockDataService(FinnhubService finnhubService, AlphaVantageService alphaVantageService) {
        this.finnhubService = finnhubService;
        this.alphaVantageService = alphaVantageService;
    }

    /**
     * Buscar símbolos de acciones usando múltiples proveedores
     */
    public List<StockSearchResult> searchStocks(String keywords) {
        log.info("Buscando stocks con keywords: {}", keywords);
        
        // Try Finnhub first
        try {
            List<FinnhubSearchResult> finnhubResults = finnhubService.searchSymbols(keywords);
            if (finnhubResults != null && !finnhubResults.isEmpty()) {
                log.info("Usando resultados de Finnhub: {} resultados", finnhubResults.size());
                return convertFinnhubSearchResults(finnhubResults);
            }
        } catch (Exception e) {
            log.warn("Finnhub search failed, trying Alpha Vantage: {}", e.getMessage());
        }

        // Fallback to Alpha Vantage
        try {
            List<StockSearchResult> alphaResults = alphaVantageService.searchSymbols(keywords);
            if (alphaResults != null && !alphaResults.isEmpty()) {
                log.info("Usando resultados de Alpha Vantage: {} resultados", alphaResults.size());
                return alphaResults;
            }
        } catch (Exception e) {
            log.error("Alpha Vantage search también falló: {}", e.getMessage());
        }

        log.warn("No se pudieron obtener resultados de ningún proveedor");
        return new ArrayList<>();
    }
    
    /**
     * Verificar si un símbolo tiene datos de precio disponibles
     */
    private boolean checkPriceAvailability(String symbol) {
        try {
            // Try to get price from Finnhub first
            Double finnhubPrice = finnhubService.getStockPrice(symbol);
            if (finnhubPrice != null && finnhubPrice > 0) {
                return true;
            }
            
            // Try Alpha Vantage as fallback
            BigDecimal alphaPrice = alphaVantageService.getStockPrice(symbol);
            return alphaPrice != null && alphaPrice.compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception e) {
            log.debug("No price data available for {}: {}", symbol, e.getMessage());
            return false;
        }
    }

    /**
     * Obtener cotización de una acción
     */
    public StockQuote getStockQuote(String symbol) {
        log.info("Obteniendo cotización para: {}", symbol);
        
        // Try Finnhub first
        try {
            FinnhubQuote finnhubQuote = finnhubService.getQuote(symbol);
            if (finnhubQuote != null && finnhubQuote.getCurrentPrice() != null && finnhubQuote.getCurrentPrice() > 0) {
                log.info("Usando cotización de Finnhub para {}", symbol);
                return convertFinnhubQuote(symbol, finnhubQuote);
            }
        } catch (Exception e) {
            log.warn("Finnhub quote failed for {}, trying Alpha Vantage: {}", symbol, e.getMessage());
        }

        // Fallback to Alpha Vantage
        try {
            StockQuote alphaQuote = alphaVantageService.getQuote(symbol);
            if (alphaQuote != null) {
                log.info("Usando cotización de Alpha Vantage para {}", symbol);
                return alphaQuote;
            }
        } catch (Exception e) {
            log.error("Alpha Vantage quote también falló para {}: {}", symbol, e.getMessage());
        }

        // Modo Simulación as last resort
        if (simulationMode) {
            log.info("SIMULACION: Generando cotización ficticia para {}", symbol);
            return generateMockQuote(symbol);
        }

        log.warn("No se pudo obtener cotización de ningún proveedor para {}", symbol);
        return null;
    }

    /**
     * Obtener información de la compañía
     */
    public CompanyOverview getCompanyOverview(String symbol) {
        log.info("Obteniendo información de compañía para: {}", symbol);
        
        // Try Finnhub first
        try {
            FinnhubCompanyProfile finnhubProfile = finnhubService.getCompanyProfile(symbol);
            if (finnhubProfile != null && finnhubProfile.getName() != null) {
                log.info("Usando perfil de Finnhub para {}", symbol);
                return convertFinnhubProfile(symbol, finnhubProfile);
            }
        } catch (Exception e) {
            log.warn("Finnhub profile failed for {}, trying Alpha Vantage: {}", symbol, e.getMessage());
        }

        // Fallback to Alpha Vantage
        try {
            CompanyOverview alphaOverview = alphaVantageService.getCompanyOverview(symbol);
            if (alphaOverview != null) {
                log.info("Usando overview de Alpha Vantage para {}", symbol);
                return alphaOverview;
            }
        } catch (Exception e) {
            log.error("Alpha Vantage overview también falló para {}: {}", symbol, e.getMessage());
        }

        log.warn("No se pudo obtener información de compañía de ningún proveedor para {}", symbol);
        return null;
    }

    /**
     * Obtener solo el precio actual
     */
    public BigDecimal getStockPrice(String symbol) {
        log.info("Obteniendo precio para: {}", symbol);
        
        // Try Finnhub first
        try {
            Double finnhubPrice = finnhubService.getStockPrice(symbol);
            if (finnhubPrice != null && finnhubPrice > 0) {
                log.info("Usando precio de Finnhub para {}: ${}", symbol, finnhubPrice);
                return BigDecimal.valueOf(finnhubPrice);
            }
        } catch (Exception e) {
            log.warn("Finnhub price failed for {}, trying Alpha Vantage: {}", symbol, e.getMessage());
        }

        // Fallback to Alpha Vantage
        try {
            BigDecimal alphaPrice = alphaVantageService.getStockPrice(symbol);
            if (alphaPrice != null) {
                log.info("Usando precio de Alpha Vantage para {}: ${}", symbol, alphaPrice);
                return alphaPrice;
            }
        } catch (Exception e) {
            log.error("Alpha Vantage price también falló para {}: {}", symbol, e.getMessage());
        }

        // Modo Simulación as last resort
        if (simulationMode) {
            log.info("SIMULACION: Generando precio ficticio para {}", symbol);
            BigDecimal mockPrice = generateMockPrice(symbol);
            return mockPrice;
        }

        log.warn("No se pudo obtener precio de ningún proveedor para {}", symbol);
        return null;
    }

    // ========== Métodos de conversión ==========

    /**
     * Convertir resultados de búsqueda de Finnhub a nuestro formato estándar
     */
    private List<StockSearchResult> convertFinnhubSearchResults(List<FinnhubSearchResult> finnhubResults) {
        List<StockSearchResult> results = new ArrayList<>();
        for (FinnhubSearchResult finnhub : finnhubResults) {
            StockSearchResult result = new StockSearchResult();
            result.setSymbol(finnhub.getSymbol());
            result.setName(finnhub.getDescription());
            result.setType(finnhub.getType());
            // Use displaySymbol to determine region/exchange
            String displaySymbol = finnhub.getDisplaySymbol();
            if (displaySymbol != null && displaySymbol.contains(":")) {
                // Format is usually "EXCHANGE:SYMBOL"
                String exchange = displaySymbol.split(":")[0];
                result.setRegion(exchange); // e.g., "NASDAQ", "LSE", "BME", etc.
            } else {
                result.setRegion("US"); // Default to US if no exchange info
            }
            results.add(result);
        }
        return results;
    }

    /**
     * Convertir cotización de Finnhub a nuestro formato estándar
     */
    private StockQuote convertFinnhubQuote(String symbol, FinnhubQuote finnhub) {
        StockQuote quote = new StockQuote();
        quote.setSymbol(symbol);
        quote.setPrice(finnhub.getCurrentPrice() != null ? BigDecimal.valueOf(finnhub.getCurrentPrice()) : null);
        quote.setChange(finnhub.getChange() != null ? BigDecimal.valueOf(finnhub.getChange()) : null);
        quote.setChangePercent(finnhub.getPercentChange() != null ? BigDecimal.valueOf(finnhub.getPercentChange()) : null);
        quote.setHigh(finnhub.getHighPrice() != null ? BigDecimal.valueOf(finnhub.getHighPrice()) : null);
        quote.setLow(finnhub.getLowPrice() != null ? BigDecimal.valueOf(finnhub.getLowPrice()) : null);
        quote.setOpen(finnhub.getOpenPrice() != null ? BigDecimal.valueOf(finnhub.getOpenPrice()) : null);
        quote.setPreviousClose(finnhub.getPreviousClose() != null ? BigDecimal.valueOf(finnhub.getPreviousClose()) : null);
        quote.setVolume(finnhub.getTimestamp() != null ? finnhub.getTimestamp() : 0L);
        return quote;
    }

    /**
     * Convertir perfil de Finnhub a nuestro formato estándar
     */
    private CompanyOverview convertFinnhubProfile(String symbol, FinnhubCompanyProfile finnhub) {
        CompanyOverview overview = new CompanyOverview();
        overview.setSymbol(symbol);
        overview.setName(finnhub.getName());
        overview.setDescription("Company in " + (finnhub.getFinnhubIndustry() != null ? finnhub.getFinnhubIndustry() : "various industries"));
        overview.setCountry(finnhub.getCountry());
        overview.setCurrency(finnhub.getCurrency());
        overview.setSector(finnhub.getFinnhubIndustry());
        overview.setIndustry(finnhub.getFinnhubIndustry());
        
        // Market cap is in millions from Finnhub, convert to actual value
        if (finnhub.getMarketCapitalization() != null) {
            overview.setMarketCap(BigDecimal.valueOf(finnhub.getMarketCapitalization() * 1_000_000));
        }
        
        // Finnhub doesn't provide these, set to null
        overview.setPeRatio(null);
        overview.setEps(null);
        overview.setDividendYield(null);
        overview.setBeta(null);
        overview.setWeek52High(null);
        overview.setWeek52Low(null);
        
        return overview;
    }

    // ========== MÉTODOS DE SIMULACIÓN (PARA TFG) ==========

    private BigDecimal generateMockPrice(String symbol) {
        // Generar un precio basado en el hash del simbolo para que sea estable
        int hash = Math.abs(symbol.hashCode());
        double basePrice = (hash % 200) + 50.0; // Precio entre 50 y 250
        
        // Añadir una pequeña variacion basada en el minuto actual para que "se mueva"
        double variation = (LocalDateTime.now().getMinute() % 10) / 100.0;
        return BigDecimal.valueOf(basePrice + variation).setScale(2, RoundingMode.HALF_UP);
    }

    private StockQuote generateMockQuote(String symbol) {
        BigDecimal price = generateMockPrice(symbol);
        StockQuote quote = new StockQuote();
        quote.setSymbol(symbol);
        quote.setPrice(price);
        quote.setOpen(price.subtract(BigDecimal.valueOf(1.5)));
        quote.setHigh(price.add(BigDecimal.valueOf(2.0)));
        quote.setLow(price.subtract(BigDecimal.valueOf(2.5)));
        quote.setPreviousClose(price.subtract(BigDecimal.valueOf(0.5)));
        quote.setChange(BigDecimal.valueOf(0.5));
        quote.setChangePercent(BigDecimal.valueOf(0.25));
        quote.setVolume(1000000L);
        quote.setLastUpdated(LocalDateTime.now());
        return quote;
    }
}

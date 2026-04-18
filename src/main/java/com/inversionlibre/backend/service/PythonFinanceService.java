package com.inversionlibre.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class PythonFinanceService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.service.url:http://localhost:8001}")
    private String pythonApiUrl;

    public JsonNode getStockData(String symbol) {
        try {
            log.info("Consultando datos de Yahoo Finance vía Python para: {}", symbol);
            String url = pythonApiUrl + "/api/finance/stock/" + symbol;
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception e) {
            log.warn("Error consultando Yahoo Finance (Python): {}", e.getMessage());
            return null;
        }
    }
}

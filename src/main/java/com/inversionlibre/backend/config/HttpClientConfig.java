package com.inversionlibre.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración para beans de HTTP clients
 */
@Configuration
public class HttpClientConfig {

    /**
     * Bean de RestTemplate para hacer llamadas HTTP a APIs externas
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

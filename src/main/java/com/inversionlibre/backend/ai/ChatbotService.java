package com.inversionlibre.backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final RestClient restClient;
    private final String pythonApiUrl;

    public ChatbotService(
            RestClient.Builder restClientBuilder,
            @Value("${ai.service.url:http://localhost:8001}") String pythonApiUrl) {
        java.net.http.HttpClient jdkHttpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build();

        this.restClient = restClientBuilder
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(jdkHttpClient))
                .build();
        this.pythonApiUrl = pythonApiUrl;
    }

    /**
     * Procesa una pregunta del usuario enviándola al microservicio de Python (API Gateway pattern):
     * 
     * @param userMessage La pregunta o mensaje del usuario
     * @param context Contexto financiero del usuario para personalización
     * @return La respuesta generada por la IA
     */
    public String askQuestion(String userMessage, String context) {
        log.info("Recibida pregunta para el chatbot, redirigiendo a microservicio Python: {}", userMessage);

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> response = restClient.post()
                    .uri(pythonApiUrl + "/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                        "message", userMessage,
                        "user_context", context != null ? context : ""
                    ))
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("response")) {
                log.info("Respuesta obtenida correctamente desde el microservicio Python.");
                return response.get("response");
            } else {
                log.warn("El microservicio Python devolvió una respuesta vacía o sin el formato esperado.");
                return "Lo siento, ha habido un problema al procesar tu consulta (respuesta vacía).";
            }
        } catch (Exception e) {
            log.error("Error de comunicación con el microservicio de IA Python: ", e);
            throw new RuntimeException("Error al comunicarse con el asistente de IA.", e);
        }
    }
}

package com.inversionlibre.backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatbotService chatbotService;

    public ChatController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chat")
    public ResponseEntity<java.util.Map<String, String>> chat(@RequestBody java.util.Map<String, String> request) {
        try {
            log.info("Peticion de chat recibida en API Gateway");
            String message = request.getOrDefault("message", "");
            if (message.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "El campo 'message' es requerido."));
            }
            String response = chatbotService.askQuestion(message);
            return ResponseEntity.ok(java.util.Map.of("response", response));
        } catch (Exception e) {
            log.error("Error procesando la petición de chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Error processing your request."));
        }
    }
}

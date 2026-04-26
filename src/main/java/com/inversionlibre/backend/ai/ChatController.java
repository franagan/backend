package com.inversionlibre.backend.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatbotService chatbotService;
    private final com.inversionlibre.backend.service.PortfolioService portfolioService;

    public ChatController(ChatbotService chatbotService, com.inversionlibre.backend.service.PortfolioService portfolioService) {
        this.chatbotService = chatbotService;
        this.portfolioService = portfolioService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, String> request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.inversionlibre.backend.model.User user) {
        try {
            log.info("Peticion de chat recibida para usuario: {}", user != null ? user.getEmail() : "Anónimo");
            String message = request.getOrDefault("message", "");
            if (message.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "El campo 'message' es requerido."));
            }

            // Construir contexto personalizado si el usuario está autenticado
            String context = "";
            if (user != null) {
                var portfolios = portfolioService.findByUserId(user.getId());
                double totalValue = portfolios.stream().mapToDouble(p -> p.getTotalValue() != null ? p.getTotalValue().doubleValue() : 0.0).sum();
                context = String.format("El usuario se llama %s. Tiene %d carteras de inversión con un valor total de %.2f euros.", 
                    user.getFirstName(), portfolios.size(), totalValue);
            }

            String response = chatbotService.askQuestion(message, context, user != null ? user.getId() : null);
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            log.error("Error procesando la petición de chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing your request."));
        }
    }

    @GetMapping("/chat/history")
    public ResponseEntity<List<com.inversionlibre.backend.model.ChatMessage>> getHistory(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.inversionlibre.backend.model.User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(chatbotService.getChatHistory(user.getId()));
    }

    @DeleteMapping("/chat/history")
    public ResponseEntity<Void> clearHistory(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.inversionlibre.backend.model.User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        chatbotService.clearChatHistory(user.getId());
        return ResponseEntity.noContent().build();
    }
}

package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador de prueba para verificar la autenticación JWT
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@Tag(name = "Pruebas", description = "Endpoints de prueba para verificar funcionalidad")
public class TestController {

    @GetMapping("/public")
    @Operation(summary = "Endpoint público", description = "Endpoint que no requiere autenticación")
    public ResponseEntity<ApiResponse<Map<String, Object>>> publicEndpoint() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Este es un endpoint público");
        data.put("timestamp", LocalDateTime.now());
        data.put("status", "OK");
        
        return ResponseEntity.ok(ApiResponse.success("Endpoint público funcionando", data));
    }

    @GetMapping("/protected")
    @Operation(
        summary = "Endpoint protegido", 
        description = "Endpoint que requiere autenticación JWT",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> protectedEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Este es un endpoint protegido");
        data.put("timestamp", LocalDateTime.now());
        data.put("authenticated", authentication != null && authentication.isAuthenticated());
        data.put("username", authentication != null ? authentication.getName() : "No autenticado");
        data.put("authorities", authentication != null ? authentication.getAuthorities() : "Sin autoridades");
        
        log.info("Acceso a endpoint protegido por usuario: {}", authentication != null ? authentication.getName() : "No autenticado");
        
        return ResponseEntity.ok(ApiResponse.success("Endpoint protegido funcionando", data));
    }

    @GetMapping("/admin")
    @Operation(
        summary = "Endpoint de administrador", 
        description = "Endpoint que requiere rol de administrador",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Este es un endpoint de administrador");
        data.put("timestamp", LocalDateTime.now());
        data.put("authenticated", authentication != null && authentication.isAuthenticated());
        data.put("username", authentication != null ? authentication.getName() : "No autenticado");
        data.put("authorities", authentication != null ? authentication.getAuthorities() : "Sin autoridades");
        
        log.info("Acceso a endpoint de administrador por usuario: {}", authentication != null ? authentication.getName() : "No autenticado");
        
        return ResponseEntity.ok(ApiResponse.success("Endpoint de administrador funcionando", data));
    }

    @GetMapping("/swagger-test")
    @Operation(summary = "Test Swagger", description = "Endpoint para probar que Swagger está funcionando correctamente")
    public ResponseEntity<ApiResponse<Map<String, Object>>> swaggerTest() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Swagger está funcionando correctamente");
        data.put("timestamp", LocalDateTime.now());
        data.put("swagger_ui_url", "http://localhost:8080/swagger-ui.html");
        data.put("api_docs_url", "http://localhost:8080/api-docs");
        data.put("status", "OK");
        
        return ResponseEntity.ok(ApiResponse.success("Swagger UI cargado correctamente", data));
    }
}

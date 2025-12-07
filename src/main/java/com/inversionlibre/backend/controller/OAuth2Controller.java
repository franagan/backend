package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.dto.JwtResponse;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.AuthService;
import com.inversionlibre.backend.service.UserService;
import com.inversionlibre.backend.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador para manejar OAuth2 (Google/Facebook)
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2", description = "Endpoints para autenticación OAuth2")
public class OAuth2Controller {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/success")
    @Operation(summary = "OAuth2 Success", description = "Maneja el éxito del login OAuth2")
    public ResponseEntity<ApiResponse<JwtResponse>> oauth2Success(@AuthenticationPrincipal OAuth2User principal) {
        try {
            if (principal == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No se pudo obtener la información del usuario OAuth2"));
            }

            String email = principal.getAttribute("email");
            String name = principal.getAttribute("name");
            String firstName = principal.getAttribute("given_name");
            String lastName = principal.getAttribute("family_name");

            if (email == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No se pudo obtener el email del usuario"));
            }

            // Buscar o crear usuario
            Optional<User> existingUser = userService.findByEmail(email);
            User user;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                log.info("Usuario OAuth2 existente encontrado: {}", email);
            } else {
                // Crear nuevo usuario
                user = User.builder()
                    .email(email)
                    .firstName(firstName != null ? firstName : name)
                    .lastName(lastName != null ? lastName : "")
                    .role(User.Role.USER)
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .build();
                
                user = userService.save(user);
                log.info("Nuevo usuario OAuth2 creado: {}", email);
            }

            // Generar JWT
            String token = jwtUtil.generateToken(user);
            
            JwtResponse response = JwtResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

            return ResponseEntity.ok(ApiResponse.success("Login OAuth2 exitoso", response));

        } catch (Exception e) {
            log.error("Error en OAuth2 success", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error en el login OAuth2: " + e.getMessage()));
        }
    }

    @GetMapping("/user")
    @Operation(summary = "OAuth2 User Info", description = "Obtiene información del usuario OAuth2")
    public ResponseEntity<ApiResponse<Map<String, Object>>> oauth2User(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("No hay usuario OAuth2 autenticado"));
        }

        Map<String, Object> userInfo = Map.of(
            "name", principal.getAttribute("name"),
            "email", principal.getAttribute("email"),
            "attributes", principal.getAttributes()
        );

        return ResponseEntity.ok(ApiResponse.success("Información del usuario OAuth2", userInfo));
    }
}
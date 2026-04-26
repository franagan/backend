package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.dto.JwtRequest;
import com.inversionlibre.backend.dto.JwtResponse;
import com.inversionlibre.backend.dto.RegisterRequest;
import com.inversionlibre.backend.dto.UserResponse;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.AuthService;
import com.inversionlibre.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para autenticación y registro de usuarios
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para autenticación y registro de usuarios")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final com.inversionlibre.backend.util.JwtUtil jwtUtil;
    private final com.inversionlibre.backend.service.EmailService emailService;

    @PostMapping("/google")
    @Operation(summary = "Login con Google", description = "Autentica con token de Google")
    public ResponseEntity<ApiResponse<JwtResponse>> googleLogin(@RequestBody com.inversionlibre.backend.dto.TokenRequest request) {
        try {
            // Verify token with Google
            String googleUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + request.getToken();
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, Object> googleClaims = restTemplate.getForObject(googleUrl, java.util.Map.class);

            if (googleClaims == null || googleClaims.get("email") == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Token de Google inválido"));
            }

            String email = (String) googleClaims.get("email");
            String name = (String) googleClaims.get("name");
            String firstName = (String) googleClaims.get("given_name");
            String lastName = (String) googleClaims.get("family_name");

            // Find or create user
            User user = userService.findByEmail(email).orElseGet(() -> {
                User newUser = User.builder()
                        .email(email)
                        .firstName(firstName != null ? firstName : name)
                        .lastName(lastName != null ? lastName : "")
                        .password(java.util.UUID.randomUUID().toString()) // Random password for Google users
                        .role(User.Role.USER)
                        .enabled(true)
                        .accountNonExpired(true)
                        .accountNonLocked(true)
                        .credentialsNonExpired(true)
                        .build();
                User savedUser = userService.save(newUser);
                
                // Enviar email de bienvenida para usuario Google nuevo
                try {
                    emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName());
                } catch (Exception e) {
                    log.error("No se pudo enviar email de bienvenida Google a {}: {}", savedUser.getEmail(), e.getMessage());
                }
                
                return savedUser;
            });

            // Generate JWT
            String token = jwtUtil.generateToken(user);
            
            JwtResponse response = JwtResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .issuedAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plusHours(24))
                .build();

            return ResponseEntity.ok(ApiResponse.success("Login con Google exitoso", response));

        } catch (Exception e) {
            log.error("Error en Google login", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Error validando token de Google"));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario y devuelve un JWT token")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Login exitoso",
            content = @Content(schema = @Schema(implementation = JwtResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "Credenciales inválidas",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Datos de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody JwtRequest request) {
        try {
            log.info("Intento de login para usuario: {}", request.getEmail());
            
            JwtResponse response = authService.authenticate(request);
            
            log.info("Login exitoso para usuario: {}", request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Login exitoso", response));
            
        } catch (Exception e) {
            log.error("Error en login para usuario: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Credenciales inválidas o cuenta deshabilitada"));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Registra un nuevo usuario en el sistema")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", 
            description = "Usuario registrado exitosamente",
            content = @Content(schema = @Schema(implementation = JwtResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Datos de entrada inválidos o usuario ya existe",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<JwtResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("Intento de registro para usuario: {}", request.getEmail());
            
            JwtResponse response = authService.register(request);
            
            log.info("Registro exitoso para usuario: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Usuario registrado exitosamente", response));
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación en registro: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error en registro para usuario: {}", request.getEmail(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error durante el registro: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token", description = "Refresca un JWT token válido")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Token refrescado exitosamente",
            content = @Content(schema = @Schema(implementation = JwtResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "Token inválido o expirado",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            log.info("Intento de refresh token");
            
            JwtResponse response = authService.refreshToken(token);
            
            log.info("Token refrescado exitosamente");
            return ResponseEntity.ok(ApiResponse.success("Token refrescado exitosamente", response));
            
        } catch (Exception e) {
            log.error("Error refrescando token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token inválido o expirado"));
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "Validar token", description = "Valida si un JWT token es válido")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Token válido",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "Token inválido",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            boolean isValid = authService.validateToken(token);
            
            if (isValid) {
                return ResponseEntity.ok(ApiResponse.success("Token válido", true));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Token inválido", "INVALID_TOKEN"));
            }
            
        } catch (Exception e) {
            log.error("Error validando token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token inválido", "INVALID_TOKEN"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Cierra la sesión del usuario (el token debe ser invalidado en el cliente)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Sesión cerrada exitosamente",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<String>> logout() {
        log.info("Usuario cerró sesión");
        return ResponseEntity.ok(ApiResponse.success("Sesión cerrada exitosamente", null));
    }

    @GetMapping("/me")
    @Operation(
        summary = "Obtener perfil actual", 
        description = "Obtiene la información del usuario autenticado",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Perfil obtenido exitosamente",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Intento de acceso a /api/auth/me sin autenticación");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("No autenticado"));
            }
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            
            log.debug("Obteniendo perfil para usuario: {}", email);
            
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
            
            UserResponse userResponse = UserResponse.fromUser(user);
            
            log.info("Perfil obtenido exitosamente para usuario: {}", email);
            return ResponseEntity.ok(ApiResponse.success("Perfil obtenido exitosamente", userResponse));
            
        } catch (ClassCastException e) {
            log.error("Error obteniendo usuario del contexto de seguridad", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Error de autenticación"));
        } catch (Exception e) {
            log.error("Error obteniendo perfil del usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener el perfil: " + e.getMessage()));
        }
    }

    @DeleteMapping("/me")
    @Operation(
        summary = "Eliminar cuenta", 
        description = "Elimina permanentemente la cuenta del usuario autenticado y todos sus datos",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            userService.deleteById(user.getId());
            
            log.info("Cuenta eliminada permanentemente: {}", email);
            return ResponseEntity.ok(ApiResponse.success("Cuenta eliminada exitosamente", null));
            
        } catch (Exception e) {
            log.error("Error eliminando cuenta", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al eliminar la cuenta"));
        }
    }
}

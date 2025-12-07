package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.dto.UpdatePreferencesRequest;
import com.inversionlibre.backend.dto.UpdateUserRequest;
import com.inversionlibre.backend.dto.UserResponse;
import com.inversionlibre.backend.model.User;
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
 * Controlador para gestión de usuarios
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Endpoints para gestión de usuarios")
public class UserController {

    private final UserService userService;

    /**
     * Obtiene el usuario autenticado desde el contexto de seguridad
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Usuario no autenticado");
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        
        return userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
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
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        try {
            User user = getCurrentUser();
            UserResponse userResponse = UserResponse.fromUser(user);
            
            log.info("Perfil obtenido exitosamente para usuario: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Perfil obtenido exitosamente", userResponse));
            
        } catch (Exception e) {
            log.error("Error obteniendo perfil del usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener el perfil: " + e.getMessage()));
        }
    }

    @PutMapping("/me")
    @Operation(
        summary = "Actualizar perfil", 
        description = "Actualiza la información del usuario autenticado",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Perfil actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Datos de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            User currentUser = getCurrentUser();
            
            log.info("Actualizando perfil para usuario: {}", currentUser.getEmail());
            
            User updatedUser = userService.updateUser(
                currentUser.getId(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhone()
            );
            
            UserResponse userResponse = UserResponse.fromUser(updatedUser);
            
            log.info("Perfil actualizado exitosamente para usuario: {}", updatedUser.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Perfil actualizado exitosamente", userResponse));
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar perfil: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error actualizando perfil del usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al actualizar el perfil: " + e.getMessage()));
        }
    }

    @GetMapping("/me/preferences")
    @Operation(
        summary = "Obtener preferencias", 
        description = "Obtiene las preferencias del usuario autenticado",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Preferencias obtenidas exitosamente",
            content = @Content(schema = @Schema(implementation = User.UserPreferences.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<User.UserPreferences>> getCurrentUserPreferences() {
        try {
            User user = getCurrentUser();
            
            // Si no tiene preferencias, devolver preferencias por defecto
            User.UserPreferences preferences = user.getPreferences();
            if (preferences == null) {
                preferences = User.UserPreferences.builder().build();
            }
            
            log.info("Preferencias obtenidas exitosamente para usuario: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Preferencias obtenidas exitosamente", preferences));
            
        } catch (Exception e) {
            log.error("Error obteniendo preferencias del usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener las preferencias: " + e.getMessage()));
        }
    }

    @PutMapping("/me/preferences")
    @Operation(
        summary = "Actualizar preferencias", 
        description = "Actualiza las preferencias del usuario autenticado",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Preferencias actualizadas exitosamente",
            content = @Content(schema = @Schema(implementation = User.UserPreferences.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Datos de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "No autenticado",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<User.UserPreferences>> updateCurrentUserPreferences(
            @Valid @RequestBody UpdatePreferencesRequest request) {
        try {
            User currentUser = getCurrentUser();
            
            log.info("Actualizando preferencias para usuario: {}", currentUser.getEmail());
            
            // Obtener preferencias actuales o crear nuevas
            User.UserPreferences currentPreferences = currentUser.getPreferences();
            if (currentPreferences == null) {
                currentPreferences = User.UserPreferences.builder().build();
            }
            
            // Actualizar solo los campos proporcionados
            User.UserPreferences updatedPreferences = User.UserPreferences.builder()
                .currency(request.getCurrency() != null ? request.getCurrency() : currentPreferences.getCurrency())
                .language(request.getLanguage() != null ? request.getLanguage() : currentPreferences.getLanguage())
                .emailNotifications(request.getEmailNotifications() != null 
                    ? request.getEmailNotifications() 
                    : currentPreferences.getEmailNotifications())
                .pushNotifications(request.getPushNotifications() != null 
                    ? request.getPushNotifications() 
                    : currentPreferences.getPushNotifications())
                .marketingEmails(request.getMarketingEmails() != null 
                    ? request.getMarketingEmails() 
                    : currentPreferences.getMarketingEmails())
                .build();
            
            User updatedUser = userService.updatePreferences(currentUser.getId(), updatedPreferences);
            
            log.info("Preferencias actualizadas exitosamente para usuario: {}", updatedUser.getEmail());
            return ResponseEntity.ok(ApiResponse.success(
                "Preferencias actualizadas exitosamente", 
                updatedUser.getPreferences()
            ));
            
        } catch (Exception e) {
            log.error("Error actualizando preferencias del usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al actualizar las preferencias: " + e.getMessage()));
        }
    }
}



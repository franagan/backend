package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Endpoints para la administración de usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Listar todos los usuarios", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        try {
            List<User> users = userService.findAllUsers();
            return ResponseEntity.ok(ApiResponse.success("Usuarios listados exitosamente", users));
        } catch (Exception e) {
            log.error("Error al listar usuarios", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Error al listar usuarios"));
        }
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Cambiar rol de usuario", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<User>> updateUserRole(
            @PathVariable String id,
            @RequestParam User.Role role) {
        try {
            User updatedUser = userService.updateUserRole(id, role);
            return ResponseEntity.ok(ApiResponse.success("Rol actualizado", updatedUser));
        } catch (Exception e) {
            log.error("Error actualizando rol", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Error al actualizar rol"));
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Activar/Desactivar usuario", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<User>> toggleUserStatus(
            @PathVariable String id,
            @RequestParam boolean enabled) {
        try {
            User updatedUser = userService.toggleUserStatus(id, enabled);
            return ResponseEntity.ok(ApiResponse.success("Estado actualizado", updatedUser));
        } catch (Exception e) {
            log.error("Error actualizando estado", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Error al actualizar estado"));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        try {
            userService.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Usuario eliminado", null));
        } catch (Exception e) {
            log.error("Error eliminando usuario", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Error al eliminar usuario"));
        }
    }
}

package com.inversionlibre.backend.service;

import com.inversionlibre.backend.dto.JwtRequest;
import com.inversionlibre.backend.dto.JwtResponse;
import com.inversionlibre.backend.dto.RegisterRequest;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Servicio para autenticación y registro de usuarios
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public JwtResponse authenticate(JwtRequest request) {
        log.info("Autenticando usuario: {}", request.getEmail());
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        
        User user = userService.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return JwtResponse.builder()
            .token(token)
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole().name())
            .issuedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(24))
            .build();
    }

    public JwtResponse register(RegisterRequest request) {
        log.info("Registrando nuevo usuario: {}", request.getEmail());
        
        // Validar que las contraseñas coincidan
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        
        // Validar términos y condiciones
        if (!request.isAcceptTerms()) {
            throw new IllegalArgumentException("Debe aceptar los términos y condiciones");
        }
        
        // Verificar si el usuario ya existe
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El usuario ya existe");
        }
        
        // Crear nuevo usuario
        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .role(User.Role.USER)
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();
        
        User savedUser = userService.save(user);
        
        // Enviar email de bienvenida
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName());
        } catch (Exception e) {
            log.error("No se pudo enviar el email de bienvenida a {}: {}", savedUser.getEmail(), e.getMessage());
        }
        
        // Generar token
        String token = jwtUtil.generateToken(savedUser);
        
        return JwtResponse.builder()
            .token(token)
            .email(savedUser.getEmail())
            .firstName(savedUser.getFirstName())
            .lastName(savedUser.getLastName())
            .role(savedUser.getRole().name())
            .issuedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(24))
            .build();
    }

    public JwtResponse refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Token inválido o expirado");
        }
        
        String email = jwtUtil.extractUsername(token);
        User user = userService.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        String newToken = jwtUtil.generateToken(user);
        
        return JwtResponse.builder()
            .token(newToken)
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole().name())
            .issuedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(24))
            .build();
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
}

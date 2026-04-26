package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio para gestión de usuarios
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final com.inversionlibre.backend.repository.PortfolioRepository portfolioRepository;
    private final com.inversionlibre.backend.repository.ChatMessageRepository chatMessageRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Cargando usuario por email: {}", email);
        
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    public Optional<User> findByEmail(String email) {
        log.debug("Buscando usuario por email: {}", email);
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(String id) {
        log.debug("Buscando usuario por ID: {}", id);
        return userRepository.findById(id);
    }

    public boolean existsById(String userId) {
        return userRepository.existsById(userId);
    }

    @Transactional
    public User save(User user) {
        log.info("Guardando usuario: {}", user.getEmail());
        User savedUser = userRepository.save(user);
        log.info("Usuario guardado exitosamente con ID: {}", savedUser.getId());
        return savedUser;
    }

    @Transactional
    public void deleteById(String id) {
        log.info("Iniciando borrado en cascada para usuario con ID: {}", id);
        
        // 1. Borrar portfolios asociados
        portfolioRepository.deleteByUserId(id);
        log.debug("Portfolios del usuario eliminados");
        
        // 2. Borrar historial de chat
        chatMessageRepository.deleteByUserId(id);
        log.debug("Historial de chat eliminado");
        
        // 3. Borrar el usuario
        userRepository.deleteById(id);
        log.info("Usuario y todos sus datos asociados eliminados exitosamente");
    }

    @Transactional
    public void addPortfolioToUser(String userId, String portfolioId) {
        log.info("Añadiendo portfolio {} al usuario {}", portfolioId, userId);
        
        User user = findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));
        
        user.addPortfolioId(portfolioId);
        save(user);
        
        log.info("Portfolio {} añadido exitosamente al usuario {}", portfolioId, userId);
    }

    @Transactional
    public void removePortfolioFromUser(String userId, String portfolioId) {
        log.info("Eliminando portfolio {} del usuario {}", portfolioId, userId);
        
        User user = findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));
        
        user.removePortfolioId(portfolioId);
        save(user);
        
        log.info("Portfolio {} eliminado exitosamente del usuario {}", portfolioId, userId);
    }

    @Transactional
    public User updateUser(String userId, String firstName, String lastName, String phone) {
        log.info("Actualizando usuario con ID: {}", userId);
        
        User user = findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            user.setFirstName(firstName);
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            user.setLastName(lastName);
        }
        if (phone != null) {
            user.setPhone(phone.trim().isEmpty() ? null : phone);
        }
        
        User updatedUser = save(user);
        log.info("Usuario actualizado exitosamente: {}", updatedUser.getEmail());
        
        return updatedUser;
    }

    @Transactional
    public User updatePreferences(String userId, User.UserPreferences preferences) {
        log.info("Actualizando preferencias del usuario con ID: {}", userId);
        
        User user = findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));
        
        if (preferences == null) {
            preferences = User.UserPreferences.builder().build();
        }
        
        user.setPreferences(preferences);
        User updatedUser = save(user);
        
        log.info("Preferencias actualizadas exitosamente para usuario: {}", updatedUser.getEmail());
        
        return updatedUser;
    }

    // --- ADMIN METHODS --- //

    public java.util.List<User> findAllUsers() {
        log.debug("Listando todos los usuarios");
        return userRepository.findAll();
    }

    @Transactional
    public User updateUserRole(String userId, User.Role newRole) {
        log.info("Actualizando rol del usuario {} a {}", userId, newRole);
        User user = findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));
        
        user.setRole(newRole);
        return save(user);
    }

    @Transactional
    public User toggleUserStatus(String userId, boolean enabled) {
        log.info("Cambiando status del usuario {} a enabled={}", userId, enabled);
        User user = findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));
        
        user.setEnabled(enabled);
        return save(user);
    }
}
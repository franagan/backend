package com.inversionlibre.backend.repository;

import com.inversionlibre.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad User
 * Proporciona operaciones CRUD y consultas personalizadas para usuarios
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // ===================================================================
    // CONSULTAS DE AUTENTICACIÓN
    // ===================================================================

    /**
     * Busca un usuario por email (único)
     * Usado para autenticación y validación de email único
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica si existe un usuario con el email dado
     * Útil para validaciones antes de crear cuenta
     */
    boolean existsByEmail(String email);

    /**
     * Busca un usuario por email e incluye solo campos necesarios para autenticación
     */
    @Query(value = "{ 'email': ?0 }", fields = "{ 'email': 1, 'password': 1, 'role': 1, 'enabled': 1, 'accountNonExpired': 1, 'accountNonLocked': 1, 'credentialsNonExpired': 1 }")
    Optional<User> findAuthenticationDataByEmail(String email);

    // ===================================================================
    // CONSULTAS POR ESTADO Y ROL
    // ===================================================================

    /**
     * Encuentra usuarios activos (enabled = true)
     */
    List<User> findByEnabledTrue();

    /**
     * Encuentra usuarios por rol
     */
    List<User> findByRole(User.Role role);

    /**
     * Encuentra usuarios por rol con paginación
     */
    Page<User> findByRole(User.Role role, Pageable pageable);

    /**
     * Encuentra usuarios activos por rol
     */
    List<User> findByRoleAndEnabledTrue(User.Role role);

    /**
     * Encuentra usuarios deshabilitados
     */
    List<User> findByEnabledFalse();

    /**
     * Encuentra usuarios con cuentas bloqueadas
     */
    List<User> findByAccountNonLockedFalse();

    // ===================================================================
    // CONSULTAS POR PERFIL DE RIESGO
    // ===================================================================

    /**
     * Encuentra usuarios por perfil de riesgo
     */
    List<User> findByRiskProfile(User.RiskProfile riskProfile);

    /**
     * Cuenta usuarios por perfil de riesgo
     */
    long countByRiskProfile(User.RiskProfile riskProfile);

    // ===================================================================
    // CONSULTAS POR DATOS PERSONALES
    // ===================================================================

    /**
     * Busca usuarios por nombre (insensible a mayúsculas)
     */
    @Query("{ 'firstName': { $regex: ?0, $options: 'i' } }")
    List<User> findByFirstNameIgnoreCase(String firstName);

    /**
     * Busca usuarios por apellido (insensible a mayúsculas)
     */
    @Query("{ 'lastName': { $regex: ?0, $options: 'i' } }")
    List<User> findByLastNameIgnoreCase(String lastName);

    /**
     * Busca usuarios por nombre completo (nombre + apellido)
     */
    @Query("{ $or: [ " +
           "{ 'firstName': { $regex: ?0, $options: 'i' } }, " +
           "{ 'lastName': { $regex: ?0, $options: 'i' } }, " +
           "{ $expr: { $regexMatch: { input: { $concat: ['$firstName', ' ', '$lastName'] }, regex: ?0, options: 'i' } } } " +
           "] }")
    List<User> findByFullNameContaining(String name);

    /**
     * Busca usuarios por teléfono
     */
    Optional<User> findByPhone(String phone);

    // ===================================================================
    // CONSULTAS POR FECHAS
    // ===================================================================

    /**
     * Encuentra usuarios creados después de una fecha
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Encuentra usuarios creados entre dos fechas
     */
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Encuentra usuarios que han iniciado sesión después de una fecha
     */
    List<User> findByLastLoginAtAfter(LocalDateTime date);

    /**
     * Encuentra usuarios que no han iniciado sesión desde una fecha
     */
    @Query("{ $or: [ { 'lastLoginAt': { $lt: ?0 } }, { 'lastLoginAt': null } ] }")
    List<User> findUsersNotLoggedInSince(LocalDateTime date);

    // ===================================================================
    // CONSULTAS POR PORTFOLIOS
    // ===================================================================

    /**
     * Encuentra usuarios que tienen un portfolio específico
     */
    List<User> findByPortfolioIdsContaining(String portfolioId);

    /**
     * Encuentra usuarios sin portfolios
     */
    @Query("{ $or: [ { 'portfolioIds': { $size: 0 } }, { 'portfolioIds': null } ] }")
    List<User> findUsersWithoutPortfolios();

    /**
     * Encuentra usuarios con más de X portfolios
     */
    @Query("{ 'portfolioIds': { $size: { $gte: ?0 } } }")
    List<User> findUsersWithPortfoliosCountGreaterThan(int count);

    // ===================================================================
    // CONSULTAS POR PREFERENCIAS
    // ===================================================================

    /**
     * Encuentra usuarios por moneda preferida
     */
    @Query("{ 'preferences.currency': ?0 }")
    List<User> findByPreferredCurrency(String currency);

    /**
     * Encuentra usuarios por idioma preferido
     */
    @Query("{ 'preferences.language': ?0 }")
    List<User> findByPreferredLanguage(String language);

    /**
     * Encuentra usuarios que han optado por notificaciones por email
     */
    @Query("{ 'preferences.emailNotifications': true }")
    List<User> findUsersWithEmailNotificationsEnabled();

    /**
     * Encuentra usuarios que han optado por emails de marketing
     */
    @Query("{ 'preferences.marketingEmails': true }")
    List<User> findUsersWithMarketingEmailsEnabled();

    // ===================================================================
    // CONSULTAS DE SEGURIDAD
    // ===================================================================

    /**
     * Encuentra usuarios con intentos de login fallidos superiores a un umbral
     */
    @Query("{ 'loginAttempts': { $gte: ?0 } }")
    List<User> findUsersWithFailedLoginAttempts(int threshold);

    /**
     * Encuentra usuarios por IP de último login
     */
    List<User> findByLastLoginIp(String ip);

    /**
     * Encuentra usuarios con credenciales expiradas
     */
    List<User> findByCredentialsNonExpiredFalse();

    // ===================================================================
    // CONSULTAS DE ESTADÍSTICAS Y REPORTING
    // ===================================================================

    /**
     * Cuenta usuarios por rol
     */
    long countByRole(User.Role role);

    /**
     * Cuenta usuarios activos
     */
    long countByEnabledTrue();

    /**
     * Cuenta usuarios registrados en el último mes
     */
    @Query(value = "{ 'createdAt': { $gte: ?0 } }", count = true)
    long countUsersRegisteredSince(LocalDateTime date);

    /**
     * Cuenta usuarios que han iniciado sesión en el último mes
     */
    @Query(value = "{ 'lastLoginAt': { $gte: ?0 } }", count = true)
    long countActiveUsersSince(LocalDateTime date);

    // ===================================================================
    // CONSULTAS PERSONALIZADAS COMPLEJAS
    // ===================================================================

    /**
     * Busca usuarios premium activos con portfolios
     */
    @Query("{ 'role': 'PREMIUM', 'enabled': true, 'portfolioIds': { $exists: true, $not: { $size: 0 } } }")
    List<User> findActivePremiumUsersWithPortfolios();

    /**
     * Busca usuarios para limpieza de datos (inactivos por más de X tiempo)
     */
    @Query("{ 'enabled': false, 'lastLoginAt': { $lt: ?0 } }")
    List<User> findInactiveUsersForCleanup(LocalDateTime cutoffDate);

    /**
     * Busca usuarios candidatos para upgrade a premium
     * (usuarios activos con múltiples portfolios)
     */
    @Query("{ 'role': 'USER', 'enabled': true, 'portfolioIds': { $size: { $gte: ?0 } }, 'lastLoginAt': { $gte: ?1 } }")
    List<User> findPremiumUpgradeCandidates(int minPortfolios, LocalDateTime recentActivityDate);

    /**
     * Proyección para listar usuarios con información básica
     */
    @Query(value = "{}", fields = "{ 'firstName': 1, 'lastName': 1, 'email': 1, 'role': 1, 'enabled': 1, 'createdAt': 1 }")
    List<User> findAllBasicInfo();

    /**
     * Busca usuarios por múltiples criterios con paginación
     */
    @Query("{ $and: [ " +
           "{ $or: [ { 'role': { $in: ?0 } }, { 'role': { $exists: false } } ] }, " +
           "{ 'enabled': ?1 }, " +
           "{ 'createdAt': { $gte: ?2 } } " +
           "] }")
    Page<User> findByMultipleCriteria(List<User.Role> roles, Boolean enabled, LocalDateTime createdAfter, Pageable pageable);
}
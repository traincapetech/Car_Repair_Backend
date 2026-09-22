package com.carservice.backend.user.repository;

import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByIdAndRole(Long id, UserRole role);

    java.util.List<User> findByRole(UserRole role);

    java.util.List<User> findByRoleIn(Collection<UserRole> roles);

    long countByRole(UserRole role);

    long countByStatus(UserStatus status);

    long countByIsActiveTrue();

    long countByRoleAndIsActiveTrue(UserRole role);

    long countByRoleAndStatus(UserRole role, UserStatus status);

    long countByRoleIn(Collection<UserRole> roles);

    long countByRoleInAndIsActiveTrue(Collection<UserRole> roles);

    @Query(value = """
        SELECT u FROM User u
        WHERE u.role = com.carservice.backend.user.enums.UserRole.CUSTOMER
          AND (:isActive IS NULL OR u.isActive = :isActive)
          AND (
            :search IS NULL OR :search = ''
            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR u.phone LIKE CONCAT('%', :search, '%')
          )
    """,
    countQuery = """
        SELECT COUNT(u) FROM User u
        WHERE u.role = com.carservice.backend.user.enums.UserRole.CUSTOMER
          AND (:isActive IS NULL OR u.isActive = :isActive)
          AND (
            :search IS NULL OR :search = ''
            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR u.phone LIKE CONCAT('%', :search, '%')
          )
    """)
    Page<User> findCustomersWithFilter(
        @Param("isActive") Boolean isActive,
        @Param("search") String search,
        Pageable pageable
    );

    @Query(value = """
        SELECT u FROM User u
        WHERE (:role IS NULL OR u.role = :role)
          AND (
            :status IS NULL 
            OR u.status = :status 
            OR (u.status IS NULL AND :status = com.carservice.backend.user.enums.UserStatus.ACTIVE AND u.isActive = true)
            OR (u.status IS NULL AND :status = com.carservice.backend.user.enums.UserStatus.INACTIVE AND (u.isActive IS NULL OR u.isActive = false))
          )
          AND (:isActive IS NULL OR u.isActive = :isActive)
          AND (
            :search IS NULL OR :search = ''
            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR u.phone LIKE CONCAT('%', :search, '%')
          )
    """,
    countQuery = """
        SELECT COUNT(u) FROM User u
        WHERE (:role IS NULL OR u.role = :role)
          AND (
            :status IS NULL 
            OR u.status = :status 
            OR (u.status IS NULL AND :status = com.carservice.backend.user.enums.UserStatus.ACTIVE AND u.isActive = true)
            OR (u.status IS NULL AND :status = com.carservice.backend.user.enums.UserStatus.INACTIVE AND (u.isActive IS NULL OR u.isActive = false))
          )
          AND (:isActive IS NULL OR u.isActive = :isActive)
          AND (
            :search IS NULL OR :search = ''
            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR u.phone LIKE CONCAT('%', :search, '%')
          )
    """)
    Page<User> findUsersWithFilter(
        @Param("role") UserRole role,
        @Param("status") UserStatus status,
        @Param("isActive") Boolean isActive,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = com.carservice.backend.user.enums.UserRole.CUSTOMER AND (:from IS NULL OR u.createdAt >= :from) AND (:to IS NULL OR u.createdAt <= :to)")
    long countCustomersByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = com.carservice.backend.user.enums.UserRole.CUSTOMER AND (:isActive IS NULL OR u.isActive = :isActive) AND (:from IS NULL OR u.createdAt >= :from) AND (:to IS NULL OR u.createdAt <= :to)")
    long countCustomersByActiveAndDateRange(@Param("isActive") Boolean isActive, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query(value = "SELECT DATE(u.created_at) as reg_date, COUNT(*) as cnt FROM users u WHERE u.role = 'CUSTOMER' AND (:from IS NULL OR u.created_at >= :from) AND (:to IS NULL OR u.created_at <= :to) GROUP BY DATE(u.created_at) ORDER BY reg_date ASC", nativeQuery = true)
    java.util.List<Object[]> countCustomerRegistrationsByDayNative(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);
}
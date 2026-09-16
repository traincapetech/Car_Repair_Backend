package com.carservice.backend.user.repository;

import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByIdAndRole(Long id, UserRole role);

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
}
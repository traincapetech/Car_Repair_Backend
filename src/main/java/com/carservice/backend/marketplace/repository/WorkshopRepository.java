package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopRepository extends JpaRepository<Workshop, Long> {
    Optional<Workshop> findByUserId(Long userId);
    List<Workshop> findByIsActiveTrueAndVerificationStatus(WorkshopVerificationStatus verificationStatus);
    Optional<Workshop> findByEmail(String email);
    Optional<Workshop> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Query(value = """
        SELECT w FROM Workshop w
        JOIN FETCH w.user u
        WHERE (:isActive IS NULL OR w.isActive = :isActive)
          AND (:verificationStatus IS NULL OR w.verificationStatus = :verificationStatus)
          AND (:city IS NULL OR :city = '' OR LOWER(w.city) = LOWER(:city))
          AND (:state IS NULL OR :state = '' OR LOWER(w.state) = LOWER(:state))
          AND (
            :search IS NULL OR :search = ''
            OR LOWER(w.businessName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(w.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR w.phone LIKE CONCAT('%', :search, '%')
            OR LOWER(w.city) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(w.state) LIKE LOWER(CONCAT('%', :search, '%'))
            OR w.pincode LIKE CONCAT('%', :search, '%')
          )
    """,
    countQuery = """
        SELECT COUNT(w) FROM Workshop w
        JOIN w.user u
        WHERE (:isActive IS NULL OR w.isActive = :isActive)
          AND (:verificationStatus IS NULL OR w.verificationStatus = :verificationStatus)
          AND (:city IS NULL OR :city = '' OR LOWER(w.city) = LOWER(:city))
          AND (:state IS NULL OR :state = '' OR LOWER(w.state) = LOWER(:state))
          AND (
            :search IS NULL OR :search = ''
            OR LOWER(w.businessName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(w.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR w.phone LIKE CONCAT('%', :search, '%')
            OR LOWER(w.city) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(w.state) LIKE LOWER(CONCAT('%', :search, '%'))
            OR w.pincode LIKE CONCAT('%', :search, '%')
          )
    """)
    Page<Workshop> findWorkshopsWithFilter(
        @Param("isActive") Boolean isActive,
        @Param("verificationStatus") WorkshopVerificationStatus verificationStatus,
        @Param("city") String city,
        @Param("state") String state,
        @Param("search") String search,
        Pageable pageable
    );

    long countByIsActiveTrue();

    long countByVerificationStatus(WorkshopVerificationStatus verificationStatus);

    @Query("SELECT COUNT(w) FROM Workshop w WHERE w.isActive = false OR w.verificationStatus = com.carservice.backend.marketplace.enums.WorkshopVerificationStatus.SUSPENDED")
    long countSuspendedOrInactive();

    @Query("SELECT COUNT(w) FROM Workshop w WHERE (:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to)")
    long countWorkshopsByDateRange(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT COUNT(w) FROM Workshop w WHERE (:status IS NULL OR w.verificationStatus = :status) AND (:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to)")
    long countWorkshopsByVerificationStatusAndDateRange(@Param("status") WorkshopVerificationStatus status, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT COUNT(w) FROM Workshop w WHERE (:isActive IS NULL OR w.isActive = :isActive) AND (:from IS NULL OR w.createdAt >= :from) AND (:to IS NULL OR w.createdAt <= :to)")
    long countWorkshopsByActiveAndDateRange(@Param("isActive") Boolean isActive, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT COUNT(w) FROM Workshop w WHERE w.latitude IS NOT NULL AND w.longitude IS NOT NULL")
    long countWorkshopsWithCoordinates();

    @Query(value = "SELECT DATE(w.created_at) as reg_date, COUNT(*) as cnt FROM workshops w WHERE (:from IS NULL OR w.created_at >= :from) AND (:to IS NULL OR w.created_at <= :to) GROUP BY DATE(w.created_at) ORDER BY reg_date ASC", nativeQuery = true)
    java.util.List<Object[]> countWorkshopRegistrationsByDayNative(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT w.city, w.state, COUNT(w) FROM Workshop w WHERE w.city IS NOT NULL AND w.city <> '' GROUP BY w.city, w.state ORDER BY COUNT(w) DESC")
    java.util.List<Object[]> countWorkshopsByCityAndState();
}

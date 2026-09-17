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
    List<Workshop> findByCityIgnoreCaseAndIsActiveTrue(String city);

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
}

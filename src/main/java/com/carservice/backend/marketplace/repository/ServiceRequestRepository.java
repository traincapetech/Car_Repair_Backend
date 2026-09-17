package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    Optional<ServiceRequest> findByRequestReference(String requestReference);
    List<ServiceRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(ServiceRequestStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE ServiceRequest sr SET sr.assignedWorkshop = :workshop, sr.status = :newStatus WHERE sr.id = :requestId AND sr.assignedWorkshop IS NULL AND (sr.status = 'MATCHED' OR sr.status = 'SUBMITTED' OR sr.status = 'RE_MATCHING')")
    int claimServiceRequest(
            @Param("requestId") Long requestId,
            @Param("workshop") Workshop workshop,
            @Param("newStatus") ServiceRequestStatus newStatus
    );

    @Query(value = "SELECT sr FROM ServiceRequest sr WHERE sr.user.id = :userId AND (:status IS NULL OR sr.status = :status)",
           countQuery = "SELECT COUNT(sr) FROM ServiceRequest sr WHERE sr.user.id = :userId AND (:status IS NULL OR sr.status = :status)")
    @EntityGraph(attributePaths = {"vehicle", "assignedWorkshop"})
    Page<ServiceRequest> findByUserIdAndOptionalStatus(@Param("userId") Long userId, @Param("status") ServiceRequestStatus status, Pageable pageable);

    @Query("SELECT sr.user.id, COUNT(sr) FROM ServiceRequest sr WHERE sr.user.id IN :userIds GROUP BY sr.user.id")
    List<Object[]> countServiceRequestsByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("SELECT sr.status, COUNT(sr) FROM ServiceRequest sr WHERE sr.user.id = :userId GROUP BY sr.status")
    List<Object[]> countServiceRequestsByStatusForUser(@Param("userId") Long userId);

    @Query(value = """
        SELECT sr FROM ServiceRequest sr
        JOIN FETCH sr.user u
        JOIN FETCH sr.vehicle v
        LEFT JOIN FETCH sr.assignedWorkshop w
        WHERE (:search IS NULL OR :search = '' OR
               LOWER(sr.requestReference) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.make) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
               (w IS NOT NULL AND LOWER(w.businessName) LIKE LOWER(CONCAT('%', :search, '%'))) OR
               (sr.bookingReference IS NOT NULL AND LOWER(sr.bookingReference) LIKE LOWER(CONCAT('%', :search, '%'))))
          AND (:status IS NULL OR sr.status = :status)
          AND (:city IS NULL OR :city = '' OR LOWER(sr.city) = LOWER(:city))
          AND (:workshopId IS NULL OR (sr.assignedWorkshop IS NOT NULL AND sr.assignedWorkshop.id = :workshopId))
          AND (:startDate IS NULL OR sr.createdAt >= :startDate)
          AND (:endDate IS NULL OR sr.createdAt <= :endDate)
    """,
    countQuery = """
        SELECT COUNT(sr) FROM ServiceRequest sr
        LEFT JOIN sr.user u
        LEFT JOIN sr.vehicle v
        LEFT JOIN sr.assignedWorkshop w
        WHERE (:search IS NULL OR :search = '' OR
               LOWER(sr.requestReference) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.make) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
               (w IS NOT NULL AND LOWER(w.businessName) LIKE LOWER(CONCAT('%', :search, '%'))) OR
               (sr.bookingReference IS NOT NULL AND LOWER(sr.bookingReference) LIKE LOWER(CONCAT('%', :search, '%'))))
          AND (:status IS NULL OR sr.status = :status)
          AND (:city IS NULL OR :city = '' OR LOWER(sr.city) = LOWER(:city))
          AND (:workshopId IS NULL OR (sr.assignedWorkshop IS NOT NULL AND sr.assignedWorkshop.id = :workshopId))
          AND (:startDate IS NULL OR sr.createdAt >= :startDate)
          AND (:endDate IS NULL OR sr.createdAt <= :endDate)
    """)
    Page<ServiceRequest> findServiceRequestsWithFilter(
            @Param("search") String search,
            @Param("status") ServiceRequestStatus status,
            @Param("city") String city,
            @Param("workshopId") Long workshopId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    long countByStatus(ServiceRequestStatus status);

    long countByStatusIn(Collection<ServiceRequestStatus> statuses);
}


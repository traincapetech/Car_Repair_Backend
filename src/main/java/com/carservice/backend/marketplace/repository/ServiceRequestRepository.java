package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
}

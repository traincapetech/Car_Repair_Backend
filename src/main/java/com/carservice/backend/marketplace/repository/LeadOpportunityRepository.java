package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.LeadOpportunity;
import com.carservice.backend.marketplace.enums.OpportunityStatus;
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
public interface LeadOpportunityRepository extends JpaRepository<LeadOpportunity, Long> {
    List<LeadOpportunity> findByWorkshopIdOrderByCreatedAtDesc(Long workshopId);
    List<LeadOpportunity> findByWorkshopIdAndStatusOrderByCreatedAtDesc(Long workshopId, OpportunityStatus status);
    List<LeadOpportunity> findByServiceRequestId(Long serviceRequestId);
    Optional<LeadOpportunity> findByServiceRequestIdAndWorkshopId(Long serviceRequestId, Long workshopId);
    Optional<LeadOpportunity> findFirstByServiceRequestIdAndWorkshopIdOrderByCreatedAtDesc(Long serviceRequestId, Long workshopId);
    boolean existsByServiceRequestIdAndWorkshopId(Long serviceRequestId, Long workshopId);
    boolean existsByServiceRequestIdAndWorkshopIdAndStatusIn(Long serviceRequestId, Long workshopId, Collection<OpportunityStatus> statuses);

    @Modifying
    @Transactional
    @Query("UPDATE LeadOpportunity lo SET lo.status = :newStatus, lo.transferredAt = :transferredAt, lo.transferReason = :reason " +
           "WHERE lo.id = :opportunityId AND lo.status IN ('AVAILABLE', 'VIEWED', 'ACCEPTED', 'PAYMENT_PENDING', 'PAID', 'CUSTOMER_DETAILS_UNLOCKED', 'ASSIGNED')")
    int markAsTransferred(
            @Param("opportunityId") Long opportunityId,
            @Param("newStatus") OpportunityStatus newStatus,
            @Param("transferredAt") LocalDateTime transferredAt,
            @Param("reason") String reason
    );
}


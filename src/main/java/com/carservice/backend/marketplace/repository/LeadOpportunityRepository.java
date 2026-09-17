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

    @Query("SELECT lo.workshop.id, COUNT(lo) FROM LeadOpportunity lo WHERE lo.workshop.id IN :workshopIds GROUP BY lo.workshop.id")
    List<Object[]> countOpportunitiesByWorkshopIds(@Param("workshopIds") Collection<Long> workshopIds);

    @Query("SELECT lo.workshop.id, COUNT(lo) FROM LeadOpportunity lo WHERE lo.workshop.id IN :workshopIds AND lo.status IN (com.carservice.backend.marketplace.enums.OpportunityStatus.ACCEPTED, com.carservice.backend.marketplace.enums.OpportunityStatus.PAID, com.carservice.backend.marketplace.enums.OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED, com.carservice.backend.marketplace.enums.OpportunityStatus.ASSIGNED, com.carservice.backend.marketplace.enums.OpportunityStatus.COMPLETED) GROUP BY lo.workshop.id")
    List<Object[]> countAcceptedOpportunitiesByWorkshopIds(@Param("workshopIds") Collection<Long> workshopIds);

    @Query(value = """
        SELECT lo FROM LeadOpportunity lo
        JOIN FETCH lo.serviceRequest sr
        LEFT JOIN FETCH sr.vehicle v
        WHERE lo.workshop.id = :workshopId
          AND (:status IS NULL OR lo.status = :status)
        ORDER BY lo.createdAt DESC
    """,
    countQuery = """
        SELECT COUNT(lo) FROM LeadOpportunity lo
        WHERE lo.workshop.id = :workshopId
          AND (:status IS NULL OR lo.status = :status)
    """)
    org.springframework.data.domain.Page<LeadOpportunity> findByWorkshopIdAndOptionalStatus(
            @Param("workshopId") Long workshopId,
            @Param("status") OpportunityStatus status,
            org.springframework.data.domain.Pageable pageable
    );

    long countByWorkshopId(Long workshopId);

    long countByWorkshopIdAndStatus(Long workshopId, OpportunityStatus status);

    long countByWorkshopIdAndStatusIn(Long workshopId, Collection<OpportunityStatus> statuses);

    @Query("SELECT lo.serviceRequest.id, COUNT(lo) FROM LeadOpportunity lo WHERE lo.serviceRequest.id IN :requestIds GROUP BY lo.serviceRequest.id")
    List<Object[]> countByServiceRequestIds(@Param("requestIds") Collection<Long> requestIds);

    @Query("SELECT lo FROM LeadOpportunity lo JOIN FETCH lo.workshop w LEFT JOIN FETCH lo.payment p WHERE lo.serviceRequest.id = :serviceRequestId ORDER BY lo.createdAt ASC")
    List<LeadOpportunity> findByServiceRequestIdWithDetails(@Param("serviceRequestId") Long serviceRequestId);

    long countByStatus(OpportunityStatus status);

    long countByStatusIn(Collection<OpportunityStatus> statuses);
}


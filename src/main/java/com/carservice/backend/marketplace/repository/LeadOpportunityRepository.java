package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.LeadOpportunity;
import com.carservice.backend.marketplace.enums.OpportunityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadOpportunityRepository extends JpaRepository<LeadOpportunity, Long> {
    List<LeadOpportunity> findByWorkshopIdOrderByCreatedAtDesc(Long workshopId);
    List<LeadOpportunity> findByWorkshopIdAndStatusOrderByCreatedAtDesc(Long workshopId, OpportunityStatus status);
    List<LeadOpportunity> findByServiceRequestId(Long serviceRequestId);
    Optional<LeadOpportunity> findByServiceRequestIdAndWorkshopId(Long serviceRequestId, Long workshopId);
    boolean existsByServiceRequestIdAndWorkshopId(Long serviceRequestId, Long workshopId);
}

package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.MarketplaceAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketplaceAuditEventRepository extends JpaRepository<MarketplaceAuditEvent, Long> {
    List<MarketplaceAuditEvent> findByServiceRequestIdOrderByCreatedAtDesc(Long serviceRequestId);
    List<MarketplaceAuditEvent> findByWorkshopIdOrderByCreatedAtDesc(Long workshopId);
    List<MarketplaceAuditEvent> findTop100ByOrderByCreatedAtDesc();
}

package com.carservice.backend.marketplace.service;

import com.carservice.backend.marketplace.entity.MarketplaceAuditEvent;
import com.carservice.backend.marketplace.enums.MarketplaceEventType;
import com.carservice.backend.marketplace.repository.MarketplaceAuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MarketplaceAuditService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceAuditService.class);

    private final MarketplaceAuditEventRepository auditEventRepository;

    public MarketplaceAuditService(MarketplaceAuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public MarketplaceAuditEvent recordEvent(
            MarketplaceEventType eventType,
            Long serviceRequestId,
            Long leadOpportunityId,
            Long workshopId,
            Long actorUserId,
            String description,
            String metadataJson
    ) {
        log.info("MarketplaceEvent [{}]: req={}, opp={}, workshop={}, actor={}, desc={}",
                eventType, serviceRequestId, leadOpportunityId, workshopId, actorUserId, description);

        MarketplaceAuditEvent event = new MarketplaceAuditEvent(
                eventType,
                serviceRequestId,
                leadOpportunityId,
                workshopId,
                actorUserId,
                description != null ? description : eventType.name(),
                metadataJson
        );
        return auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceAuditEvent> getAuditEventsForRequest(Long serviceRequestId) {
        return auditEventRepository.findByServiceRequestIdOrderByCreatedAtDesc(serviceRequestId);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceAuditEvent> getAuditEventsForWorkshop(Long workshopId) {
        return auditEventRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopId);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceAuditEvent> getRecentAuditEvents() {
        return auditEventRepository.findTop100ByOrderByCreatedAtDesc();
    }
}

package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.ServiceRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestItemRepository extends JpaRepository<ServiceRequestItem, Long> {
    List<ServiceRequestItem> findByServiceRequestId(Long serviceRequestId);

    boolean existsByServiceCatalogId(Long serviceCatalogId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT sri.serviceNameSnapshot, COUNT(sri), COALESCE(SUM(sri.finalPriceSnapshot), 0)
        FROM ServiceRequestItem sri
        GROUP BY sri.serviceNameSnapshot
        ORDER BY COUNT(sri) DESC
    """)
    List<Object[]> findMostRequestedServices(org.springframework.data.domain.Pageable pageable);
}

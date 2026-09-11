package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.ServiceRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestItemRepository extends JpaRepository<ServiceRequestItem, Long> {
    List<ServiceRequestItem> findByServiceRequestId(Long serviceRequestId);
}

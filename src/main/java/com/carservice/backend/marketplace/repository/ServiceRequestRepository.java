package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    Optional<ServiceRequest> findByRequestReference(String requestReference);
    List<ServiceRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(ServiceRequestStatus status);
}

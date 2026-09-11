package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopRepository extends JpaRepository<Workshop, Long> {
    Optional<Workshop> findByUserId(Long userId);
    List<Workshop> findByIsActiveTrueAndVerificationStatus(WorkshopVerificationStatus verificationStatus);
    List<Workshop> findByCityIgnoreCaseAndIsActiveTrue(String city);
}

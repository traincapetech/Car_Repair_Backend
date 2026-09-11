package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.LeadPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeadPaymentRepository extends JpaRepository<LeadPayment, Long> {
    Optional<LeadPayment> findByOpportunityId(Long opportunityId);
}

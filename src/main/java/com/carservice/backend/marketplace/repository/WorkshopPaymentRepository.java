package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.WorkshopPayment;
import com.carservice.backend.marketplace.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopPaymentRepository extends JpaRepository<WorkshopPayment, Long> {

    List<WorkshopPayment> findByOpportunityId(Long opportunityId);

    List<WorkshopPayment> findByWorkshopIdOrderByCreatedAtDesc(Long workshopId);

    Optional<WorkshopPayment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<WorkshopPayment> findByOpportunityIdAndIdempotencyKey(Long opportunityId, String idempotencyKey);

    List<WorkshopPayment> findByOpportunityIdAndPaymentStatus(Long opportunityId, PaymentStatus status);

    Optional<WorkshopPayment> findFirstByOpportunityIdAndPaymentStatusOrderByCreatedAtDesc(Long opportunityId, PaymentStatus status);
}

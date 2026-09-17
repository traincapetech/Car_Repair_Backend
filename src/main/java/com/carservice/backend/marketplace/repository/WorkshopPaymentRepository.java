package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.WorkshopPayment;
import com.carservice.backend.marketplace.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopPaymentRepository extends JpaRepository<WorkshopPayment, Long> {

    List<WorkshopPayment> findByOpportunityId(Long opportunityId);

    List<WorkshopPayment> findByWorkshopIdOrderByCreatedAtDesc(Long workshopId);

    Optional<WorkshopPayment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<WorkshopPayment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<WorkshopPayment> findByOpportunityIdAndIdempotencyKey(Long opportunityId, String idempotencyKey);

    List<WorkshopPayment> findByOpportunityIdAndPaymentStatus(Long opportunityId, PaymentStatus status);

    Optional<WorkshopPayment> findFirstByOpportunityIdAndPaymentStatusOrderByCreatedAtDesc(Long opportunityId, PaymentStatus status);

    @Query("SELECT wp FROM WorkshopPayment wp JOIN FETCH wp.workshop w JOIN FETCH wp.opportunity lo WHERE lo.serviceRequest.id = :serviceRequestId ORDER BY wp.createdAt ASC")
    List<WorkshopPayment> findByServiceRequestIdWithDetails(@Param("serviceRequestId") Long serviceRequestId);

    @Query("SELECT COALESCE(SUM(wp.amount), 0) FROM WorkshopPayment wp WHERE wp.paymentStatus = 'SUCCESS'")
    BigDecimal sumSuccessfulPayments();

    long countByPaymentStatus(PaymentStatus paymentStatus);
}

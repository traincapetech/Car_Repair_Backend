package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.Refund;
import com.carservice.backend.marketplace.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByPaymentId(Long paymentId);

    List<Refund> findByOpportunityId(Long opportunityId);

    List<Refund> findByWorkshopIdOrderByCreatedAtDesc(Long workshopId);

    List<Refund> findByRefundStatus(RefundStatus refundStatus);

    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);

    List<Refund> findAllByOrderByCreatedAtDesc();

    @Query("SELECT r FROM Refund r JOIN FETCH r.workshop w JOIN FETCH r.opportunity lo WHERE lo.serviceRequest.id = :serviceRequestId ORDER BY r.createdAt ASC")
    List<Refund> findByServiceRequestIdWithDetails(@Param("serviceRequestId") Long serviceRequestId);

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Refund r WHERE r.refundStatus = 'SUCCESS'")
    BigDecimal sumProcessedRefunds();

    long countByRefundStatus(RefundStatus refundStatus);
}

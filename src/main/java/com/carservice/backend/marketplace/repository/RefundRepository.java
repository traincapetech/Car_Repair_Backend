package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.Refund;
import com.carservice.backend.marketplace.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByPaymentId(Long paymentId);

    List<Refund> findByOpportunityId(Long opportunityId);

    List<Refund> findByWorkshopIdOrderByCreatedAtDesc(Long workshopId);

    List<Refund> findByRefundStatus(RefundStatus refundStatus);

    List<Refund> findAllByOrderByCreatedAtDesc();
}

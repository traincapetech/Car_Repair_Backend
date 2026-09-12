package com.carservice.backend.marketplace.service;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.dto.RefundResponse;
import com.carservice.backend.marketplace.entity.LeadOpportunity;
import com.carservice.backend.marketplace.entity.Refund;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.entity.WorkshopPayment;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.payment.PaymentGateway;
import com.carservice.backend.marketplace.payment.PaymentRefund;
import com.carservice.backend.marketplace.repository.RefundRepository;
import com.carservice.backend.marketplace.repository.WorkshopPaymentRepository;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkshopRefundService {

    private static final Logger log = LoggerFactory.getLogger(WorkshopRefundService.class);

    private final RefundRepository refundRepository;
    private final WorkshopPaymentRepository workshopPaymentRepository;
    private final WorkshopRepository workshopRepository;
    private final WalletService walletService;
    private final PaymentGateway paymentGateway;
    private final MarketplaceAuditService auditService;

    public WorkshopRefundService(
            RefundRepository refundRepository,
            WorkshopPaymentRepository workshopPaymentRepository,
            WorkshopRepository workshopRepository,
            WalletService walletService,
            PaymentGateway paymentGateway,
            MarketplaceAuditService auditService
    ) {
        this.refundRepository = refundRepository;
        this.workshopPaymentRepository = workshopPaymentRepository;
        this.workshopRepository = workshopRepository;
        this.walletService = walletService;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
    }

    private Workshop resolveWorkshop(User currentUser) {
        return workshopRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No workshop partner account found for user: " + currentUser.getEmail()));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Refund createRefundRecord(WorkshopPayment payment, RefundReason reason) {
        LeadOpportunity opportunity = payment.getOpportunity();
        Workshop workshop = payment.getWorkshop();

        log.info("Creating refund record for payment #{}, workshop: {}, reason: {}", payment.getId(), workshop.getBusinessName(), reason);

        Refund refund = new Refund(
                payment,
                opportunity,
                workshop,
                payment.getAmount(),
                RefundStatus.INITIATED,
                reason
        );

        Refund savedRefund = refundRepository.save(refund);

        auditService.recordEvent(
                MarketplaceEventType.REFUND_INITIATED,
                opportunity.getServiceRequest().getId(),
                opportunity.getId(),
                workshop.getId(),
                null,
                "Refund initiated for payment #" + payment.getId() + " due to " + reason,
                "{\"amount\": " + payment.getAmount() + ", \"reason\": \"" + reason + "\"}"
        );

        // Process refund based on payment method
        if (payment.getPaymentMethod() == PaymentMethod.WALLET) {
            processWalletRefund(savedRefund, payment, workshop, opportunity);
        } else if (payment.getPaymentMethod() == PaymentMethod.RAZORPAY) {
            processRazorpayRefund(savedRefund, payment, opportunity);
        }

        return savedRefund;
    }

    private void processWalletRefund(Refund refund, WorkshopPayment payment, Workshop workshop, LeadOpportunity opportunity) {
        try {
            walletService.credit(
                    workshop,
                    refund.getRefundAmount(),
                    WalletReferenceType.OPPORTUNITY_REFUND,
                    "RFND-" + refund.getId(),
                    "REFUND-" + payment.getId(),
                    "Refund for lost lead opportunity #" + opportunity.getId()
            );

            refund.setRefundStatus(RefundStatus.SUCCESS);
            refund.setProcessedAt(LocalDateTime.now());
            refundRepository.save(refund);

            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            workshopPaymentRepository.save(payment);

            auditService.recordEvent(
                    MarketplaceEventType.REFUND_SUCCESS,
                    opportunity.getServiceRequest().getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    null,
                    "Wallet refund of ₹" + refund.getRefundAmount() + " credited successfully",
                    "{\"refundId\": " + refund.getId() + "}"
            );
        } catch (Exception e) {
            log.error("Failed to process wallet refund: {}", e.getMessage(), e);
            refund.setRefundStatus(RefundStatus.FAILED);
            refund.setFailureReason(e.getMessage());
            refundRepository.save(refund);

            payment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
            payment.setFailureReason(e.getMessage());
            workshopPaymentRepository.save(payment);

            auditService.recordEvent(
                    MarketplaceEventType.REFUND_FAILED,
                    opportunity.getServiceRequest().getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    null,
                    "Wallet refund failed: " + e.getMessage(),
                    null
            );
        }
    }

    private void processRazorpayRefund(Refund refund, WorkshopPayment payment, LeadOpportunity opportunity) {
        if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank()) {
            refund.setRefundStatus(RefundStatus.PENDING);
            payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            refundRepository.save(refund);
            workshopPaymentRepository.save(payment);
            return;
        }

        try {
            PaymentRefund pr = paymentGateway.initiateRefund(
                    payment.getRazorpayPaymentId(),
                    refund.getRefundAmount(),
                    refund.getRefundReason().name()
            );

            refund.setRazorpayRefundId(pr.getRefundId());
            refund.setRefundStatus(RefundStatus.PENDING);
            refund.setProcessedAt(LocalDateTime.now());
            refundRepository.save(refund);

            payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            workshopPaymentRepository.save(payment);

            auditService.recordEvent(
                    MarketplaceEventType.REFUND_SUCCESS,
                    opportunity.getServiceRequest().getId(),
                    opportunity.getId(),
                    payment.getWorkshop().getId(),
                    null,
                    "Razorpay refund initiated with reference: " + pr.getRefundId(),
                    "{\"razorpayRefundId\": \"" + pr.getRefundId() + "\"}"
            );
        } catch (Exception e) {
            log.error("Failed to initiate Razorpay refund: {}", e.getMessage(), e);
            refund.setRefundStatus(RefundStatus.FAILED);
            refund.setFailureReason(e.getMessage());
            refundRepository.save(refund);

            payment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
            payment.setFailureReason(e.getMessage());
            workshopPaymentRepository.save(payment);
        }
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsForWorkshop(User currentUser) {
        Workshop workshop = resolveWorkshop(currentUser);
        return refundRepository.findByWorkshopIdOrderByCreatedAtDesc(workshop.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefundDetails(User currentUser, Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with id: " + refundId));

        if (currentUser.getRole() != UserRole.ADMIN) {
            Workshop workshop = resolveWorkshop(currentUser);
            if (!refund.getWorkshop().getId().equals(workshop.getId())) {
                throw new IllegalArgumentException("You are not authorized to view this refund");
            }
        }

        return mapToResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getAllRefundsForAdmin() {
        return refundRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RefundResponse mapToResponse(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getOpportunity().getId(),
                refund.getWorkshop().getId(),
                refund.getWorkshop().getBusinessName(),
                refund.getRefundAmount(),
                refund.getRefundStatus(),
                refund.getRefundReason(),
                refund.getRazorpayRefundId(),
                refund.getFailureReason(),
                refund.getInitiatedAt(),
                refund.getProcessedAt(),
                refund.getCreatedAt()
        );
    }
}

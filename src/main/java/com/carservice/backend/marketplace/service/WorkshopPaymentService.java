package com.carservice.backend.marketplace.service;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.dto.InitiatePaymentRequest;
import com.carservice.backend.marketplace.dto.InitiatePaymentResponse;
import com.carservice.backend.marketplace.dto.VerifyPaymentRequest;
import com.carservice.backend.marketplace.dto.WorkshopPaymentResponse;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.exception.PaymentGatewayUnavailableException;
import com.carservice.backend.marketplace.payment.PaymentGateway;
import com.carservice.backend.marketplace.payment.PaymentOrder;
import com.carservice.backend.marketplace.payment.RazorpayPaymentGateway;
import com.carservice.backend.marketplace.repository.*;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkshopPaymentService {

    private static final Logger log = LoggerFactory.getLogger(WorkshopPaymentService.class);

    private final WorkshopPaymentRepository workshopPaymentRepository;
    private final LeadOpportunityRepository leadOpportunityRepository;
    private final WorkshopRepository workshopRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final PlatformConfigService platformConfigService;
    private final WalletService walletService;
    private final WorkshopRefundService refundService;
    private final PaymentGateway paymentGateway;
    private final MarketplaceAuditService auditService;
    private final LeadPaymentRepository leadPaymentRepository;
    private final WorkshopJobService workshopJobService;
    private final com.carservice.backend.booking.repository.BookingRepository bookingRepository;

    public WorkshopPaymentService(
            WorkshopPaymentRepository workshopPaymentRepository,
            LeadOpportunityRepository leadOpportunityRepository,
            WorkshopRepository workshopRepository,
            ServiceRequestRepository serviceRequestRepository,
            PlatformConfigService platformConfigService,
            WalletService walletService,
            WorkshopRefundService refundService,
            PaymentGateway paymentGateway,
            MarketplaceAuditService auditService,
            LeadPaymentRepository leadPaymentRepository,
            WorkshopJobService workshopJobService,
            com.carservice.backend.booking.repository.BookingRepository bookingRepository
    ) {
        this.workshopPaymentRepository = workshopPaymentRepository;
        this.leadOpportunityRepository = leadOpportunityRepository;
        this.workshopRepository = workshopRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.platformConfigService = platformConfigService;
        this.walletService = walletService;
        this.refundService = refundService;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.leadPaymentRepository = leadPaymentRepository;
        this.workshopJobService = workshopJobService;
        this.bookingRepository = bookingRepository;
    }

    private Workshop resolveWorkshop(User currentUser) {
        return workshopRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No workshop partner account found for user: " + currentUser.getEmail()));
    }

    @Transactional
    public InitiatePaymentResponse initiatePayment(User currentUser, InitiatePaymentRequest request) {
        Workshop workshop = resolveWorkshop(currentUser);

        LeadOpportunity opportunity = leadOpportunityRepository.findById(request.getOpportunityId())
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + request.getOpportunityId()));

        if (!opportunity.getWorkshop().getId().equals(workshop.getId())) {
            throw new IllegalArgumentException("You are not authorized to pay for this lead opportunity");
        }

        if (opportunity.getStatus() == OpportunityStatus.DECLINED
                || opportunity.getStatus() == OpportunityStatus.EXPIRED
                || opportunity.getStatus() == OpportunityStatus.TRANSFERRED
                || opportunity.getStatus() == OpportunityStatus.CANCELLED
                || opportunity.getStatus() == OpportunityStatus.LOST) {
            throw new IllegalArgumentException("Cannot initiate payment for opportunity in state: " + opportunity.getStatus());
        }

        if (opportunity.isCustomerDetailsUnlocked()) {
            throw new IllegalArgumentException("Customer details are already unlocked for this opportunity");
        }

        // Idempotency check
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<WorkshopPayment> existing = workshopPaymentRepository.findByOpportunityIdAndIdempotencyKey(
                    opportunity.getId(),
                    request.getIdempotencyKey().trim()
            );
            if (existing.isPresent() && existing.get().getPaymentStatus() != PaymentStatus.FAILED && existing.get().getPaymentStatus() != PaymentStatus.CANCELLED) {
                log.info("Returning existing payment for idempotency key: {}", request.getIdempotencyKey());
                return mapToInitiateResponse(existing.get());
            }
        }

        // Authoritative fee snapshot
        BigDecimal fee = (opportunity.getFeeSnapshot() != null && opportunity.getFeeSnapshot().compareTo(BigDecimal.ZERO) > 0)
                ? opportunity.getFeeSnapshot()
                : platformConfigService.getLeadAcceptanceFee();

        String razorpayOrderId = null;
        if (request.getPaymentMethod() == PaymentMethod.RAZORPAY) {
            if (!paymentGateway.isAvailable()) {
                throw new PaymentGatewayUnavailableException("Razorpay payment gateway is currently unavailable.");
            }
            PaymentOrder order = paymentGateway.createOrder(fee, "INR", "OPP-" + opportunity.getId());
            razorpayOrderId = order.getOrderId();
        }

        WorkshopPayment payment = new WorkshopPayment(
                opportunity,
                workshop,
                fee,
                "INR",
                request.getPaymentMethod(),
                PaymentStatus.CREATED,
                request.getIdempotencyKey()
        );
        payment.setRazorpayOrderId(razorpayOrderId);
        WorkshopPayment savedPayment = workshopPaymentRepository.save(payment);

        opportunity.setStatus(OpportunityStatus.PAYMENT_PENDING);
        leadOpportunityRepository.save(opportunity);

        auditService.recordEvent(
                MarketplaceEventType.PAYMENT_CREATED,
                opportunity.getServiceRequest().getId(),
                opportunity.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Payment #" + savedPayment.getId() + " initiated via " + request.getPaymentMethod() + " for ₹" + fee,
                "{\"paymentId\": " + savedPayment.getId() + ", \"method\": \"" + request.getPaymentMethod() + "\"}"
        );

        return mapToInitiateResponse(savedPayment);
    }

    @Transactional
    public WorkshopPaymentResponse claimOpportunityWithWallet(User currentUser, Long opportunityId, String idempotencyKey) {
        Workshop workshop = resolveWorkshop(currentUser);

        LeadOpportunity opportunity = leadOpportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        if (!opportunity.getWorkshop().getId().equals(workshop.getId())) {
            throw new IllegalArgumentException("You are not authorized to pay for this lead opportunity");
        }

        if (opportunity.isCustomerDetailsUnlocked()) {
            Optional<WorkshopPayment> existingSuccessful = workshopPaymentRepository
                    .findFirstByOpportunityIdAndPaymentStatusOrderByCreatedAtDesc(opportunity.getId(), PaymentStatus.SUCCESS);
            if (existingSuccessful.isPresent()) {
                return mapToResponse(existingSuccessful.get());
            }
        }

        if (opportunity.getStatus() == OpportunityStatus.DECLINED
                || opportunity.getStatus() == OpportunityStatus.EXPIRED
                || opportunity.getStatus() == OpportunityStatus.TRANSFERRED
                || opportunity.getStatus() == OpportunityStatus.CANCELLED
                || opportunity.getStatus() == OpportunityStatus.LOST) {
            throw new IllegalArgumentException("Cannot pay for opportunity in state: " + opportunity.getStatus());
        }

        BigDecimal fee = (opportunity.getFeeSnapshot() != null && opportunity.getFeeSnapshot().compareTo(BigDecimal.ZERO) > 0)
                ? opportunity.getFeeSnapshot()
                : platformConfigService.getLeadAcceptanceFee();

        // 1. Debit Workshop Wallet
        String debitIdempotencyKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : "OPP-ACCEPT-" + opportunity.getId();
        walletService.debit(
                workshop,
                fee,
                WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE,
                "OPP-" + opportunity.getId(),
                debitIdempotencyKey,
                "Lead acceptance fee for opportunity #" + opportunity.getId()
        );

        // 2. Create / Update WorkshopPayment
        LocalDateTime now = LocalDateTime.now();
        WorkshopPayment payment = new WorkshopPayment(
                opportunity,
                workshop,
                fee,
                "INR",
                PaymentMethod.WALLET,
                PaymentStatus.SUCCESS,
                idempotencyKey
        );
        payment.setPaidAt(now);
        WorkshopPayment savedPayment = workshopPaymentRepository.save(payment);

        // 3. Backward compatible LeadPayment record
        String txRef = "WAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LeadPayment leadPayment = new LeadPayment(
                opportunity,
                LeadPaymentMethod.WALLET,
                fee,
                txRef,
                LeadPaymentStatus.SUCCESS
        );
        leadPayment.setPaidAt(now);
        leadPaymentRepository.save(leadPayment);
        opportunity.setPayment(leadPayment);

        // 4. ATOMIC CLAIM AT DATABASE LEVEL
        ServiceRequest serviceRequest = opportunity.getServiceRequest();
        int claimedRows = serviceRequestRepository.claimServiceRequest(
                serviceRequest.getId(),
                workshop,
                ServiceRequestStatus.ACCEPTED
        );

        if (claimedRows == 1) {
            // Workshop won!
            log.info("Workshop {} successfully claimed service request #{}", workshop.getBusinessName(), serviceRequest.getId());
            serviceRequest.setAssignedWorkshop(workshop);
            serviceRequest.setStatus(ServiceRequestStatus.ACCEPTED);
            opportunity.setStatus(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED);
            opportunity.setPaidAt(now);
            opportunity.setUnlockedAt(now);
            leadOpportunityRepository.save(opportunity);

            // Cancel competing unaccepted / unpaid opportunities (leave PAYMENT_PENDING so race-condition handling can refund them)
            List<LeadOpportunity> competing = leadOpportunityRepository.findByServiceRequestId(serviceRequest.getId());
            for (LeadOpportunity comp : competing) {
                if (!comp.getId().equals(opportunity.getId())
                        && (comp.getStatus() == OpportunityStatus.AVAILABLE
                        || comp.getStatus() == OpportunityStatus.VIEWED
                        || comp.getStatus() == OpportunityStatus.ACCEPTED)) {
                    comp.setStatus(OpportunityStatus.CANCELLED);
                    leadOpportunityRepository.save(comp);
                }
            }

            auditService.recordEvent(
                    MarketplaceEventType.PAYMENT_SUCCESS,
                    serviceRequest.getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Wallet payment of ₹" + fee + " successful for opportunity #" + opportunity.getId(),
                    "{\"method\": \"WALLET\", \"reference\": \"" + txRef + "\"}"
            );
            auditService.recordEvent(
                    MarketplaceEventType.DETAILS_UNLOCKED,
                    serviceRequest.getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Customer contact details unlocked for " + workshop.getBusinessName(),
                    null
            );
            auditService.recordEvent(
                    MarketplaceEventType.ASSIGNED_TO_WORKSHOP,
                    serviceRequest.getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Service request " + serviceRequest.getRequestReference() + " assigned to " + workshop.getBusinessName(),
                    null
            );

            // Initialize Workshop Job
            workshopJobService.createJobForAssignedOpportunity(serviceRequest, workshop, opportunity);

            // Confirm linked Booking if present
            if (serviceRequest.getBooking() != null) {
                com.carservice.backend.booking.entity.Booking linkedBooking = serviceRequest.getBooking();
                if (linkedBooking.getStatus() == com.carservice.backend.booking.enums.BookingStatus.PENDING) {
                    linkedBooking.setStatus(com.carservice.backend.booking.enums.BookingStatus.CONFIRMED);
                    bookingRepository.save(linkedBooking);
                }
            }

            return mapToResponse(savedPayment);
        } else {
            // RACE CONDITION: Another workshop claimed the request first!
            log.warn("Race condition lost: Workshop {} paid for opportunity #{} but service request #{} is already claimed by another workshop",
                    workshop.getBusinessName(), opportunity.getId(), serviceRequest.getId());

            opportunity.setStatus(OpportunityStatus.LOST);
            leadOpportunityRepository.save(opportunity);

            savedPayment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            workshopPaymentRepository.save(savedPayment);

            auditService.recordEvent(
                    MarketplaceEventType.OPPORTUNITY_LOST,
                    serviceRequest.getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Opportunity #" + opportunity.getId() + " lost - service request already claimed by another workshop",
                    null
            );
            auditService.recordEvent(
                    MarketplaceEventType.PAYMENT_REFUND_ELIGIBLE,
                    serviceRequest.getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Payment #" + savedPayment.getId() + " is eligible for refund due to race condition loss",
                    null
            );

            // Automatic refund creation and wallet credit
            refundService.createRefundRecord(savedPayment, RefundReason.OPPORTUNITY_ALREADY_ASSIGNED);

            return mapToResponse(savedPayment);
        }
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public WorkshopPaymentResponse verifyAndClaimRazorpayPayment(User currentUser, Long paymentId, VerifyPaymentRequest verifyRequest) {
        Workshop workshop = resolveWorkshop(currentUser);

        WorkshopPayment payment = workshopPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (!payment.getWorkshop().getId().equals(workshop.getId())) {
            throw new IllegalArgumentException("You are not authorized to verify this payment");
        }

        LeadOpportunity opportunity = payment.getOpportunity();
        ServiceRequest serviceRequest = opportunity.getServiceRequest();

        if (!paymentGateway.isAvailable()) {
            throw new PaymentGatewayUnavailableException("Razorpay payment gateway is currently unavailable.");
        }

        // Verify that the order ID matches the payment's stored order ID
        if (payment.getRazorpayOrderId() == null || !payment.getRazorpayOrderId().equals(verifyRequest.getRazorpayOrderId())) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Razorpay order ID mismatch");
            workshopPaymentRepository.save(payment);
            auditService.recordEvent(
                    MarketplaceEventType.PAYMENT_FAILED,
                    opportunity.getServiceRequest().getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Razorpay payment verification failed: order ID mismatch",
                    null
            );
            throw new IllegalArgumentException("Payment verification failed: order ID mismatch");
        }

        // Verify Razorpay signature
        boolean isValid = paymentGateway.verifySignature(
                verifyRequest.getRazorpayOrderId(),
                verifyRequest.getRazorpayPaymentId(),
                verifyRequest.getRazorpaySignature()
        );

        if (!isValid) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid Razorpay signature");
            workshopPaymentRepository.save(payment);
            auditService.recordEvent(
                    MarketplaceEventType.PAYMENT_FAILED,
                    opportunity.getServiceRequest().getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Razorpay payment verification failed: invalid signature",
                    null
            );
            throw new IllegalArgumentException("Payment verification failed: invalid signature");
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setRazorpayPaymentId(verifyRequest.getRazorpayPaymentId());
        payment.setRazorpaySignature(verifyRequest.getRazorpaySignature());
        payment.setPaidAt(now);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        WorkshopPayment savedPayment = workshopPaymentRepository.save(payment);

        // Backward compatible LeadPayment record
        LeadPayment leadPayment = new LeadPayment(
                opportunity,
                LeadPaymentMethod.DIRECT,
                payment.getAmount(),
                verifyRequest.getRazorpayPaymentId(),
                LeadPaymentStatus.SUCCESS
        );
        leadPayment.setPaidAt(now);
        leadPaymentRepository.save(leadPayment);
        opportunity.setPayment(leadPayment);

        // ATOMIC CLAIM AT DATABASE LEVEL
        int claimedRows = serviceRequestRepository.claimServiceRequest(
                serviceRequest.getId(),
                workshop,
                ServiceRequestStatus.ACCEPTED
        );

        if (claimedRows == 1) {
            // Workshop won!
            log.info("Workshop {} successfully claimed service request #{} via Razorpay payment", workshop.getBusinessName(), serviceRequest.getId());
            serviceRequest.setAssignedWorkshop(workshop);
            serviceRequest.setStatus(ServiceRequestStatus.ACCEPTED);
            opportunity.setStatus(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED);
            opportunity.setPaidAt(now);
            opportunity.setUnlockedAt(now);
            leadOpportunityRepository.save(opportunity);

            // Cancel competing unaccepted / unpaid opportunities (leave PAYMENT_PENDING so race-condition handling can refund them)
            List<LeadOpportunity> competing = leadOpportunityRepository.findByServiceRequestId(serviceRequest.getId());
            for (LeadOpportunity comp : competing) {
                if (!comp.getId().equals(opportunity.getId())
                        && (comp.getStatus() == OpportunityStatus.AVAILABLE
                        || comp.getStatus() == OpportunityStatus.VIEWED
                        || comp.getStatus() == OpportunityStatus.ACCEPTED)) {
                    comp.setStatus(OpportunityStatus.CANCELLED);
                    leadOpportunityRepository.save(comp);
                }
            }

            auditService.recordEvent(
                    MarketplaceEventType.PAYMENT_SUCCESS,
                    serviceRequest.getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Razorpay payment of ₹" + payment.getAmount() + " verified for opportunity #" + opportunity.getId(),
                    "{\"method\": \"RAZORPAY\", \"paymentId\": \"" + verifyRequest.getRazorpayPaymentId() + "\"}"
            );
            auditService.recordEvent(
                    MarketplaceEventType.DETAILS_UNLOCKED,
                    serviceRequest.getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Customer contact details unlocked for " + workshop.getBusinessName(),
                    null
            );
            auditService.recordEvent(
                    MarketplaceEventType.ASSIGNED_TO_WORKSHOP,
                    serviceRequest.getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Service request " + serviceRequest.getRequestReference() + " assigned to " + workshop.getBusinessName(),
                    null
            );

            // Initialize Workshop Job
            workshopJobService.createJobForAssignedOpportunity(serviceRequest, workshop, opportunity);

            // Confirm linked Booking if present
            if (serviceRequest.getBooking() != null) {
                com.carservice.backend.booking.entity.Booking linkedBooking = serviceRequest.getBooking();
                if (linkedBooking.getStatus() == com.carservice.backend.booking.enums.BookingStatus.PENDING) {
                    linkedBooking.setStatus(com.carservice.backend.booking.enums.BookingStatus.CONFIRMED);
                    bookingRepository.save(linkedBooking);
                }
            }

            return mapToResponse(savedPayment);
        } else {
            // RACE CONDITION: Another workshop claimed the request first!
            log.warn("Race condition lost: Workshop {} verified Razorpay payment for opportunity #{} but service request #{} was already claimed",
                    workshop.getBusinessName(), opportunity.getId(), serviceRequest.getId());

            opportunity.setStatus(OpportunityStatus.LOST);
            leadOpportunityRepository.save(opportunity);

            savedPayment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            workshopPaymentRepository.save(savedPayment);

            auditService.recordEvent(
                    MarketplaceEventType.OPPORTUNITY_LOST,
                    serviceRequest.getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Opportunity #" + opportunity.getId() + " lost - service request already claimed by another workshop",
                    null
            );
            auditService.recordEvent(
                    MarketplaceEventType.PAYMENT_REFUND_ELIGIBLE,
                    serviceRequest.getId(),
                    opportunity.getId(),
                    workshop.getId(),
                    currentUser.getId(),
                    "Razorpay payment #" + savedPayment.getId() + " is eligible for refund due to race condition loss",
                    null
            );

            // Create refund record
            refundService.createRefundRecord(savedPayment, RefundReason.OPPORTUNITY_ALREADY_ASSIGNED);

            // Reload payment record from database to reflect updated status (REFUNDED, REFUND_PENDING, or REFUND_FAILED)
            savedPayment = workshopPaymentRepository.findById(savedPayment.getId()).orElse(savedPayment);

            return mapToResponse(savedPayment);
        }
    }

    @Transactional
    public WorkshopPaymentResponse cancelPayment(User currentUser, Long paymentId) {
        Workshop workshop = resolveWorkshop(currentUser);

        WorkshopPayment payment = workshopPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (!payment.getWorkshop().getId().equals(workshop.getId())) {
            throw new IllegalArgumentException("You are not authorized to cancel this payment");
        }

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS || payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalArgumentException("Cannot cancel a completed or refunded payment");
        }

        payment.setPaymentStatus(PaymentStatus.CANCELLED);
        WorkshopPayment saved = workshopPaymentRepository.save(payment);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public WorkshopPaymentResponse getPaymentDetails(User currentUser, Long paymentId) {
        WorkshopPayment payment = workshopPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (currentUser.getRole() != UserRole.ADMIN) {
            Workshop workshop = resolveWorkshop(currentUser);
            if (!payment.getWorkshop().getId().equals(workshop.getId())) {
                throw new IllegalArgumentException("You are not authorized to view this payment");
            }
        }

        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<WorkshopPaymentResponse> getPaymentsForOpportunity(User currentUser, Long opportunityId) {
        LeadOpportunity opportunity = leadOpportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        if (currentUser.getRole() != UserRole.ADMIN) {
            Workshop workshop = resolveWorkshop(currentUser);
            if (!opportunity.getWorkshop().getId().equals(workshop.getId())) {
                throw new IllegalArgumentException("You are not authorized to view payments for this opportunity");
            }
        }

        return workshopPaymentRepository.findByOpportunityId(opportunityId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private InitiatePaymentResponse mapToInitiateResponse(WorkshopPayment payment) {
        String keyId = paymentGateway.getKeyId();
        return new InitiatePaymentResponse(
                payment.getId(),
                payment.getOpportunity().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getRazorpayOrderId(),
                keyId,
                payment.getIdempotencyKey(),
                payment.getCreatedAt()
        );
    }

    public WorkshopPaymentResponse mapToResponse(WorkshopPayment payment) {
        return new WorkshopPaymentResponse(
                payment.getId(),
                payment.getOpportunity().getId(),
                payment.getWorkshop().getId(),
                payment.getWorkshop().getBusinessName(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getIdempotencyKey(),
                payment.getFailureReason(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}

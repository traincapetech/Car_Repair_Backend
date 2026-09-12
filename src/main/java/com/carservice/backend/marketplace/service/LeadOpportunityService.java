package com.carservice.backend.marketplace.service;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.dto.*;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.repository.LeadOpportunityRepository;
import com.carservice.backend.marketplace.repository.LeadPaymentRepository;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.marketplace.repository.WorkshopPaymentRepository;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.marketplace.event.OpportunityRematchedEvent;
import com.carservice.backend.marketplace.event.OpportunityTransferredEvent;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.vehicle.entity.Vehicle;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeadOpportunityService {

    private final LeadOpportunityRepository leadOpportunityRepository;
    private final LeadPaymentRepository leadPaymentRepository;
    private final WorkshopRepository workshopRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WalletService walletService;
    private final MatchingEngineService matchingEngineService;
    private final PlatformConfigService platformConfigService;
    private final MarketplaceAuditService auditService;
    private final WorkshopPaymentRepository workshopPaymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public LeadOpportunityService(
            LeadOpportunityRepository leadOpportunityRepository,
            LeadPaymentRepository leadPaymentRepository,
            WorkshopRepository workshopRepository,
            ServiceRequestRepository serviceRequestRepository,
            WalletService walletService,
            MatchingEngineService matchingEngineService,
            PlatformConfigService platformConfigService,
            MarketplaceAuditService auditService,
            WorkshopPaymentRepository workshopPaymentRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.leadOpportunityRepository = leadOpportunityRepository;
        this.leadPaymentRepository = leadPaymentRepository;
        this.workshopRepository = workshopRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.walletService = walletService;
        this.matchingEngineService = matchingEngineService;
        this.platformConfigService = platformConfigService;
        this.auditService = auditService;
        this.workshopPaymentRepository = workshopPaymentRepository;
        this.eventPublisher = eventPublisher;
    }

    private Workshop resolveWorkshop(User currentUser) {
        return workshopRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No workshop partner account found for user: " + currentUser.getEmail()));
    }

    @Transactional
    public List<LeadOpportunityResponse> getOpportunitiesForWorkshop(User currentUser, OpportunityStatus statusFilter) {
        Workshop workshop = resolveWorkshop(currentUser);

        List<LeadOpportunity> list;
        if (statusFilter != null) {
            list = leadOpportunityRepository.findByWorkshopIdAndStatusOrderByCreatedAtDesc(workshop.getId(), statusFilter);
        } else {
            list = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshop.getId());
        }

        return list.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeadOpportunityResponse getOpportunityDetails(User currentUser, Long opportunityId) {
        LeadOpportunity opportunity = leadOpportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        if (currentUser.getRole() != UserRole.ADMIN) {
            Workshop workshop = resolveWorkshop(currentUser);
            if (!opportunity.getWorkshop().getId().equals(workshop.getId())) {
                throw new IllegalArgumentException("You are not authorized to view this lead opportunity");
            }
        }

        // Auto-mark as VIEWED if AVAILABLE
        if (opportunity.getStatus() == OpportunityStatus.AVAILABLE) {
            opportunity.setStatus(OpportunityStatus.VIEWED);
            opportunity.setViewedAt(LocalDateTime.now());
            opportunity = leadOpportunityRepository.save(opportunity);

            auditService.recordEvent(
                    MarketplaceEventType.OPPORTUNITY_VIEWED,
                    opportunity.getServiceRequest().getId(),
                    opportunity.getId(),
                    opportunity.getWorkshop().getId(),
                    currentUser.getId(),
                    "Workshop viewed opportunity #" + opportunity.getId(),
                    null
            );
        }

        return mapToResponse(opportunity);
    }

    @Transactional
    public LeadOpportunityResponse acceptOpportunity(User currentUser, Long opportunityId) {
        LeadOpportunity opportunity = leadOpportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        Workshop workshop = resolveWorkshop(currentUser);
        if (!opportunity.getWorkshop().getId().equals(workshop.getId())) {
            throw new IllegalArgumentException("You are not authorized to accept this lead opportunity");
        }

        if (opportunity.getStatus() != OpportunityStatus.AVAILABLE && opportunity.getStatus() != OpportunityStatus.VIEWED) {
            throw new IllegalStateException("Opportunity cannot be accepted from state: " + opportunity.getStatus());
        }

        opportunity.setStatus(OpportunityStatus.ACCEPTED);
        opportunity.setAcceptedAt(LocalDateTime.now());
        LeadOpportunity saved = leadOpportunityRepository.save(opportunity);

        auditService.recordEvent(
                MarketplaceEventType.OPPORTUNITY_ACCEPTED,
                saved.getServiceRequest().getId(),
                saved.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Workshop accepted opportunity #" + saved.getId() + ". Payment pending.",
                "{\"fee\": " + saved.getFeeSnapshot() + "}"
        );

        return mapToResponse(saved);
    }

    @Transactional
    public LeadOpportunityResponse payAndUnlockWithWallet(User currentUser, Long opportunityId) {
        LeadOpportunity opportunity = leadOpportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        Workshop workshop = resolveWorkshop(currentUser);
        if (!opportunity.getWorkshop().getId().equals(workshop.getId())) {
            throw new IllegalArgumentException("You are not authorized to pay for this lead opportunity");
        }

        if (opportunity.isCustomerDetailsUnlocked()) {
            return mapToResponse(opportunity);
        }

        if (opportunity.getStatus() == OpportunityStatus.DECLINED
                || opportunity.getStatus() == OpportunityStatus.EXPIRED
                || opportunity.getStatus() == OpportunityStatus.TRANSFERRED
                || opportunity.getStatus() == OpportunityStatus.CANCELLED) {
            throw new IllegalStateException("Cannot pay for opportunity in state: " + opportunity.getStatus());
        }

        // 1. Debit Workshop Wallet
        walletService.debitForLead(workshop, opportunity.getFeeSnapshot(), opportunity.getId());

        // 2. Create LeadPayment & WorkshopPayment
        LocalDateTime now = LocalDateTime.now();
        String txRef = "WAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LeadPayment payment = new LeadPayment(
                opportunity,
                LeadPaymentMethod.WALLET,
                opportunity.getFeeSnapshot(),
                txRef,
                LeadPaymentStatus.SUCCESS
        );
        payment.setPaidAt(now);
        leadPaymentRepository.save(payment);
        opportunity.setPayment(payment);

        WorkshopPayment wp = new WorkshopPayment(
                opportunity,
                workshop,
                opportunity.getFeeSnapshot(),
                "INR",
                PaymentMethod.WALLET,
                PaymentStatus.SUCCESS,
                txRef
        );
        wp.setPaidAt(now);
        workshopPaymentRepository.save(wp);

        // 3. Unlock Details & Assign
        opportunity.setStatus(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED);
        opportunity.setPaidAt(now);
        opportunity.setUnlockedAt(now);
        LeadOpportunity saved = leadOpportunityRepository.save(opportunity);

        // 4. Update ServiceRequest (Atomic Claim)
        ServiceRequest request = opportunity.getServiceRequest();
        serviceRequestRepository.claimServiceRequest(request.getId(), workshop, ServiceRequestStatus.ACCEPTED);
        request.setAssignedWorkshop(workshop);
        request.setStatus(ServiceRequestStatus.ACCEPTED);

        // 5. Cancel competing unaccepted/unpaid opportunities for this request
        List<LeadOpportunity> competing = leadOpportunityRepository.findByServiceRequestId(request.getId());
        for (LeadOpportunity comp : competing) {
            if (!comp.getId().equals(opportunity.getId())
                    && (comp.getStatus() == OpportunityStatus.AVAILABLE || comp.getStatus() == OpportunityStatus.VIEWED)) {
                comp.setStatus(OpportunityStatus.CANCELLED);
                leadOpportunityRepository.save(comp);
            }
        }

        // 6. Record Audits
        auditService.recordEvent(
                MarketplaceEventType.PAYMENT_SUCCESS,
                request.getId(),
                saved.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Wallet payment of ₹" + saved.getFeeSnapshot() + " successful for opportunity #" + saved.getId(),
                "{\"method\": \"WALLET\", \"reference\": \"" + txRef + "\"}"
        );
        auditService.recordEvent(
                MarketplaceEventType.DETAILS_UNLOCKED,
                request.getId(),
                saved.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Customer contact & location details unlocked for workshop " + workshop.getBusinessName(),
                null
        );
        auditService.recordEvent(
                MarketplaceEventType.ASSIGNED_TO_WORKSHOP,
                request.getId(),
                saved.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Service request " + request.getRequestReference() + " officially assigned to " + workshop.getBusinessName(),
                null
        );

        return mapToResponse(saved);
    }

    @Transactional
    public LeadOpportunityResponse payAndUnlockDirect(User currentUser, Long opportunityId, DirectPaymentRequest paymentRequest) {
        LeadOpportunity opportunity = leadOpportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        Workshop workshop = resolveWorkshop(currentUser);
        if (!opportunity.getWorkshop().getId().equals(workshop.getId())) {
            throw new IllegalArgumentException("You are not authorized to pay for this lead opportunity");
        }

        if (opportunity.isCustomerDetailsUnlocked()) {
            return mapToResponse(opportunity);
        }

        if (opportunity.getStatus() == OpportunityStatus.DECLINED
                || opportunity.getStatus() == OpportunityStatus.EXPIRED
                || opportunity.getStatus() == OpportunityStatus.TRANSFERRED
                || opportunity.getStatus() == OpportunityStatus.CANCELLED) {
            throw new IllegalStateException("Cannot pay for opportunity in state: " + opportunity.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        String txRef = (paymentRequest.getTransactionReference() != null && !paymentRequest.getTransactionReference().isBlank())
                ? paymentRequest.getTransactionReference()
                : "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        LeadPayment payment = new LeadPayment(
                opportunity,
                paymentRequest.getPaymentMethod(),
                opportunity.getFeeSnapshot(),
                txRef,
                LeadPaymentStatus.SUCCESS
        );
        payment.setPaidAt(now);
        leadPaymentRepository.save(payment);
        opportunity.setPayment(payment);

        WorkshopPayment wp = new WorkshopPayment(
                opportunity,
                workshop,
                opportunity.getFeeSnapshot(),
                "INR",
                paymentRequest.getPaymentMethod() == LeadPaymentMethod.WALLET ? PaymentMethod.WALLET : PaymentMethod.RAZORPAY,
                PaymentStatus.SUCCESS,
                txRef
        );
        wp.setPaidAt(now);
        wp.setRazorpayPaymentId(paymentRequest.getTransactionReference());
        workshopPaymentRepository.save(wp);

        opportunity.setStatus(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED);
        opportunity.setPaidAt(now);
        opportunity.setUnlockedAt(now);
        LeadOpportunity saved = leadOpportunityRepository.save(opportunity);

        // Update Service Request (Atomic Claim)
        ServiceRequest request = opportunity.getServiceRequest();
        serviceRequestRepository.claimServiceRequest(request.getId(), workshop, ServiceRequestStatus.ACCEPTED);
        request.setAssignedWorkshop(workshop);
        request.setStatus(ServiceRequestStatus.ACCEPTED);

        // Cancel other opportunities
        List<LeadOpportunity> competing = leadOpportunityRepository.findByServiceRequestId(request.getId());
        for (LeadOpportunity comp : competing) {
            if (!comp.getId().equals(opportunity.getId())
                    && (comp.getStatus() == OpportunityStatus.AVAILABLE || comp.getStatus() == OpportunityStatus.VIEWED)) {
                comp.setStatus(OpportunityStatus.CANCELLED);
                leadOpportunityRepository.save(comp);
            }
        }

        auditService.recordEvent(
                MarketplaceEventType.PAYMENT_SUCCESS,
                request.getId(),
                saved.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Direct payment of ₹" + saved.getFeeSnapshot() + " via " + paymentRequest.getPaymentMethod() + " successful",
                "{\"method\": \"" + paymentRequest.getPaymentMethod() + "\", \"reference\": \"" + txRef + "\"}"
        );
        auditService.recordEvent(
                MarketplaceEventType.DETAILS_UNLOCKED,
                request.getId(),
                saved.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Customer contact details unlocked for " + workshop.getBusinessName(),
                null
        );
        auditService.recordEvent(
                MarketplaceEventType.ASSIGNED_TO_WORKSHOP,
                request.getId(),
                saved.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Service request " + request.getRequestReference() + " assigned to " + workshop.getBusinessName(),
                null
        );

        return mapToResponse(saved);
    }

    @Transactional
    public LeadOpportunityResponse transferOpportunity(User currentUser, Long opportunityId, TransferOpportunityRequest transferRequest) {
        if (transferRequest == null || transferRequest.getReason() == null || transferRequest.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Transfer reason is required");
        }

        if (!TransferReason.isValid(transferRequest.getReason())) {
            throw new IllegalArgumentException("Invalid transfer reason: " + transferRequest.getReason()
                    + ". Valid reasons are: WORKSHOP_AT_CAPACITY, PARTS_UNAVAILABLE, OUTSIDE_SERVICE_RADIUS, SPECIALIZED_EQUIPMENT_REQUIRED, OTHER");
        }

        LeadOpportunity opportunity = leadOpportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        Workshop workshop = resolveWorkshop(currentUser);
        if (!opportunity.getWorkshop().getId().equals(workshop.getId())) {
            throw new IllegalArgumentException("You are not authorized to transfer this lead opportunity");
        }

        if (opportunity.getStatus() == OpportunityStatus.TRANSFERRED) {
            throw new IllegalArgumentException("Opportunity has already been transferred");
        }

        if (opportunity.getStatus() == OpportunityStatus.LOST
                || opportunity.getStatus() == OpportunityStatus.CANCELLED
                || opportunity.getStatus() == OpportunityStatus.DECLINED
                || opportunity.getStatus() == OpportunityStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot transfer opportunity in status: " + opportunity.getStatus());
        }

        String baseReason = transferRequest.getReason().trim();
        String recordedReason = baseReason;
        if (transferRequest.getNotes() != null && !transferRequest.getNotes().trim().isEmpty()) {
            recordedReason = baseReason + ": " + transferRequest.getNotes().trim();
        }
        if (recordedReason.length() > 255) {
            recordedReason = recordedReason.substring(0, 255);
        }

        LocalDateTime now = LocalDateTime.now();
        int updatedRows = leadOpportunityRepository.markAsTransferred(
                opportunity.getId(),
                OpportunityStatus.TRANSFERRED,
                now,
                recordedReason
        );

        if (updatedRows == 0) {
            LeadOpportunity latest = leadOpportunityRepository.findById(opportunityId).orElse(opportunity);
            if (latest.getStatus() == OpportunityStatus.TRANSFERRED) {
                throw new IllegalArgumentException("Opportunity has already been transferred");
            }
            throw new IllegalArgumentException("Cannot transfer opportunity in status: " + latest.getStatus());
        }

        opportunity.setStatus(OpportunityStatus.TRANSFERRED);
        opportunity.setTransferredAt(now);
        opportunity.setTransferReason(recordedReason);

        auditService.recordEvent(
                MarketplaceEventType.TRANSFER_REQUESTED,
                opportunity.getServiceRequest().getId(),
                opportunity.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Workshop " + workshop.getBusinessName() + " requested transfer: " + recordedReason,
                "{\"reason\": \"" + baseReason + "\", \"notes\": \"" + (transferRequest.getNotes() != null ? transferRequest.getNotes().trim() : "") + "\"}"
        );
        auditService.recordEvent(
                MarketplaceEventType.OPPORTUNITY_TRANSFERRED,
                opportunity.getServiceRequest().getId(),
                opportunity.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Opportunity #" + opportunity.getId() + " marked TRANSFERRED",
                "{\"transferredAt\": \"" + now + "\"}"
        );

        // Re-matching logic
        Long serviceRequestId = opportunity.getServiceRequest().getId();
        ServiceRequest request = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found"));
        if (request.getAssignedWorkshop() != null && request.getAssignedWorkshop().getId().equals(workshop.getId())) {
            request.setAssignedWorkshop(null);
        }
        request.setStatus(ServiceRequestStatus.RE_MATCHING);
        serviceRequestRepository.save(request);

        // Exclude workshops that have transferred, declined, or currently hold an active opportunity
        List<LeadOpportunity> existingOpportunities = leadOpportunityRepository.findByServiceRequestId(request.getId());
        Set<Long> excludedWorkshopIds = new HashSet<>();
        excludedWorkshopIds.add(workshop.getId());

        for (LeadOpportunity opp : existingOpportunities) {
            if (opp.getStatus() == OpportunityStatus.TRANSFERRED || opp.getStatus() == OpportunityStatus.DECLINED) {
                excludedWorkshopIds.add(opp.getWorkshop().getId());
            }
            if (opp.getStatus() == OpportunityStatus.AVAILABLE
                    || opp.getStatus() == OpportunityStatus.VIEWED
                    || opp.getStatus() == OpportunityStatus.ACCEPTED
                    || opp.getStatus() == OpportunityStatus.PAYMENT_PENDING
                    || opp.getStatus() == OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED) {
                excludedWorkshopIds.add(opp.getWorkshop().getId());
            }
        }

        List<Workshop> newEligibleWorkshops = matchingEngineService.findEligibleWorkshops(request, excludedWorkshopIds);
        BigDecimal currentFee = platformConfigService.getLeadAcceptanceFee();

        List<Long> newWorkshopIds = new ArrayList<>();
        List<Long> newOpportunityIds = new ArrayList<>();

        if (!newEligibleWorkshops.isEmpty()) {
            for (Workshop newWorkshop : newEligibleWorkshops) {
                LeadOpportunity newOpportunity = new LeadOpportunity(request, newWorkshop, currentFee);
                LeadOpportunity savedNewOpp = leadOpportunityRepository.save(newOpportunity);
                newWorkshopIds.add(newWorkshop.getId());
                newOpportunityIds.add(savedNewOpp.getId());

                auditService.recordEvent(
                        MarketplaceEventType.OPPORTUNITY_CREATED,
                        request.getId(),
                        savedNewOpp.getId(),
                        newWorkshop.getId(),
                        null,
                        "Re-matched opportunity generated for " + newWorkshop.getBusinessName() + " with fee ₹" + currentFee,
                        "{\"feeSnapshot\": " + currentFee + ", \"transferredFromWorkshopId\": " + workshop.getId() + ", \"transferredFromOpportunityId\": " + opportunity.getId() + "}"
                );
            }

            request.setStatus(ServiceRequestStatus.MATCHED);
            serviceRequestRepository.save(request);

            auditService.recordEvent(
                    MarketplaceEventType.RE_MATCHED,
                    request.getId(),
                    null,
                    null,
                    currentUser.getId(),
                    "Re-matched " + newEligibleWorkshops.size() + " new workshops for request " + request.getRequestReference() + " following transfer by " + workshop.getBusinessName(),
                    "{\"newWorkshopsCount\": " + newEligibleWorkshops.size() + ", \"transferredFromWorkshopId\": " + workshop.getId() + ", \"newOpportunityIds\": " + newOpportunityIds + "}"
            );
        } else {
            auditService.recordEvent(
                    MarketplaceEventType.RE_MATCHED,
                    request.getId(),
                    null,
                    null,
                    currentUser.getId(),
                    "No eligible replacement workshops available for request " + request.getRequestReference() + " following transfer. Status kept as RE_MATCHING.",
                    "{\"newWorkshopsCount\": 0, \"transferredFromWorkshopId\": " + workshop.getId() + "}"
            );
        }

        // Publish decoupled Spring application domain events
        eventPublisher.publishEvent(new OpportunityTransferredEvent(
                opportunity.getId(),
                request.getId(),
                workshop.getId(),
                recordedReason,
                now
        ));
        if (!newOpportunityIds.isEmpty()) {
            eventPublisher.publishEvent(new OpportunityRematchedEvent(
                    request.getId(),
                    workshop.getId(),
                    newWorkshopIds,
                    newOpportunityIds
            ));
        }

        return mapToResponse(opportunity);
    }

    public LeadOpportunityResponse mapToResponse(LeadOpportunity opp) {
        LeadOpportunityResponse response = new LeadOpportunityResponse();
        response.setId(opp.getId());

        ServiceRequest req = opp.getServiceRequest();
        response.setServiceRequestId(req.getId());
        response.setRequestReference(req.getRequestReference());

        Workshop w = opp.getWorkshop();
        response.setWorkshopId(w.getId());
        response.setWorkshopName(w.getBusinessName());

        response.setFeeSnapshot(opp.getFeeSnapshot());
        response.setStatus(opp.getStatus());
        response.setViewedAt(opp.getViewedAt());
        response.setAcceptedAt(opp.getAcceptedAt());
        response.setPaidAt(opp.getPaidAt());
        response.setUnlockedAt(opp.getUnlockedAt());
        response.setTransferredAt(opp.getTransferredAt());
        response.setTransferReason(opp.getTransferReason());

        response.setPreferredDate(req.getPreferredDate());
        response.setPreferredTimeSlot(req.getPreferredTimeSlot());
        response.setCity(req.getCity());
        response.setPincode(req.getPincode());
        response.setLatitude(req.getLatitude());
        response.setLongitude(req.getLongitude());
        response.setCustomerNotes(req.getCustomerNotes());

        Vehicle v = req.getVehicle();
        response.setVehicleSummary(v.getMake() + " " + v.getModel() + " (" + v.getYear() + ") - "
                + v.getRegistrationNumber() + " [" + v.getFuelType() + "]");

        List<ServiceRequestItemResponse> serviceResponses = req.getItems().stream()
                .map(i -> new ServiceRequestItemResponse(
                        i.getId(),
                        i.getServiceCatalog() != null ? i.getServiceCatalog().getId() : null,
                        i.getServiceNameSnapshot(),
                        i.getBasePriceSnapshot(),
                        i.getDiscountTypeSnapshot(),
                        i.getDiscountValueSnapshot(),
                        i.getFinalPriceSnapshot()
                ))
                .collect(Collectors.toList());
        response.setRequestedServices(serviceResponses);
        response.setTotalServiceAmount(req.getTotalAmount());

        boolean unlocked = opp.isCustomerDetailsUnlocked();
        response.setCustomerDetailsUnlocked(unlocked);

        User customerUser = req.getUser();
        if (unlocked) {
            response.setCustomerProfile(CustomerProfileDto.full(
                    customerUser.getName(),
                    customerUser.getPhone(),
                    customerUser.getEmail(),
                    req.getAddress(),
                    req.getCity(),
                    req.getPincode()
            ));
        } else {
            response.setCustomerProfile(CustomerProfileDto.masked(
                    req.getCity(),
                    req.getPincode()
            ));
        }

        if (opp.getPayment() != null) {
            LeadPayment p = opp.getPayment();
            response.setPayment(new LeadPaymentResponse(
                    p.getId(),
                    p.getPaymentMethod(),
                    p.getAmount(),
                    p.getTransactionReference(),
                    p.getStatus(),
                    p.getPaidAt()
            ));
        }

        response.setCreatedAt(opp.getCreatedAt());
        return response;
    }
}

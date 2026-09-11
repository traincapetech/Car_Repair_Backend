package com.carservice.backend.marketplace.service;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.dto.*;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.repository.LeadOpportunityRepository;
import com.carservice.backend.marketplace.repository.LeadPaymentRepository;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.vehicle.entity.Vehicle;
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

    public LeadOpportunityService(
            LeadOpportunityRepository leadOpportunityRepository,
            LeadPaymentRepository leadPaymentRepository,
            WorkshopRepository workshopRepository,
            ServiceRequestRepository serviceRequestRepository,
            WalletService walletService,
            MatchingEngineService matchingEngineService,
            PlatformConfigService platformConfigService,
            MarketplaceAuditService auditService
    ) {
        this.leadOpportunityRepository = leadOpportunityRepository;
        this.leadPaymentRepository = leadPaymentRepository;
        this.workshopRepository = workshopRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.walletService = walletService;
        this.matchingEngineService = matchingEngineService;
        this.platformConfigService = platformConfigService;
        this.auditService = auditService;
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

        // 2. Create LeadPayment
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

        // 3. Unlock Details & Assign
        opportunity.setStatus(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED);
        opportunity.setPaidAt(now);
        opportunity.setUnlockedAt(now);
        LeadOpportunity saved = leadOpportunityRepository.save(opportunity);

        // 4. Update ServiceRequest
        ServiceRequest request = opportunity.getServiceRequest();
        request.setAssignedWorkshop(workshop);
        request.setStatus(ServiceRequestStatus.ACCEPTED);
        serviceRequestRepository.save(request);

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

        opportunity.setStatus(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED);
        opportunity.setPaidAt(now);
        opportunity.setUnlockedAt(now);
        LeadOpportunity saved = leadOpportunityRepository.save(opportunity);

        // Update Service Request
        ServiceRequest request = opportunity.getServiceRequest();
        request.setAssignedWorkshop(workshop);
        request.setStatus(ServiceRequestStatus.ACCEPTED);
        serviceRequestRepository.save(request);

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
        LeadOpportunity opportunity = leadOpportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        Workshop workshop = resolveWorkshop(currentUser);
        if (!opportunity.getWorkshop().getId().equals(workshop.getId())) {
            throw new IllegalArgumentException("You are not authorized to transfer this lead opportunity");
        }

        if (opportunity.getStatus() == OpportunityStatus.TRANSFERRED) {
            throw new IllegalStateException("Opportunity has already been transferred");
        }

        LocalDateTime now = LocalDateTime.now();
        opportunity.setStatus(OpportunityStatus.TRANSFERRED);
        opportunity.setTransferredAt(now);
        opportunity.setTransferReason(transferRequest.getReason());
        LeadOpportunity saved = leadOpportunityRepository.save(opportunity);

        auditService.recordEvent(
                MarketplaceEventType.TRANSFER_REQUESTED,
                opportunity.getServiceRequest().getId(),
                saved.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Workshop " + workshop.getBusinessName() + " requested transfer: " + transferRequest.getReason(),
                "{\"reason\": \"" + transferRequest.getReason() + "\"}"
        );
        auditService.recordEvent(
                MarketplaceEventType.OPPORTUNITY_TRANSFERRED,
                opportunity.getServiceRequest().getId(),
                saved.getId(),
                workshop.getId(),
                currentUser.getId(),
                "Opportunity #" + saved.getId() + " marked TRANSFERRED",
                null
        );

        // Re-matching logic
        ServiceRequest request = opportunity.getServiceRequest();
        request.setAssignedWorkshop(null);
        request.setStatus(ServiceRequestStatus.RE_MATCHING);
        serviceRequestRepository.save(request);

        // Exclude workshops that have transferred or declined this request
        List<LeadOpportunity> existingOpportunities = leadOpportunityRepository.findByServiceRequestId(request.getId());
        Set<Long> excludedWorkshopIds = existingOpportunities.stream()
                .filter(o -> o.getStatus() == OpportunityStatus.TRANSFERRED || o.getStatus() == OpportunityStatus.DECLINED)
                .map(o -> o.getWorkshop().getId())
                .collect(Collectors.toSet());
        excludedWorkshopIds.add(workshop.getId());

        List<Workshop> newEligibleWorkshops = matchingEngineService.findEligibleWorkshops(request, excludedWorkshopIds);
        BigDecimal currentFee = platformConfigService.getLeadAcceptanceFee();

        if (!newEligibleWorkshops.isEmpty()) {
            for (Workshop newWorkshop : newEligibleWorkshops) {
                LeadOpportunity newOpportunity = new LeadOpportunity(request, newWorkshop, currentFee);
                LeadOpportunity savedNewOpp = leadOpportunityRepository.save(newOpportunity);

                auditService.recordEvent(
                        MarketplaceEventType.OPPORTUNITY_CREATED,
                        request.getId(),
                        savedNewOpp.getId(),
                        newWorkshop.getId(),
                        null,
                        "Re-matched opportunity generated for " + newWorkshop.getBusinessName() + " with fee ₹" + currentFee,
                        "{\"feeSnapshot\": " + currentFee + "}"
                );
            }

            request.setStatus(ServiceRequestStatus.MATCHED);
            serviceRequestRepository.save(request);

            auditService.recordEvent(
                    MarketplaceEventType.RE_MATCHED,
                    request.getId(),
                    null,
                    null,
                    null,
                    "Re-matched " + newEligibleWorkshops.size() + " new workshops for request " + request.getRequestReference(),
                    "{\"newWorkshopsCount\": " + newEligibleWorkshops.size() + "}"
            );
        }

        return mapToResponse(saved);
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

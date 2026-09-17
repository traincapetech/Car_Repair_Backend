package com.carservice.backend.admin.service;

import com.carservice.backend.admin.dto.*;
import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.dto.RefundResponse;
import com.carservice.backend.marketplace.dto.ServiceRequestItemResponse;
import com.carservice.backend.marketplace.dto.WorkshopPaymentResponse;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminMarketplaceService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final LeadOpportunityRepository leadOpportunityRepository;
    private final WorkshopPaymentRepository workshopPaymentRepository;
    private final RefundRepository refundRepository;
    private final MarketplaceAuditEventRepository auditEventRepository;
    private final WorkshopRepository workshopRepository;

    public AdminMarketplaceService(
            ServiceRequestRepository serviceRequestRepository,
            LeadOpportunityRepository leadOpportunityRepository,
            WorkshopPaymentRepository workshopPaymentRepository,
            RefundRepository refundRepository,
            MarketplaceAuditEventRepository auditEventRepository,
            WorkshopRepository workshopRepository
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.leadOpportunityRepository = leadOpportunityRepository;
        this.workshopPaymentRepository = workshopPaymentRepository;
        this.refundRepository = refundRepository;
        this.auditEventRepository = auditEventRepository;
        this.workshopRepository = workshopRepository;
    }

    /**
     * Aggregates real-time marketplace KPIs across service requests, opportunities, payments, and refunds.
     */
    public AdminMarketplaceSummaryResponse getMarketplaceSummary() {
        AdminMarketplaceSummaryResponse summary = new AdminMarketplaceSummaryResponse();

        // Service Requests
        summary.setTotalServiceRequests(serviceRequestRepository.count());
        summary.setActiveRequests(serviceRequestRepository.countByStatusIn(List.of(
                ServiceRequestStatus.SUBMITTED,
                ServiceRequestStatus.MATCHED,
                ServiceRequestStatus.ACCEPTED,
                ServiceRequestStatus.IN_PROGRESS,
                ServiceRequestStatus.RE_MATCHING
        )));
        summary.setMatchingRequests(serviceRequestRepository.countByStatusIn(List.of(
                ServiceRequestStatus.SUBMITTED,
                ServiceRequestStatus.MATCHED,
                ServiceRequestStatus.RE_MATCHING
        )));
        summary.setAssignedRequests(serviceRequestRepository.countByStatusIn(List.of(
                ServiceRequestStatus.ACCEPTED,
                ServiceRequestStatus.IN_PROGRESS,
                ServiceRequestStatus.COMPLETED
        )));
        summary.setCompletedRequests(serviceRequestRepository.countByStatus(ServiceRequestStatus.COMPLETED));
        summary.setCancelledRequests(serviceRequestRepository.countByStatus(ServiceRequestStatus.CANCELLED));

        // Opportunities
        summary.setTotalOpportunities(leadOpportunityRepository.count());
        summary.setAvailableOpportunities(leadOpportunityRepository.countByStatus(OpportunityStatus.AVAILABLE));
        summary.setAcceptedOpportunities(leadOpportunityRepository.countByStatusIn(List.of(
                OpportunityStatus.ACCEPTED,
                OpportunityStatus.PAID,
                OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED,
                OpportunityStatus.ASSIGNED,
                OpportunityStatus.COMPLETED
        )));
        summary.setTransferredOpportunities(leadOpportunityRepository.countByStatus(OpportunityStatus.TRANSFERRED));
        summary.setLostOpportunities(leadOpportunityRepository.countByStatus(OpportunityStatus.LOST));

        // Payments & Revenue
        summary.setTotalSuccessfulPayments(workshopPaymentRepository.countByPaymentStatus(PaymentStatus.SUCCESS));
        BigDecimal revenue = workshopPaymentRepository.sumSuccessfulPayments();
        summary.setTotalAcceptanceRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        // Refunds
        summary.setTotalRefunds(refundRepository.count());
        BigDecimal refunded = refundRepository.sumProcessedRefunds();
        summary.setTotalRefundedAmount(refunded != null ? refunded : BigDecimal.ZERO);
        summary.setPendingRefunds(refundRepository.countByRefundStatus(RefundStatus.INITIATED));

        return summary;
    }

    /**
     * Paginated, searchable, and filtered list of service requests with eager relationship joins.
     */
    public Page<AdminServiceRequestListResponse> getServiceRequests(
            String search,
            ServiceRequestStatus status,
            String city,
            Long workshopId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<ServiceRequest> requestsPage = serviceRequestRepository.findServiceRequestsWithFilter(
                search,
                status,
                city,
                workshopId,
                startDate,
                endDate,
                pageable
        );

        List<Long> requestIds = requestsPage.getContent().stream()
                .map(ServiceRequest::getId)
                .collect(Collectors.toList());

        Map<Long, Long> opportunityCounts = new HashMap<>();
        if (!requestIds.isEmpty()) {
            List<Object[]> countRows = leadOpportunityRepository.countByServiceRequestIds(requestIds);
            for (Object[] row : countRows) {
                opportunityCounts.put((Long) row[0], (Long) row[1]);
            }
        }

        return requestsPage.map(sr -> mapToListResponse(sr, opportunityCounts.getOrDefault(sr.getId(), 0L)));
    }

    /**
     * Complete 360° dossier for a service request including customer, vehicle, requested items,
     * assigned workshop, booking, and opportunity financial metrics.
     */
    public AdminServiceRequestDetailResponse getServiceRequestDetail(Long requestId) {
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + requestId));

        AdminServiceRequestDetailResponse response = new AdminServiceRequestDetailResponse();
        response.setId(sr.getId());
        response.setRequestReference(sr.getRequestReference());

        // Customer
        if (sr.getUser() != null) {
            response.setCustomerId(sr.getUser().getId());
            response.setCustomerName(sr.getUser().getName());
            response.setCustomerPhone(sr.getUser().getPhone());
            response.setCustomerEmail(sr.getUser().getEmail());
            response.setCustomerAddress(sr.getAddress());
            response.setCustomerCity(sr.getCity());
            response.setCustomerPincode(sr.getPincode());
            response.setCustomerCreatedAt(sr.getUser().getCreatedAt());
        }

        // Vehicle
        if (sr.getVehicle() != null) {
            response.setVehicleId(sr.getVehicle().getId());
            response.setVehicleMake(sr.getVehicle().getMake());
            response.setVehicleModel(sr.getVehicle().getModel());
            response.setVehicleYear(sr.getVehicle().getYear());
            response.setVehicleRegistrationNumber(sr.getVehicle().getRegistrationNumber());
            response.setVehicleFuelType(sr.getVehicle().getFuelType());
            response.setVehicleTransmission(sr.getVehicle().getTransmission());
        }

        // Location & Schedule
        response.setCity(sr.getCity());
        response.setAddress(sr.getAddress());
        response.setPincode(sr.getPincode());
        response.setLatitude(sr.getLatitude());
        response.setLongitude(sr.getLongitude());
        response.setPreferredDate(sr.getPreferredDate());
        response.setPreferredTimeSlot(sr.getPreferredTimeSlot());
        response.setCustomerNotes(sr.getCustomerNotes());

        // Financial & Status
        response.setTotalAmount(sr.getTotalAmount());
        response.setStatus(sr.getStatus());

        // Assigned Workshop
        if (sr.getAssignedWorkshop() != null) {
            Workshop w = sr.getAssignedWorkshop();
            response.setAssignedWorkshopId(w.getId());
            response.setAssignedWorkshopName(w.getBusinessName());
            response.setAssignedWorkshopPhone(w.getPhone());
            response.setAssignedWorkshopAddress(w.getAddress());
            response.setAssignedWorkshopCity(w.getCity());
        }

        // Booking & Job
        if (sr.getBooking() != null) {
            response.setBookingId(sr.getBooking().getId());
        }
        response.setBookingReference(sr.getBookingReference());
        if (sr.getCurrentJob() != null) {
            response.setCurrentJobId(sr.getCurrentJob().getId());
            response.setCurrentJobStatus(sr.getCurrentJob().getStatus());
        }

        // Items
        if (sr.getItems() != null) {
            List<ServiceRequestItemResponse> items = sr.getItems().stream()
                    .map(item -> new ServiceRequestItemResponse(
                            item.getId(),
                            item.getServiceCatalog() != null ? item.getServiceCatalog().getId() : null,
                            item.getServiceNameSnapshot(),
                            item.getBasePriceSnapshot(),
                            item.getDiscountTypeSnapshot(),
                            item.getDiscountValueSnapshot(),
                            item.getFinalPriceSnapshot()
                    ))
                    .collect(Collectors.toList());
            response.setItems(items);
        }

        // Opportunities & Financial Breakdown for this request
        List<LeadOpportunity> opportunities = leadOpportunityRepository.findByServiceRequestIdWithDetails(requestId);
        response.setTotalOpportunities(opportunities.size());
        response.setAcceptedOpportunities(opportunities.stream()
                .filter(o -> o.getStatus() == OpportunityStatus.ACCEPTED
                        || o.getStatus() == OpportunityStatus.PAID
                        || o.getStatus() == OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED
                        || o.getStatus() == OpportunityStatus.ASSIGNED
                        || o.getStatus() == OpportunityStatus.COMPLETED)
                .count());
        response.setTransferredOpportunities(opportunities.stream()
                .filter(o -> o.getStatus() == OpportunityStatus.TRANSFERRED)
                .count());
        response.setLostOpportunities(opportunities.stream()
                .filter(o -> o.getStatus() == OpportunityStatus.LOST)
                .count());

        List<WorkshopPayment> payments = workshopPaymentRepository.findByServiceRequestIdWithDetails(requestId);
        BigDecimal paidSum = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS)
                .map(WorkshopPayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalPaidAmount(paidSum);

        List<Refund> refunds = refundRepository.findByServiceRequestIdWithDetails(requestId);
        BigDecimal refundSum = refunds.stream()
                .filter(r -> r.getRefundStatus() == RefundStatus.SUCCESS)
                .map(Refund::getRefundAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalRefundedAmount(refundSum);

        response.setCreatedAt(sr.getCreatedAt());
        response.setUpdatedAt(sr.getUpdatedAt());

        return response;
    }

    /**
     * Lists all workshop opportunities generated for a specific service request.
     */
    public List<AdminMarketplaceOpportunityResponse> getOpportunitiesForRequest(Long requestId) {
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + requestId));

        List<LeadOpportunity> opportunities = leadOpportunityRepository.findByServiceRequestIdWithDetails(requestId);
        List<Refund> refunds = refundRepository.findByServiceRequestIdWithDetails(requestId);
        Map<Long, Refund> refundByOpportunity = refunds.stream()
                .filter(r -> r.getOpportunity() != null)
                .collect(Collectors.toMap(r -> r.getOpportunity().getId(), r -> r, (r1, r2) -> r1));

        return opportunities.stream()
                .map(lo -> mapToOpportunityResponse(lo, sr.getAssignedWorkshop(), refundByOpportunity.get(lo.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Returns all workshop payments associated with the opportunities of this service request.
     */
    public List<WorkshopPaymentResponse> getPaymentsForRequest(Long requestId) {
        serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + requestId));

        List<WorkshopPayment> payments = workshopPaymentRepository.findByServiceRequestIdWithDetails(requestId);
        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all refunds associated with the opportunities / payments of this service request.
     */
    public List<RefundResponse> getRefundsForRequest(Long requestId) {
        serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + requestId));

        List<Refund> refunds = refundRepository.findByServiceRequestIdWithDetails(requestId);
        return refunds.stream()
                .map(this::mapToRefundResponse)
                .collect(Collectors.toList());
    }

    /**
     * Reconstructs the chronological audit timeline for the service request from marketplace_audit_events.
     */
    public List<AdminMarketplaceTimelineEventResponse> getTimelineForRequest(Long requestId) {
        serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + requestId));

        List<MarketplaceAuditEvent> events = auditEventRepository.findByServiceRequestIdOrderByCreatedAtAsc(requestId);

        // Preload workshop names to display clean identity in timeline nodes
        Set<Long> workshopIds = events.stream()
                .map(MarketplaceAuditEvent::getWorkshopId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> workshopNameMap = new HashMap<>();
        if (!workshopIds.isEmpty()) {
            workshopRepository.findAllById(workshopIds).forEach(w -> workshopNameMap.put(w.getId(), w.getBusinessName()));
        }

        return events.stream()
                .map(e -> new AdminMarketplaceTimelineEventResponse(
                        e.getId(),
                        e.getEventType(),
                        e.getDescription(),
                        e.getActorUserId(),
                        e.getWorkshopId(),
                        e.getWorkshopId() != null ? workshopNameMap.get(e.getWorkshopId()) : null,
                        e.getLeadOpportunityId(),
                        e.getMetadataJson(),
                        e.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private AdminServiceRequestListResponse mapToListResponse(ServiceRequest sr, long opportunityCount) {
        AdminServiceRequestListResponse res = new AdminServiceRequestListResponse();
        res.setId(sr.getId());
        res.setRequestReference(sr.getRequestReference());

        if (sr.getUser() != null) {
            res.setCustomerId(sr.getUser().getId());
            res.setCustomerName(sr.getUser().getName());
            res.setCustomerPhone(sr.getUser().getPhone());
            res.setCustomerEmail(sr.getUser().getEmail());
        }

        if (sr.getVehicle() != null) {
            res.setVehicleId(sr.getVehicle().getId());
            res.setVehicleSummary(sr.getVehicle().getMake() + " " + sr.getVehicle().getModel() +
                    " (" + sr.getVehicle().getYear() + ") - " + sr.getVehicle().getRegistrationNumber());
        }

        if (sr.getItems() != null && !sr.getItems().isEmpty()) {
            res.setServiceCount(sr.getItems().size());
            res.setServiceNamesSummary(sr.getItems().stream()
                    .map(ServiceRequestItem::getServiceNameSnapshot)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", ")));
        } else {
            res.setServiceCount(0);
            res.setServiceNamesSummary("No services listed");
        }

        res.setCity(sr.getCity());
        res.setAddress(sr.getAddress());
        res.setPincode(sr.getPincode());
        res.setPreferredDate(sr.getPreferredDate());
        res.setPreferredTimeSlot(sr.getPreferredTimeSlot());
        res.setTotalAmount(sr.getTotalAmount());
        res.setStatus(sr.getStatus());

        if (sr.getAssignedWorkshop() != null) {
            res.setAssignedWorkshopId(sr.getAssignedWorkshop().getId());
            res.setAssignedWorkshopName(sr.getAssignedWorkshop().getBusinessName());
        }

        res.setOpportunityCount(opportunityCount);
        res.setBookingReference(sr.getBookingReference());
        res.setCreatedAt(sr.getCreatedAt());
        res.setUpdatedAt(sr.getUpdatedAt());

        return res;
    }

    private AdminMarketplaceOpportunityResponse mapToOpportunityResponse(
            LeadOpportunity lo,
            Workshop assignedWorkshop,
            Refund refund
    ) {
        AdminMarketplaceOpportunityResponse res = new AdminMarketplaceOpportunityResponse();
        res.setId(lo.getId());

        if (lo.getWorkshop() != null) {
            res.setWorkshopId(lo.getWorkshop().getId());
            res.setWorkshopName(lo.getWorkshop().getBusinessName());
            res.setWorkshopCity(lo.getWorkshop().getCity());
            res.setWorkshopPhone(lo.getWorkshop().getPhone());
        }

        res.setFeeSnapshot(lo.getFeeSnapshot());
        res.setStatus(lo.getStatus());
        res.setViewedAt(lo.getViewedAt());
        res.setAcceptedAt(lo.getAcceptedAt());
        res.setPaidAt(lo.getPaidAt());
        res.setUnlockedAt(lo.getUnlockedAt());
        res.setTransferredAt(lo.getTransferredAt());
        res.setTransferReason(lo.getTransferReason());

        if (lo.getPayment() != null) {
            res.setPaymentId(lo.getPayment().getId());
            res.setPaymentMethod(PaymentMethod.valueOf(lo.getPayment().getPaymentMethod().name()));
            res.setPaymentAmount(lo.getPayment().getAmount());
            res.setPaymentStatus(PaymentStatus.valueOf(lo.getPayment().getStatus().name()));
            res.setPaymentTransactionReference(lo.getPayment().getTransactionReference());
            res.setPaymentPaidAt(lo.getPayment().getPaidAt());
        }

        if (refund != null) {
            res.setRefundId(refund.getId());
            res.setRefundStatus(refund.getRefundStatus());
            res.setRefundAmount(refund.getRefundAmount());
            res.setRefundReason(refund.getRefundReason());
            res.setRefundProcessedAt(refund.getProcessedAt());
        }

        boolean assigned = assignedWorkshop != null && lo.getWorkshop() != null
                && assignedWorkshop.getId().equals(lo.getWorkshop().getId());
        res.setAssigned(assigned);
        res.setCustomerDetailsUnlocked(lo.isCustomerDetailsUnlocked());
        res.setCreatedAt(lo.getCreatedAt());
        res.setUpdatedAt(lo.getUpdatedAt());

        return res;
    }

    private WorkshopPaymentResponse mapToPaymentResponse(WorkshopPayment p) {
        return new WorkshopPaymentResponse(
                p.getId(),
                p.getOpportunity() != null ? p.getOpportunity().getId() : null,
                p.getWorkshop() != null ? p.getWorkshop().getId() : null,
                p.getWorkshop() != null ? p.getWorkshop().getBusinessName() : null,
                p.getAmount(),
                p.getCurrency(),
                p.getPaymentMethod(),
                p.getPaymentStatus(),
                p.getRazorpayOrderId(),
                p.getRazorpayPaymentId(),
                p.getIdempotencyKey(),
                p.getFailureReason(),
                p.getPaidAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private RefundResponse mapToRefundResponse(Refund r) {
        return new RefundResponse(
                r.getId(),
                r.getPayment() != null ? r.getPayment().getId() : null,
                r.getOpportunity() != null ? r.getOpportunity().getId() : null,
                r.getWorkshop() != null ? r.getWorkshop().getId() : null,
                r.getWorkshop() != null ? r.getWorkshop().getBusinessName() : null,
                r.getRefundAmount(),
                r.getRefundStatus(),
                r.getRefundReason(),
                r.getRazorpayRefundId(),
                r.getFailureReason(),
                r.getInitiatedAt(),
                r.getProcessedAt(),
                r.getCreatedAt()
        );
    }
}

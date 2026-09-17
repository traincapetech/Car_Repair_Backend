package com.carservice.backend.admin.service;

import com.carservice.backend.admin.dto.*;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.dto.RefundResponse;
import com.carservice.backend.marketplace.dto.WorkshopJobResponse;
import com.carservice.backend.marketplace.dto.WorkshopPaymentResponse;
import com.carservice.backend.marketplace.dto.WorkshopWalletResponse;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.repository.*;
import com.carservice.backend.marketplace.service.MarketplaceAuditService;
import com.carservice.backend.marketplace.service.WalletService;
import com.carservice.backend.marketplace.service.WorkshopJobService;
import com.carservice.backend.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminWorkshopService {

    private static final Logger log = LoggerFactory.getLogger(AdminWorkshopService.class);

    private final WorkshopRepository workshopRepository;
    private final WorkshopServiceRepository workshopServiceRepository;
    private final LeadOpportunityRepository leadOpportunityRepository;
    private final WorkshopJobRepository workshopJobRepository;
    private final BookingRepository bookingRepository;
    private final WorkshopWalletRepository workshopWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WorkshopPaymentRepository workshopPaymentRepository;
    private final RefundRepository refundRepository;
    private final MarketplaceAuditService marketplaceAuditService;
    private final WalletService walletService;
    private final WorkshopJobService workshopJobService;

    public AdminWorkshopService(
            WorkshopRepository workshopRepository,
            WorkshopServiceRepository workshopServiceRepository,
            LeadOpportunityRepository leadOpportunityRepository,
            WorkshopJobRepository workshopJobRepository,
            BookingRepository bookingRepository,
            WorkshopWalletRepository workshopWalletRepository,
            WalletTransactionRepository walletTransactionRepository,
            WorkshopPaymentRepository workshopPaymentRepository,
            RefundRepository refundRepository,
            MarketplaceAuditService marketplaceAuditService,
            WalletService walletService,
            WorkshopJobService workshopJobService
    ) {
        this.workshopRepository = workshopRepository;
        this.workshopServiceRepository = workshopServiceRepository;
        this.leadOpportunityRepository = leadOpportunityRepository;
        this.workshopJobRepository = workshopJobRepository;
        this.bookingRepository = bookingRepository;
        this.workshopWalletRepository = workshopWalletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.workshopPaymentRepository = workshopPaymentRepository;
        this.refundRepository = refundRepository;
        this.marketplaceAuditService = marketplaceAuditService;
        this.walletService = walletService;
        this.workshopJobService = workshopJobService;
    }

    @Transactional(readOnly = true)
    public Page<AdminWorkshopListResponse> getWorkshops(
            int page,
            int size,
            String search,
            String status,
            String verificationStatus,
            String city,
            String state,
            String activity,
            String sort,
            String direction
    ) {
        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(100, Math.max(1, size));

        Boolean activeFilter = null;
        if (status != null && !status.isBlank()) {
            String trimmed = status.trim().toUpperCase();
            if ("ACTIVE".equals(trimmed) || "TRUE".equals(trimmed)) {
                activeFilter = Boolean.TRUE;
            } else if ("INACTIVE".equals(trimmed) || "FALSE".equals(trimmed)) {
                activeFilter = Boolean.FALSE;
            }
        }

        WorkshopVerificationStatus verFilter = null;
        if (verificationStatus != null && !verificationStatus.isBlank() && !"ALL".equalsIgnoreCase(verificationStatus.trim())) {
            try {
                verFilter = WorkshopVerificationStatus.valueOf(verificationStatus.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        String searchFilter = (search != null && !search.isBlank()) ? search.trim() : null;
        String cityFilter = (city != null && !city.isBlank()) ? city.trim() : null;
        String stateFilter = (state != null && !state.isBlank()) ? state.trim() : null;

        String sortProp = "createdAt";
        if (sort != null) {
            String s = sort.trim();
            if (s.equalsIgnoreCase("businessName") || s.equalsIgnoreCase("createdAt") || s.equalsIgnoreCase("updatedAt") || s.equalsIgnoreCase("city")) {
                sortProp = s;
            }
        }

        Sort.Direction sortDirection = (direction != null && direction.trim().equalsIgnoreCase("ASC"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(sortDirection, sortProp));
        Page<Workshop> workshopPage = workshopRepository.findWorkshopsWithFilter(
                activeFilter,
                verFilter,
                cityFilter,
                stateFilter,
                searchFilter,
                pageable
        );

        if (workshopPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, workshopPage.getTotalElements());
        }

        List<Long> workshopIds = workshopPage.getContent().stream().map(Workshop::getId).toList();

        // High-performance batch queries (0 N+1 overhead)
        Map<Long, Long> oppCounts = toCountMap(leadOpportunityRepository.countOpportunitiesByWorkshopIds(workshopIds));
        Map<Long, Long> acceptedOppCounts = toCountMap(leadOpportunityRepository.countAcceptedOpportunitiesByWorkshopIds(workshopIds));
        Map<Long, Long> completedJobCounts = toCountMap(workshopJobRepository.countCompletedJobsByWorkshopIds(workshopIds));
        Map<Long, Long> capCounts = toCountMap(workshopServiceRepository.countActiveServicesByWorkshopIds(workshopIds));

        List<AdminWorkshopListResponse> list = workshopPage.getContent().stream().map(w -> new AdminWorkshopListResponse(
                w.getId(),
                w.getBusinessName(),
                w.getUser() != null ? w.getUser().getName() : null,
                w.getEmail(),
                w.getPhone(),
                w.getCity(),
                w.getState(),
                w.getPincode(),
                w.getServiceRadiusKm(),
                w.getVerificationStatus(),
                w.getIsActive(),
                w.getStatusReason(),
                capCounts.getOrDefault(w.getId(), 0L),
                oppCounts.getOrDefault(w.getId(), 0L),
                acceptedOppCounts.getOrDefault(w.getId(), 0L),
                completedJobCounts.getOrDefault(w.getId(), 0L),
                w.getCreatedAt(),
                w.getUpdatedAt()
        )).collect(Collectors.toList());

        // Marketplace activity filter application if requested
        if (activity != null && !activity.isBlank() && !"ALL".equalsIgnoreCase(activity.trim())) {
            String act = activity.trim().toUpperCase();
            if ("HAS_ACTIVE_OPPORTUNITIES".equals(act)) {
                list = list.stream().filter(w -> w.getOpportunitiesCount() > 0).collect(Collectors.toList());
            } else if ("HAS_ACCEPTED_OPPORTUNITIES".equals(act)) {
                list = list.stream().filter(w -> w.getAcceptedOpportunitiesCount() > 0).collect(Collectors.toList());
            } else if ("HAS_COMPLETED_JOBS".equals(act)) {
                list = list.stream().filter(w -> w.getCompletedJobsCount() > 0).collect(Collectors.toList());
            } else if ("NO_ACTIVITY".equals(act)) {
                list = list.stream().filter(w -> w.getOpportunitiesCount() == 0 && w.getCompletedJobsCount() == 0).collect(Collectors.toList());
            }
        }

        return new PageImpl<>(list, pageable, workshopPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminWorkshopSummaryResponse getWorkshopSummary() {
        long total = workshopRepository.count();
        long active = workshopRepository.countByIsActiveTrue();
        long pending = workshopRepository.countByVerificationStatus(WorkshopVerificationStatus.PENDING);
        long suspendedOrInactive = workshopRepository.countSuspendedOrInactive();

        return new AdminWorkshopSummaryResponse(total, active, pending, suspendedOrInactive);
    }

    @Transactional(readOnly = true)
    public AdminWorkshopDetailResponse getWorkshopDetail(Long workshopId) {
        Workshop workshop = findWorkshopOrThrow(workshopId);

        AdminWorkshopDetailResponse response = new AdminWorkshopDetailResponse();
        response.setId(workshop.getId());
        response.setBusinessName(workshop.getBusinessName());

        if (workshop.getUser() != null) {
            response.setOwnerId(workshop.getUser().getId());
            response.setOwnerName(workshop.getUser().getName());
            response.setEmail(workshop.getUser().getEmail());
            response.setPhone(workshop.getUser().getPhone());
        } else {
            response.setEmail(workshop.getEmail());
            response.setPhone(workshop.getPhone());
        }

        response.setAddress(workshop.getAddress());
        response.setCity(workshop.getCity());
        response.setState(workshop.getState());
        response.setPincode(workshop.getPincode());
        response.setLatitude(workshop.getLatitude());
        response.setLongitude(workshop.getLongitude());
        response.setServiceRadiusKm(workshop.getServiceRadiusKm());

        response.setVerificationStatus(workshop.getVerificationStatus());
        response.setIsActive(workshop.getIsActive());
        response.setStatusReason(workshop.getStatusReason());
        response.setCreatedAt(workshop.getCreatedAt());
        response.setUpdatedAt(workshop.getUpdatedAt());

        // Wallet Summary
        Optional<WorkshopWallet> walletOpt = workshopWalletRepository.findByWorkshopId(workshopId);
        if (walletOpt.isPresent()) {
            WorkshopWallet wallet = walletOpt.get();
            response.setWalletBalance(wallet.getBalance());
            response.setWalletCurrency(wallet.getCurrency());
            response.setWalletStatus(wallet.getStatus());

            List<WalletTransaction> transactions = walletTransactionRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopId);
            response.setTotalWalletTransactions(transactions.size());

            BigDecimal credits = BigDecimal.ZERO;
            BigDecimal debits = BigDecimal.ZERO;
            for (WalletTransaction t : transactions) {
                if (t.getType() == WalletTransactionType.CREDIT) {
                    credits = credits.add(t.getAmount());
                } else if (t.getType() == WalletTransactionType.DEBIT) {
                    debits = debits.add(t.getAmount());
                }
            }
            response.setTotalWalletCredits(credits.setScale(2, RoundingMode.HALF_UP));
            response.setTotalWalletDebits(debits.setScale(2, RoundingMode.HALF_UP));
        }

        // Marketplace performance metrics
        long available = leadOpportunityRepository.countByWorkshopIdAndStatus(workshopId, OpportunityStatus.AVAILABLE);
        long accepted = leadOpportunityRepository.countByWorkshopIdAndStatus(workshopId, OpportunityStatus.ACCEPTED);
        long unlocked = leadOpportunityRepository.countByWorkshopIdAndStatusIn(workshopId, List.of(
                OpportunityStatus.PAID,
                OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED,
                OpportunityStatus.ASSIGNED
        ));
        long transferred = leadOpportunityRepository.countByWorkshopIdAndStatus(workshopId, OpportunityStatus.TRANSFERRED);
        long lost = leadOpportunityRepository.countByWorkshopIdAndStatus(workshopId, OpportunityStatus.LOST);
        long completed = workshopJobRepository.countByWorkshopIdAndStatus(workshopId, WorkshopJobStatus.COMPLETED);
        long totalOpp = leadOpportunityRepository.countByWorkshopId(workshopId);

        response.setAvailableOpportunities(available);
        response.setAcceptedOpportunities(accepted);
        response.setUnlockedOpportunities(unlocked);
        response.setTransferredOpportunities(transferred);
        response.setLostOpportunities(lost);
        response.setCompletedJobs(completed);
        response.setTotalOpportunities(totalOpp);

        double rate = totalOpp > 0 ? ((double) (accepted + unlocked) / totalOpp) * 100.0 : 0.0;
        response.setAcceptanceRate(Math.round(rate * 10.0) / 10.0);

        // Bookings count
        Page<Booking> testPage = bookingRepository.findBookingsByWorkshopIdAndOptionalStatus(workshopId, null, PageRequest.of(0, 1));
        response.setTotalBookings(testPage.getTotalElements());

        // Capabilities
        List<WorkshopService> services = workshopServiceRepository.findByWorkshopIdWithCatalog(workshopId);
        response.setCapabilities(services.stream().map(this::mapToCapabilityResponse).collect(Collectors.toList()));

        return response;
    }

    @Transactional
    public AdminWorkshopDetailResponse updateWorkshopStatus(Long workshopId, UpdateWorkshopStatusRequest request, User adminUser) {
        Workshop workshop = findWorkshopOrThrow(workshopId);

        boolean currentActive = Boolean.TRUE.equals(workshop.getIsActive());
        boolean targetActive = Boolean.TRUE.equals(request.getIsActive());

        if (currentActive == targetActive) {
            String stateDesc = currentActive ? "ACTIVE" : "INACTIVE";
            throw new IllegalArgumentException("Workshop operational status is already " + stateDesc);
        }

        if (!targetActive && (request.getReason() == null || request.getReason().trim().isBlank())) {
            throw new IllegalArgumentException("A reason is required when deactivating a workshop");
        }

        String reason = request.getReason() != null ? request.getReason().trim() : null;
        workshop.setIsActive(targetActive);
        workshop.setStatusReason(reason);
        workshopRepository.save(workshop);

        String desc = "Workshop operational status changed to " + (targetActive ? "ACTIVE" : "INACTIVE")
                + (reason != null ? ". Reason: " + reason : "");
        String metadataJson = String.format("{\"oldStatus\":%s,\"newStatus\":%s,\"reason\":%s}",
                currentActive, targetActive, reason != null ? "\"" + reason.replace("\"", "\\\"") + "\"" : "null");

        marketplaceAuditService.recordEvent(
                MarketplaceEventType.WORKSHOP_STATUS_CHANGED,
                null,
                null,
                workshopId,
                adminUser != null ? adminUser.getId() : null,
                desc,
                metadataJson
        );

        log.info("AUDIT: Admin [{}] changed workshop [{}] status from [{}] to [{}] with reason: {}",
                adminUser != null ? adminUser.getEmail() : "SYSTEM",
                workshop.getBusinessName(),
                currentActive ? "ACTIVE" : "INACTIVE",
                targetActive ? "ACTIVE" : "INACTIVE",
                reason);

        return getWorkshopDetail(workshopId);
    }

    @Transactional
    public AdminWorkshopDetailResponse updateWorkshopVerification(Long workshopId, UpdateWorkshopVerificationRequest request, User adminUser) {
        Workshop workshop = findWorkshopOrThrow(workshopId);

        WorkshopVerificationStatus currentStatus = workshop.getVerificationStatus();
        WorkshopVerificationStatus targetStatus = request.getStatus();

        if (currentStatus == targetStatus) {
            throw new IllegalArgumentException("Workshop verification status is already " + currentStatus);
        }

        if ((targetStatus == WorkshopVerificationStatus.REJECTED || targetStatus == WorkshopVerificationStatus.SUSPENDED)
                && (request.getReason() == null || request.getReason().trim().isBlank())) {
            throw new IllegalArgumentException("A reason is required when rejecting or suspending a workshop");
        }

        String reason = request.getReason() != null ? request.getReason().trim() : null;
        workshop.setVerificationStatus(targetStatus);
        workshop.setStatusReason(reason);

        // If suspended, also deactivate operational status
        if (targetStatus == WorkshopVerificationStatus.SUSPENDED) {
            workshop.setIsActive(false);
        }

        workshopRepository.save(workshop);

        String desc = "Workshop verification status changed from " + currentStatus + " to " + targetStatus
                + (reason != null ? ". Reason: " + reason : "");
        String metadataJson = String.format("{\"oldVerificationStatus\":\"%s\",\"newVerificationStatus\":\"%s\",\"reason\":%s}",
                currentStatus, targetStatus, reason != null ? "\"" + reason.replace("\"", "\\\"") + "\"" : "null");

        marketplaceAuditService.recordEvent(
                MarketplaceEventType.WORKSHOP_VERIFICATION_CHANGED,
                null,
                null,
                workshopId,
                adminUser != null ? adminUser.getId() : null,
                desc,
                metadataJson
        );

        log.info("AUDIT: Admin [{}] changed workshop [{}] verification from [{}] to [{}] with reason: {}",
                adminUser != null ? adminUser.getEmail() : "SYSTEM",
                workshop.getBusinessName(),
                currentStatus,
                targetStatus,
                reason);

        return getWorkshopDetail(workshopId);
    }

    @Transactional(readOnly = true)
    public List<AdminWorkshopCapabilityResponse> getWorkshopCapabilities(Long workshopId) {
        findWorkshopOrThrow(workshopId);
        return workshopServiceRepository.findByWorkshopIdWithCatalog(workshopId)
                .stream()
                .map(this::mapToCapabilityResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AdminWorkshopOpportunityResponse> getWorkshopOpportunities(
            Long workshopId,
            int page,
            int size,
            OpportunityStatus status
    ) {
        findWorkshopOrThrow(workshopId);

        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(100, Math.max(1, size));

        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LeadOpportunity> oppPage = leadOpportunityRepository.findByWorkshopIdAndOptionalStatus(workshopId, status, pageable);

        List<AdminWorkshopOpportunityResponse> dtos = oppPage.getContent().stream()
                .map(this::mapToOpportunityResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, oppPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<AdminWorkshopBookingResponse> getWorkshopBookings(
            Long workshopId,
            int page,
            int size,
            BookingStatus status
    ) {
        findWorkshopOrThrow(workshopId);

        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(100, Math.max(1, size));

        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> bookingPage = bookingRepository.findBookingsByWorkshopIdAndOptionalStatus(workshopId, status, pageable);

        List<AdminWorkshopBookingResponse> dtos = bookingPage.getContent().stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, bookingPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<WorkshopJobResponse> getWorkshopJobs(
            Long workshopId,
            int page,
            int size,
            WorkshopJobStatus status
    ) {
        findWorkshopOrThrow(workshopId);

        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(100, Math.max(1, size));

        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WorkshopJob> jobPage = workshopJobRepository.findByWorkshopIdAndOptionalStatus(workshopId, status, pageable);

        List<WorkshopJobResponse> dtos = jobPage.getContent().stream()
                .map(workshopJobService::mapToJobResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, jobPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<WorkshopPaymentResponse> getWorkshopPayments(Long workshopId) {
        findWorkshopOrThrow(workshopId);
        List<WorkshopPayment> payments = workshopPaymentRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopId);
        return payments.stream().map(p -> new WorkshopPaymentResponse(
                p.getId(),
                p.getOpportunity() != null ? p.getOpportunity().getId() : null,
                p.getWorkshop().getId(),
                p.getWorkshop().getBusinessName(),
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
        )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getWorkshopRefunds(Long workshopId) {
        findWorkshopOrThrow(workshopId);
        List<Refund> refunds = refundRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopId);
        return refunds.stream().map(r -> new RefundResponse(
                r.getId(),
                r.getPayment() != null ? r.getPayment().getId() : null,
                r.getOpportunity() != null ? r.getOpportunity().getId() : null,
                r.getWorkshop().getId(),
                r.getWorkshop().getBusinessName(),
                r.getRefundAmount(),
                r.getRefundStatus(),
                r.getRefundReason(),
                r.getRazorpayRefundId(),
                r.getFailureReason(),
                r.getInitiatedAt(),
                r.getProcessedAt(),
                r.getCreatedAt()
        )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkshopWalletResponse getWorkshopWallet(Long workshopId) {
        findWorkshopOrThrow(workshopId);
        return walletService.getWalletResponse(workshopId);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceAuditEvent> getWorkshopAuditEvents(Long workshopId) {
        findWorkshopOrThrow(workshopId);
        return marketplaceAuditService.getAuditEventsForWorkshop(workshopId);
    }

    private Workshop findWorkshopOrThrow(Long workshopId) {
        return workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + workshopId));
    }

    private AdminWorkshopCapabilityResponse mapToCapabilityResponse(WorkshopService ws) {
        AdminWorkshopCapabilityResponse res = new AdminWorkshopCapabilityResponse();
        res.setId(ws.getId());
        if (ws.getServiceCatalog() != null) {
            res.setServiceCatalogId(ws.getServiceCatalog().getId());
            res.setName(ws.getServiceCatalog().getName());
            res.setDescription(ws.getServiceCatalog().getDescription());
            res.setCategory(ws.getServiceCatalog().getCategory());
            res.setBasePrice(ws.getServiceCatalog().getBasePrice());
            res.setEstimatedDurationMinutes(ws.getServiceCatalog().getEstimatedDurationMinutes());
            res.setIsServiceActive(ws.getServiceCatalog().getIsActive());
        }
        res.setIsCapabilityActive(ws.getIsActive());
        res.setLinkedAt(ws.getCreatedAt());
        return res;
    }

    private AdminWorkshopOpportunityResponse mapToOpportunityResponse(LeadOpportunity lo) {
        AdminWorkshopOpportunityResponse res = new AdminWorkshopOpportunityResponse();
        res.setId(lo.getId());
        if (lo.getServiceRequest() != null) {
            res.setServiceRequestId(lo.getServiceRequest().getId());
            res.setRequestReference(lo.getServiceRequest().getRequestReference());
            res.setCustomerCity(lo.getServiceRequest().getCity());
            res.setTotalAmount(lo.getServiceRequest().getTotalAmount());

            if (lo.getServiceRequest().getVehicle() != null) {
                res.setVehicleInfo(lo.getServiceRequest().getVehicle().getMake() + " " +
                        lo.getServiceRequest().getVehicle().getModel() + " (" +
                        lo.getServiceRequest().getVehicle().getRegistrationNumber() + ")");
            }

            if (lo.getServiceRequest().getItems() != null && !lo.getServiceRequest().getItems().isEmpty()) {
                String summary = lo.getServiceRequest().getItems().stream()
                        .map(item -> item.getServiceCatalog() != null ? item.getServiceCatalog().getName() : "Service")
                        .collect(Collectors.joining(", "));
                res.setServicesSummary(summary);
            }
        }
        res.setFeeSnapshot(lo.getFeeSnapshot());
        res.setStatus(lo.getStatus());
        res.setViewedAt(lo.getViewedAt());
        res.setAcceptedAt(lo.getAcceptedAt());
        res.setPaidAt(lo.getPaidAt());
        res.setUnlockedAt(lo.getUnlockedAt());
        res.setTransferredAt(lo.getTransferredAt());
        res.setTransferReason(lo.getTransferReason());
        res.setCreatedAt(lo.getCreatedAt());
        return res;
    }

    private AdminWorkshopBookingResponse mapToBookingResponse(Booking b) {
        AdminWorkshopBookingResponse res = new AdminWorkshopBookingResponse();
        res.setId(b.getId());
        res.setBookingReference(b.getBookingReference());
        if (b.getUser() != null) {
            res.setCustomerId(b.getUser().getId());
            res.setCustomerName(b.getUser().getName());
            res.setCustomerEmail(b.getUser().getEmail());
            res.setCustomerPhone(b.getUser().getPhone());
        }
        if (b.getVehicle() != null) {
            res.setVehicleId(b.getVehicle().getId());
            res.setVehiclePlate(b.getVehicle().getRegistrationNumber());
            res.setVehicleModel(b.getVehicle().getMake() + " " + b.getVehicle().getModel());
        }
        res.setServiceName(b.getServiceNameSnapshot() != null ? b.getServiceNameSnapshot() : (b.getService() != null ? b.getService().getName() : "Service"));
        res.setTotalAmount(b.getTotalAmount());
        res.setStatus(b.getStatus());
        res.setBookingDate(b.getBookingDate());
        res.setBookingTime(b.getBookingTime());
        res.setTimeSlot(b.getTimeSlot());
        res.setCity(b.getCity());
        res.setCreatedAt(b.getCreatedAt());
        return res;
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    Long id = ((Number) row[0]).longValue();
                    Long count = ((Number) row[1]).longValue();
                    map.put(id, count);
                }
            }
        }
        return map;
    }
}

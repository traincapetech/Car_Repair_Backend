package com.carservice.backend.marketplace.controller;

import com.carservice.backend.admin.dto.*;
import com.carservice.backend.admin.service.AdminMarketplaceService;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.PlatformConfigResponse;
import com.carservice.backend.marketplace.dto.RefundResponse;
import com.carservice.backend.marketplace.dto.ServiceRequestResponse;
import com.carservice.backend.marketplace.dto.UpdateLeadFeeRequest;
import com.carservice.backend.marketplace.dto.WorkshopPaymentResponse;
import com.carservice.backend.marketplace.entity.MarketplaceAuditEvent;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.service.MarketplaceAuditService;
import com.carservice.backend.marketplace.service.PlatformConfigService;
import com.carservice.backend.marketplace.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/marketplace")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMarketplaceController {

    private final PlatformConfigService platformConfigService;
    private final ServiceRequestService serviceRequestService;
    private final MarketplaceAuditService auditService;
    private final AdminMarketplaceService adminMarketplaceService;

    public AdminMarketplaceController(
            PlatformConfigService platformConfigService,
            ServiceRequestService serviceRequestService,
            MarketplaceAuditService auditService,
            AdminMarketplaceService adminMarketplaceService
    ) {
        this.platformConfigService = platformConfigService;
        this.serviceRequestService = serviceRequestService;
        this.auditService = auditService;
        this.adminMarketplaceService = adminMarketplaceService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminMarketplaceSummaryResponse>> getMarketplaceSummary() {
        AdminMarketplaceSummaryResponse summary = adminMarketplaceService.getMarketplaceSummary();
        return ResponseEntity.ok(ApiResponse.success("Marketplace summary metrics retrieved successfully", summary));
    }

    @GetMapping("/service-requests")
    public ResponseEntity<ApiResponse<Page<AdminServiceRequestListResponse>>> getServiceRequests(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long workshopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "DESC") String direction
    ) {
        ServiceRequestStatus srStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                srStatus = ServiceRequestStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Sort.Direction sortDir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sort == null || sort.trim().isEmpty()) ? "createdAt" : sort.trim();
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(sortDir, sortProperty));

        Page<AdminServiceRequestListResponse> result = adminMarketplaceService.getServiceRequests(
                search,
                srStatus,
                city,
                workshopId,
                startDate,
                endDate,
                pageRequest
        );
        return ResponseEntity.ok(ApiResponse.success("Service requests retrieved successfully", result));
    }

    @GetMapping("/service-requests/{id}")
    public ResponseEntity<ApiResponse<AdminServiceRequestDetailResponse>> getServiceRequestDetail(
            @PathVariable Long id
    ) {
        AdminServiceRequestDetailResponse response = adminMarketplaceService.getServiceRequestDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Service request 360 detail retrieved successfully", response));
    }

    @GetMapping("/service-requests/{id}/opportunities")
    public ResponseEntity<ApiResponse<List<AdminMarketplaceOpportunityResponse>>> getServiceRequestOpportunities(
            @PathVariable Long id
    ) {
        List<AdminMarketplaceOpportunityResponse> list = adminMarketplaceService.getOpportunitiesForRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Service request opportunities retrieved successfully", list));
    }

    @GetMapping("/service-requests/{id}/payments")
    public ResponseEntity<ApiResponse<List<WorkshopPaymentResponse>>> getServiceRequestPayments(
            @PathVariable Long id
    ) {
        List<WorkshopPaymentResponse> list = adminMarketplaceService.getPaymentsForRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Service request payments retrieved successfully", list));
    }

    @GetMapping("/service-requests/{id}/refunds")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getServiceRequestRefunds(
            @PathVariable Long id
    ) {
        List<RefundResponse> list = adminMarketplaceService.getRefundsForRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Service request refunds retrieved successfully", list));
    }

    @GetMapping("/service-requests/{id}/timeline")
    public ResponseEntity<ApiResponse<List<AdminMarketplaceTimelineEventResponse>>> getServiceRequestTimeline(
            @PathVariable Long id
    ) {
        List<AdminMarketplaceTimelineEventResponse> timeline = adminMarketplaceService.getTimelineForRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Service request timeline retrieved successfully", timeline));
    }

    @GetMapping("/config/lead-fee")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLeadFee() {
        BigDecimal fee = platformConfigService.getLeadAcceptanceFee();
        return ResponseEntity.ok(ApiResponse.success("Current lead fee retrieved", Map.of("leadAcceptanceFee", fee)));
    }

    @PutMapping("/config/lead-fee")
    public ResponseEntity<ApiResponse<PlatformConfigResponse>> updateLeadFee(
            @Valid @RequestBody UpdateLeadFeeRequest request
    ) {
        PlatformConfigResponse response = platformConfigService.updateLeadAcceptanceFee(request.getFee());
        return ResponseEntity.ok(ApiResponse.success("Lead fee updated successfully", response));
    }

    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<PlatformConfigResponse>>> getAllConfigs() {
        List<PlatformConfigResponse> list = platformConfigService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success("Platform configs retrieved", list));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getAllRequests() {
        List<ServiceRequestResponse> list = serviceRequestService.getAllRequestsForAdmin();
        return ResponseEntity.ok(ApiResponse.success("All service requests retrieved", list));
    }

    @GetMapping("/audit-events")
    public ResponseEntity<ApiResponse<List<MarketplaceAuditEvent>>> getRecentAuditEvents() {
        List<MarketplaceAuditEvent> events = auditService.getRecentAuditEvents();
        return ResponseEntity.ok(ApiResponse.success("Recent audit events retrieved", events));
    }

    @GetMapping("/audit-events/request/{requestId}")
    public ResponseEntity<ApiResponse<List<MarketplaceAuditEvent>>> getAuditEventsForRequest(@PathVariable Long requestId) {
        List<MarketplaceAuditEvent> events = auditService.getAuditEventsForRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success("Audit events for request retrieved", events));
    }

    @GetMapping("/audit-events/workshop/{workshopId}")
    public ResponseEntity<ApiResponse<List<MarketplaceAuditEvent>>> getAuditEventsForWorkshop(@PathVariable Long workshopId) {
        List<MarketplaceAuditEvent> events = auditService.getAuditEventsForWorkshop(workshopId);
        return ResponseEntity.ok(ApiResponse.success("Audit events for workshop retrieved", events));
    }
}

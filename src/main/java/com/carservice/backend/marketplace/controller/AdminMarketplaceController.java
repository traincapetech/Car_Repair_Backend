package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.PlatformConfigResponse;
import com.carservice.backend.marketplace.dto.ServiceRequestResponse;
import com.carservice.backend.marketplace.dto.UpdateLeadFeeRequest;
import com.carservice.backend.marketplace.entity.MarketplaceAuditEvent;
import com.carservice.backend.marketplace.service.MarketplaceAuditService;
import com.carservice.backend.marketplace.service.PlatformConfigService;
import com.carservice.backend.marketplace.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/marketplace")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMarketplaceController {

    private final PlatformConfigService platformConfigService;
    private final ServiceRequestService serviceRequestService;
    private final MarketplaceAuditService auditService;

    public AdminMarketplaceController(
            PlatformConfigService platformConfigService,
            ServiceRequestService serviceRequestService,
            MarketplaceAuditService auditService
    ) {
        this.platformConfigService = platformConfigService;
        this.serviceRequestService = serviceRequestService;
        this.auditService = auditService;
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

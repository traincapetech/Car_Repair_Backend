package com.carservice.backend.admin.controller;

import com.carservice.backend.admin.dto.*;
import com.carservice.backend.admin.service.AdminWorkshopService;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.RefundResponse;
import com.carservice.backend.marketplace.dto.WorkshopJobResponse;
import com.carservice.backend.marketplace.dto.WorkshopPaymentResponse;
import com.carservice.backend.marketplace.dto.WorkshopWalletResponse;
import com.carservice.backend.marketplace.entity.MarketplaceAuditEvent;
import com.carservice.backend.marketplace.enums.OpportunityStatus;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/workshops")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWorkshopController {

    private final AdminWorkshopService adminWorkshopService;

    public AdminWorkshopController(AdminWorkshopService adminWorkshopService) {
        this.adminWorkshopService = adminWorkshopService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminWorkshopListResponse>>> getWorkshops(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String activity,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "DESC") String direction
    ) {
        Page<AdminWorkshopListResponse> result = adminWorkshopService.getWorkshops(
                page, size, search, status, verificationStatus, city, state, activity, sort, direction
        );
        return ResponseEntity.ok(ApiResponse.success("Workshops retrieved successfully", result));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminWorkshopSummaryResponse>> getWorkshopSummary() {
        AdminWorkshopSummaryResponse summary = adminWorkshopService.getWorkshopSummary();
        return ResponseEntity.ok(ApiResponse.success("Workshop summary counters retrieved successfully", summary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminWorkshopDetailResponse>> getWorkshopDetail(@PathVariable Long id) {
        AdminWorkshopDetailResponse detail = adminWorkshopService.getWorkshopDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Workshop 360° details retrieved successfully", detail));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminWorkshopDetailResponse>> updateWorkshopStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkshopStatusRequest request,
            Authentication authentication
    ) {
        User adminUser = (authentication != null && authentication.getPrincipal() instanceof User)
                ? (User) authentication.getPrincipal()
                : null;

        AdminWorkshopDetailResponse updated = adminWorkshopService.updateWorkshopStatus(id, request, adminUser);
        String action = Boolean.TRUE.equals(request.getIsActive()) ? "activated" : "deactivated";
        return ResponseEntity.ok(ApiResponse.success("Workshop operational status " + action + " successfully", updated));
    }

    @PatchMapping("/{id}/verification")
    public ResponseEntity<ApiResponse<AdminWorkshopDetailResponse>> updateWorkshopVerification(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkshopVerificationRequest request,
            Authentication authentication
    ) {
        User adminUser = (authentication != null && authentication.getPrincipal() instanceof User)
                ? (User) authentication.getPrincipal()
                : null;

        AdminWorkshopDetailResponse updated = adminWorkshopService.updateWorkshopVerification(id, request, adminUser);
        return ResponseEntity.ok(ApiResponse.success("Workshop verification status updated to " + request.getStatus(), updated));
    }

    @GetMapping("/{id}/capabilities")
    public ResponseEntity<ApiResponse<List<AdminWorkshopCapabilityResponse>>> getWorkshopCapabilities(@PathVariable Long id) {
        List<AdminWorkshopCapabilityResponse> capabilities = adminWorkshopService.getWorkshopCapabilities(id);
        return ResponseEntity.ok(ApiResponse.success("Workshop capabilities retrieved successfully", capabilities));
    }

    @GetMapping("/{id}/opportunities")
    public ResponseEntity<ApiResponse<Page<AdminWorkshopOpportunityResponse>>> getWorkshopOpportunities(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) OpportunityStatus status
    ) {
        Page<AdminWorkshopOpportunityResponse> opportunities = adminWorkshopService.getWorkshopOpportunities(id, page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Workshop opportunities retrieved successfully", opportunities));
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<ApiResponse<Page<AdminWorkshopBookingResponse>>> getWorkshopBookings(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) BookingStatus status
    ) {
        Page<AdminWorkshopBookingResponse> bookings = adminWorkshopService.getWorkshopBookings(id, page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Workshop bookings retrieved successfully", bookings));
    }

    @GetMapping("/{id}/jobs")
    public ResponseEntity<ApiResponse<Page<WorkshopJobResponse>>> getWorkshopJobs(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) WorkshopJobStatus status
    ) {
        Page<WorkshopJobResponse> jobs = adminWorkshopService.getWorkshopJobs(id, page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Workshop jobs retrieved successfully", jobs));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<List<WorkshopPaymentResponse>>> getWorkshopPayments(@PathVariable Long id) {
        List<WorkshopPaymentResponse> payments = adminWorkshopService.getWorkshopPayments(id);
        return ResponseEntity.ok(ApiResponse.success("Workshop payments retrieved successfully", payments));
    }

    @GetMapping("/{id}/refunds")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getWorkshopRefunds(@PathVariable Long id) {
        List<RefundResponse> refunds = adminWorkshopService.getWorkshopRefunds(id);
        return ResponseEntity.ok(ApiResponse.success("Workshop refunds retrieved successfully", refunds));
    }

    @GetMapping("/{id}/wallet")
    public ResponseEntity<ApiResponse<WorkshopWalletResponse>> getWorkshopWallet(@PathVariable Long id) {
        WorkshopWalletResponse wallet = adminWorkshopService.getWorkshopWallet(id);
        return ResponseEntity.ok(ApiResponse.success("Workshop wallet retrieved successfully", wallet));
    }

    @GetMapping("/{id}/audit-events")
    public ResponseEntity<ApiResponse<List<MarketplaceAuditEvent>>> getWorkshopAuditEvents(@PathVariable Long id) {
        List<MarketplaceAuditEvent> events = adminWorkshopService.getWorkshopAuditEvents(id);
        return ResponseEntity.ok(ApiResponse.success("Workshop audit events retrieved successfully", events));
    }
}

package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.RefundResponse;
import com.carservice.backend.marketplace.service.WorkshopRefundService;
import com.carservice.backend.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkshopRefundController {

    private final WorkshopRefundService refundService;

    public WorkshopRefundController(WorkshopRefundService refundService) {
        this.refundService = refundService;
    }

    // ==========================================
    // PARTNER REFUND ENDPOINTS
    // ==========================================

    @GetMapping("/api/v1/partner/refunds")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getPartnerRefunds(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<RefundResponse> refunds = refundService.getRefundsForWorkshop(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Refund records fetched successfully", refunds));
    }

    @GetMapping("/api/v1/partner/refunds/{id}")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> getPartnerRefundDetails(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        RefundResponse response = refundService.getRefundDetails(currentUser, id);
        return ResponseEntity.ok(ApiResponse.success("Refund details fetched successfully", response));
    }

    // ==========================================
    // ADMIN MARKETPLACE REFUND ENDPOINTS
    // ==========================================

    @GetMapping("/api/v1/admin/marketplace/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getAllRefundsForAdmin() {
        List<RefundResponse> refunds = refundService.getAllRefundsForAdmin();
        return ResponseEntity.ok(ApiResponse.success("All platform refund records fetched successfully", refunds));
    }

    @GetMapping("/api/v1/admin/marketplace/refunds/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> getAdminRefundDetails(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        RefundResponse response = refundService.getRefundDetails(currentUser, id);
        return ResponseEntity.ok(ApiResponse.success("Refund details fetched successfully", response));
    }
}

package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.InitiatePaymentRequest;
import com.carservice.backend.marketplace.dto.InitiatePaymentResponse;
import com.carservice.backend.marketplace.dto.VerifyPaymentRequest;
import com.carservice.backend.marketplace.dto.WalletClaimPaymentRequest;
import com.carservice.backend.marketplace.dto.WorkshopPaymentResponse;
import com.carservice.backend.marketplace.service.WorkshopPaymentService;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partner/payments")
public class WorkshopPaymentController {

    private final WorkshopPaymentService paymentService;

    public WorkshopPaymentController(WorkshopPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiatePayment(
            Authentication authentication,
            @Valid @RequestBody InitiatePaymentRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        InitiatePaymentResponse response = paymentService.initiatePayment(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated successfully", response));
    }

    @PostMapping("/pay-wallet")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorkshopPaymentResponse>> payWithWallet(
            Authentication authentication,
            @Valid @RequestBody WalletClaimPaymentRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        WorkshopPaymentResponse response = paymentService.claimOpportunityWithWallet(
                currentUser,
                request.getOpportunityId(),
                request.getIdempotencyKey()
        );
        return ResponseEntity.ok(ApiResponse.success("Payment completed via wallet", response));
    }

    @PostMapping("/{id}/verify-razorpay")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorkshopPaymentResponse>> verifyRazorpayPayment(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        WorkshopPaymentResponse response = paymentService.verifyAndClaimRazorpayPayment(currentUser, id, request);
        return ResponseEntity.ok(ApiResponse.success("Razorpay payment verified successfully", response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorkshopPaymentResponse>> cancelPayment(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        WorkshopPaymentResponse response = paymentService.cancelPayment(currentUser, id);
        return ResponseEntity.ok(ApiResponse.success("Payment cancelled successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorkshopPaymentResponse>> getPaymentDetails(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        WorkshopPaymentResponse response = paymentService.getPaymentDetails(currentUser, id);
        return ResponseEntity.ok(ApiResponse.success("Payment details retrieved successfully", response));
    }

    @GetMapping("/opportunity/{opportunityId}")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WorkshopPaymentResponse>>> getPaymentsForOpportunity(
            Authentication authentication,
            @PathVariable Long opportunityId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        List<WorkshopPaymentResponse> response = paymentService.getPaymentsForOpportunity(currentUser, opportunityId);
        return ResponseEntity.ok(ApiResponse.success("Payments for opportunity retrieved successfully", response));
    }
}

package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.DirectPaymentRequest;
import com.carservice.backend.marketplace.dto.LeadOpportunityResponse;
import com.carservice.backend.marketplace.dto.TransferOpportunityRequest;
import com.carservice.backend.marketplace.enums.OpportunityStatus;
import com.carservice.backend.marketplace.service.LeadOpportunityService;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partner/opportunities")
public class PartnerOpportunityController {

    private final LeadOpportunityService leadOpportunityService;

    public PartnerOpportunityController(LeadOpportunityService leadOpportunityService) {
        this.leadOpportunityService = leadOpportunityService;
    }

    @GetMapping
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LeadOpportunityResponse>>> getOpportunities(
            Authentication authentication,
            @RequestParam(required = false) OpportunityStatus status
    ) {
        User currentUser = (User) authentication.getPrincipal();
        List<LeadOpportunityResponse> list = leadOpportunityService.getOpportunitiesForWorkshop(currentUser, status);

        return ResponseEntity.ok(ApiResponse.success("Lead opportunities fetched successfully", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeadOpportunityResponse>> getOpportunityDetails(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        LeadOpportunityResponse response = leadOpportunityService.getOpportunityDetails(currentUser, id);

        return ResponseEntity.ok(ApiResponse.success("Opportunity details fetched successfully", response));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeadOpportunityResponse>> acceptOpportunity(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        LeadOpportunityResponse response = leadOpportunityService.acceptOpportunity(currentUser, id);

        return ResponseEntity.ok(ApiResponse.success("Opportunity accepted successfully. Proceed to payment to unlock customer details.", response));
    }

    @PostMapping("/{id}/pay/wallet")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeadOpportunityResponse>> payWithWallet(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        LeadOpportunityResponse response = leadOpportunityService.payAndUnlockWithWallet(currentUser, id);

        return ResponseEntity.ok(ApiResponse.success("Payment completed via wallet. Customer contact details unlocked.", response));
    }

    @PostMapping("/{id}/pay/direct")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeadOpportunityResponse>> payDirect(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody DirectPaymentRequest paymentRequest
    ) {
        User currentUser = (User) authentication.getPrincipal();
        LeadOpportunityResponse response = leadOpportunityService.payAndUnlockDirect(currentUser, id, paymentRequest);

        return ResponseEntity.ok(ApiResponse.success("Payment completed successfully. Customer contact details unlocked.", response));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeadOpportunityResponse>> transferOpportunity(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TransferOpportunityRequest transferRequest
    ) {
        User currentUser = (User) authentication.getPrincipal();
        LeadOpportunityResponse response = leadOpportunityService.transferOpportunity(currentUser, id, transferRequest);

        return ResponseEntity.ok(ApiResponse.success("Opportunity transferred and re-matching initiated.", response));
    }
}

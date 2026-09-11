package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.WalletTopupRequest;
import com.carservice.backend.marketplace.dto.WalletTransactionResponse;
import com.carservice.backend.marketplace.dto.WorkshopWalletResponse;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.marketplace.service.WalletService;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partner/wallet")
public class PartnerWalletController {

    private final WalletService walletService;
    private final WorkshopRepository workshopRepository;

    public PartnerWalletController(WalletService walletService, WorkshopRepository workshopRepository) {
        this.walletService = walletService;
        this.workshopRepository = workshopRepository;
    }

    private Workshop resolveWorkshop(User currentUser) {
        return workshopRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No workshop partner account found for user: " + currentUser.getEmail()));
    }

    @GetMapping
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorkshopWalletResponse>> getWallet(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Workshop workshop = resolveWorkshop(currentUser);
        WorkshopWalletResponse response = walletService.getWalletResponse(workshop.getId());

        return ResponseEntity.ok(ApiResponse.success("Wallet details retrieved successfully", response));
    }

    @PostMapping("/topup")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> topupWallet(
            Authentication authentication,
            @Valid @RequestBody WalletTopupRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Workshop workshop = resolveWorkshop(currentUser);
        WalletTransactionResponse response = walletService.topup(
                workshop.getId(),
                request.getAmount(),
                request.getDescription(),
                request.getReferenceId()
        );

        return ResponseEntity.ok(ApiResponse.success("Wallet top-up successful", response));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getTransactions(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Workshop workshop = resolveWorkshop(currentUser);
        List<WalletTransactionResponse> transactions = walletService.getTransactions(workshop.getId());

        return ResponseEntity.ok(ApiResponse.success("Wallet transactions retrieved successfully", transactions));
    }
}

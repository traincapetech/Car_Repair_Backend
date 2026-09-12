package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.*;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.WalletTransactionType;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.marketplace.service.WalletService;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    @PostMapping("/topup/initiate")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TopupInitiateResponse>> initiateTopup(
            Authentication authentication,
            @Valid @RequestBody TopupInitiateRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Workshop workshop = resolveWorkshop(currentUser);
        TopupInitiateResponse response = walletService.initiateTopup(
                workshop.getId(),
                request.getAmount(),
                request.getDescription()
        );

        return ResponseEntity.ok(ApiResponse.success("Top-up intent initiated successfully", response));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getTransactions(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) WalletTransactionType type
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Workshop workshop = resolveWorkshop(currentUser);

        if (page != null) {
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
            Page<WalletTransactionResponse> txPage = walletService.getTransactionsPaginated(workshop.getId(), type, pageable);
            return ResponseEntity.ok(ApiResponse.success("Wallet transactions retrieved successfully", txPage));
        } else {
            List<WalletTransactionResponse> transactions = walletService.getTransactions(workshop.getId());
            if (type != null) {
                transactions = transactions.stream().filter(t -> t.getType() == type).collect(Collectors.toList());
            }
            return ResponseEntity.ok(ApiResponse.success("Wallet transactions retrieved successfully", transactions));
        }
    }

    @GetMapping("/transactions/{id}")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> getTransactionById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Workshop workshop = resolveWorkshop(currentUser);
        WalletTransactionResponse response = walletService.getTransactionById(workshop.getId(), id);

        return ResponseEntity.ok(ApiResponse.success("Wallet transaction retrieved successfully", response));
    }
}

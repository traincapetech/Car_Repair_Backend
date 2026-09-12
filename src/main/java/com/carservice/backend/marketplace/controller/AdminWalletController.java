package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.AdminWalletAdjustmentRequest;
import com.carservice.backend.marketplace.dto.WalletTransactionResponse;
import com.carservice.backend.marketplace.dto.WorkshopWalletResponse;
import com.carservice.backend.marketplace.enums.WalletTransactionType;
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
@RequestMapping("/api/v1/admin/wallets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWalletController {

    private final WalletService walletService;

    public AdminWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WorkshopWalletResponse>>> getAllWallets(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        Page<WorkshopWalletResponse> wallets = walletService.getAllWallets(pageable);
        return ResponseEntity.ok(ApiResponse.success("All workshop wallets retrieved successfully", wallets));
    }

    @GetMapping("/{workshopId}")
    public ResponseEntity<ApiResponse<WorkshopWalletResponse>> getWalletForWorkshop(@PathVariable Long workshopId) {
        WorkshopWalletResponse wallet = walletService.getWalletResponse(workshopId);
        return ResponseEntity.ok(ApiResponse.success("Workshop wallet retrieved successfully", wallet));
    }

    @GetMapping("/{workshopId}/transactions")
    public ResponseEntity<ApiResponse<?>> getTransactionsForWorkshop(
            @PathVariable Long workshopId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) WalletTransactionType type
    ) {
        if (page != null) {
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
            Page<WalletTransactionResponse> txPage = walletService.getTransactionsPaginated(workshopId, type, pageable);
            return ResponseEntity.ok(ApiResponse.success("Workshop wallet transactions retrieved successfully", txPage));
        } else {
            List<WalletTransactionResponse> transactions = walletService.getTransactions(workshopId);
            if (type != null) {
                transactions = transactions.stream().filter(t -> t.getType() == type).collect(Collectors.toList());
            }
            return ResponseEntity.ok(ApiResponse.success("Workshop wallet transactions retrieved successfully", transactions));
        }
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> adjustWallet(
            Authentication authentication,
            @Valid @RequestBody AdminWalletAdjustmentRequest request
    ) {
        User adminUser = (User) authentication.getPrincipal();
        WalletTransactionResponse response = walletService.adminAdjustWallet(request, adminUser);
        return ResponseEntity.ok(ApiResponse.success("Wallet adjustment processed successfully", response));
    }
}

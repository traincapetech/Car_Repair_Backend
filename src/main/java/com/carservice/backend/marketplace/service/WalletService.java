package com.carservice.backend.marketplace.service;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.dto.WalletTransactionResponse;
import com.carservice.backend.marketplace.dto.WorkshopWalletResponse;
import com.carservice.backend.marketplace.entity.WalletTransaction;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.entity.WorkshopWallet;
import com.carservice.backend.marketplace.enums.WalletTransactionType;
import com.carservice.backend.marketplace.repository.WalletTransactionRepository;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.marketplace.repository.WorkshopWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private final WorkshopWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WorkshopRepository workshopRepository;

    public WalletService(
            WorkshopWalletRepository walletRepository,
            WalletTransactionRepository transactionRepository,
            WorkshopRepository workshopRepository
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.workshopRepository = workshopRepository;
    }

    @Transactional
    public WorkshopWallet getOrCreateWallet(Workshop workshop) {
        return walletRepository.findByWorkshopId(workshop.getId())
                .orElseGet(() -> {
                    WorkshopWallet newWallet = new WorkshopWallet(workshop, BigDecimal.ZERO);
                    return walletRepository.save(newWallet);
                });
    }

    @Transactional(readOnly = true)
    public WorkshopWalletResponse getWalletResponse(Long workshopId) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + workshopId));

        WorkshopWallet wallet = walletRepository.findByWorkshopId(workshopId)
                .orElseGet(() -> new WorkshopWallet(workshop, BigDecimal.ZERO));

        return new WorkshopWalletResponse(
                wallet.getId(),
                workshop.getId(),
                workshop.getBusinessName(),
                wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO,
                wallet.getUpdatedAt()
        );
    }

    @Transactional
    public WalletTransactionResponse topup(Long workshopId, BigDecimal amount, String description, String referenceId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top-up amount must be strictly greater than 0");
        }

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + workshopId));

        WorkshopWallet wallet = getOrCreateWallet(workshop);
        wallet.credit(amount);
        WorkshopWallet savedWallet = walletRepository.save(wallet);

        String ref = (referenceId != null && !referenceId.isBlank())
                ? referenceId
                : "TOPUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String desc = (description != null && !description.isBlank())
                ? description
                : "Wallet top-up of ₹" + amount;

        WalletTransaction tx = new WalletTransaction(
                savedWallet,
                WalletTransactionType.CREDIT,
                amount,
                savedWallet.getBalance(),
                ref,
                desc
        );
        WalletTransaction savedTx = transactionRepository.save(tx);

        return new WalletTransactionResponse(
                savedTx.getId(),
                savedWallet.getId(),
                savedTx.getType(),
                savedTx.getAmount(),
                savedTx.getBalanceAfter(),
                savedTx.getReferenceId(),
                savedTx.getDescription(),
                savedTx.getCreatedAt()
        );
    }

    @Transactional
    public WalletTransaction debitForLead(Workshop workshop, BigDecimal amount, Long opportunityId) {
        WorkshopWallet wallet = getOrCreateWallet(workshop);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance. Required: ₹" + amount + ", Current balance: ₹" + wallet.getBalance());
        }

        wallet.debit(amount);
        WorkshopWallet savedWallet = walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction(
                savedWallet,
                WalletTransactionType.DEBIT,
                amount,
                savedWallet.getBalance(),
                "LEAD-" + opportunityId,
                "Lead acceptance fee for opportunity #" + opportunityId
        );
        return transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getTransactions(Long workshopId) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + workshopId));

        WorkshopWallet wallet = walletRepository.findByWorkshopId(workshop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for workshop: " + workshopId));

        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream()
                .map(tx -> new WalletTransactionResponse(
                        tx.getId(),
                        wallet.getId(),
                        tx.getType(),
                        tx.getAmount(),
                        tx.getBalanceAfter(),
                        tx.getReferenceId(),
                        tx.getDescription(),
                        tx.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}

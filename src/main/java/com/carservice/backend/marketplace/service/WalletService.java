package com.carservice.backend.marketplace.service;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.dto.*;
import com.carservice.backend.marketplace.entity.WalletTransaction;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.entity.WorkshopWallet;
import com.carservice.backend.marketplace.enums.WalletReferenceType;
import com.carservice.backend.marketplace.enums.WalletStatus;
import com.carservice.backend.marketplace.enums.WalletTransactionType;
import com.carservice.backend.marketplace.repository.WalletTransactionRepository;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.marketplace.repository.WorkshopWalletRepository;
import com.carservice.backend.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private static final BigDecimal MIN_TOPUP_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal MAX_TOPUP_AMOUNT = new BigDecimal("50000.00");

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
                    WorkshopWallet newWallet = new WorkshopWallet(workshop, BigDecimal.ZERO, "INR", WalletStatus.ACTIVE);
                    return walletRepository.save(newWallet);
                });
    }

    @Transactional
    public WorkshopWallet getWallet(Workshop workshop) {
        return getOrCreateWallet(workshop);
    }

    @Transactional
    public BigDecimal getBalance(Workshop workshop) {
        return getOrCreateWallet(workshop).getBalance();
    }

    @Transactional
    public WorkshopWalletResponse getWalletResponse(Long workshopId) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + workshopId));

        WorkshopWallet wallet = walletRepository.findByWorkshopId(workshopId)
                .orElseGet(() -> {
                    WorkshopWallet newWallet = new WorkshopWallet(workshop, BigDecimal.ZERO, "INR", WalletStatus.ACTIVE);
                    return walletRepository.save(newWallet);
                });

        return new WorkshopWalletResponse(
                wallet.getId(),
                workshop.getId(),
                workshop.getBusinessName(),
                wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO,
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getUpdatedAt()
        );
    }

    /**
     * Prepares top-up intent without crediting money (safe preparation endpoint for STEP 18E Razorpay).
     */
    @Transactional
    public TopupInitiateResponse initiateTopup(Long workshopId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(MIN_TOPUP_AMOUNT) < 0) {
            throw new IllegalArgumentException("Minimum top-up amount is ₹" + MIN_TOPUP_AMOUNT);
        }
        if (amount.compareTo(MAX_TOPUP_AMOUNT) > 0) {
            throw new IllegalArgumentException("Maximum top-up amount per transaction is ₹" + MAX_TOPUP_AMOUNT);
        }

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + workshopId));

        WorkshopWallet wallet = getOrCreateWallet(workshop);
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is currently " + wallet.getStatus() + ". Cannot initiate top-up.");
        }

        String intentId = "TOPUP-INTENT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String desc = (description != null && !description.isBlank())
                ? description.trim()
                : "Wallet top-up of ₹" + amount.setScale(2, RoundingMode.HALF_UP);

        return new TopupInitiateResponse(
                intentId,
                workshop.getId(),
                workshop.getBusinessName(),
                amount.setScale(2, RoundingMode.HALF_UP),
                wallet.getCurrency(),
                "PENDING",
                desc,
                LocalDateTime.now()
        );
    }

    /**
     * Credit operation with pessimistic locking and idempotency protection.
     */
    @Transactional
    public WalletTransactionResponse credit(
            Workshop workshop,
            BigDecimal amount,
            WalletReferenceType referenceType,
            String referenceId,
            String idempotencyKey,
            String description
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be strictly greater than 0");
        }

        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);

        // 1. Idempotency Key Guard
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<WalletTransaction> existingTx = transactionRepository
                    .findFirstByWorkshopIdAndIdempotencyKey(workshop.getId(), idempotencyKey.trim());
            if (existingTx.isPresent()) {
                log.warn("Idempotent credit hit for workshop #{}, key: {}. Returning existing transaction #{}",
                        workshop.getId(), idempotencyKey, existingTx.get().getId());
                return mapToResponse(existingTx.get());
            }
        }

        // 2. Refund Duplicate Check
        if (referenceType == WalletReferenceType.OPPORTUNITY_REFUND && referenceId != null && !referenceId.isBlank()) {
            boolean alreadyRefunded = transactionRepository
                    .existsByWorkshopIdAndReferenceTypeAndReferenceId(workshop.getId(), referenceType, referenceId.trim());
            if (alreadyRefunded) {
                log.warn("Duplicate refund credit attempt for workshop #{}, reference: {}. Skipping duplicate credit.",
                        workshop.getId(), referenceId);
                List<WalletTransaction> txs = transactionRepository.findByWorkshopIdOrderByCreatedAtDesc(workshop.getId());
                for (WalletTransaction t : txs) {
                    if (referenceId.trim().equals(t.getReferenceId())) {
                        return mapToResponse(t);
                    }
                }
            }
        }

        // 3. Acquire Pessimistic Write Lock on Wallet Row
        WorkshopWallet wallet = walletRepository.findByWorkshopIdWithLock(workshop.getId())
                .orElseGet(() -> {
                    WorkshopWallet newWallet = new WorkshopWallet(workshop, BigDecimal.ZERO, "INR", WalletStatus.ACTIVE);
                    return walletRepository.save(newWallet);
                });

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Cannot credit wallet in status: " + wallet.getStatus());
        }

        BigDecimal balanceBefore = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        wallet.credit(normalizedAmount);
        BigDecimal balanceAfter = wallet.getBalance();
        WorkshopWallet savedWallet = walletRepository.save(wallet);

        String refId = (referenceId != null && !referenceId.isBlank())
                ? referenceId.trim()
                : "CRED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String desc = (description != null && !description.isBlank())
                ? description.trim()
                : "Credit of ₹" + normalizedAmount;

        WalletTransaction tx = new WalletTransaction(
                savedWallet,
                workshop,
                WalletTransactionType.CREDIT,
                referenceType != null ? referenceType : WalletReferenceType.WALLET_TOPUP,
                normalizedAmount,
                balanceBefore,
                balanceAfter,
                refId,
                idempotencyKey != null ? idempotencyKey.trim() : null,
                desc
        );

        WalletTransaction savedTx = transactionRepository.save(tx);
        log.info("Wallet credited: workshop={}, amount=₹{}, before=₹{}, after=₹{}, txId={}",
                workshop.getId(), normalizedAmount, balanceBefore, balanceAfter, savedTx.getId());

        return mapToResponse(savedTx);
    }

    /**
     * Debit operation with pessimistic locking, balance check, and idempotency protection.
     */
    @Transactional
    public WalletTransactionResponse debit(
            Workshop workshop,
            BigDecimal amount,
            WalletReferenceType referenceType,
            String referenceId,
            String idempotencyKey,
            String description
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be strictly greater than 0");
        }

        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);

        // 1. Idempotency Key Guard
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<WalletTransaction> existingTx = transactionRepository
                    .findFirstByWorkshopIdAndIdempotencyKey(workshop.getId(), idempotencyKey.trim());
            if (existingTx.isPresent()) {
                log.warn("Idempotent debit hit for workshop #{}, key: {}. Returning existing transaction #{}",
                        workshop.getId(), idempotencyKey, existingTx.get().getId());
                return mapToResponse(existingTx.get());
            }
        }

        // 2. Acquire Pessimistic Write Lock on Wallet Row
        WorkshopWallet wallet = walletRepository.findByWorkshopIdWithLock(workshop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for workshop: " + workshop.getId()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Cannot debit wallet in status: " + wallet.getStatus());
        }

        BigDecimal balanceBefore = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        if (balanceBefore.compareTo(normalizedAmount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance. Available: ₹" + balanceBefore + ", Required: ₹" + normalizedAmount);
        }

        wallet.debit(normalizedAmount);
        BigDecimal balanceAfter = wallet.getBalance();
        WorkshopWallet savedWallet = walletRepository.save(wallet);

        String refId = (referenceId != null && !referenceId.isBlank())
                ? referenceId.trim()
                : "DEB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String desc = (description != null && !description.isBlank())
                ? description.trim()
                : "Debit of ₹" + normalizedAmount;

        WalletTransaction tx = new WalletTransaction(
                savedWallet,
                workshop,
                WalletTransactionType.DEBIT,
                referenceType != null ? referenceType : WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE,
                normalizedAmount,
                balanceBefore,
                balanceAfter,
                refId,
                idempotencyKey != null ? idempotencyKey.trim() : null,
                desc
        );

        WalletTransaction savedTx = transactionRepository.save(tx);
        log.info("Wallet debited: workshop={}, amount=₹{}, before=₹{}, after=₹{}, txId={}",
                workshop.getId(), normalizedAmount, balanceBefore, balanceAfter, savedTx.getId());

        return mapToResponse(savedTx);
    }

    /**
     * Backward-compatible development/test topup method.
     */
    @Transactional
    public WalletTransactionResponse topup(Long workshopId, BigDecimal amount, String description, String referenceId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top-up amount must be strictly greater than 0");
        }

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + workshopId));

        return credit(
                workshop,
                amount,
                WalletReferenceType.WALLET_TOPUP,
                referenceId,
                null,
                description
        );
    }

    /**
     * Backward-compatible debit method for lead acceptance.
     */
    @Transactional
    public WalletTransaction debitForLead(Workshop workshop, BigDecimal amount, Long opportunityId) {
        WalletTransactionResponse resp = debit(
                workshop,
                amount,
                WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE,
                "LEAD-" + opportunityId,
                null,
                "Lead acceptance fee for opportunity #" + opportunityId
        );
        return transactionRepository.findById(resp.getId()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getTransactions(Long workshopId) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + workshopId));

        return transactionRepository.findByWorkshopIdOrderByCreatedAtDesc(workshop.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactionsPaginated(Long workshopId, WalletTransactionType type, Pageable pageable) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + workshopId));

        Page<WalletTransaction> page;
        if (type != null) {
            page = transactionRepository.findByWorkshopIdAndTypeOrderByCreatedAtDesc(workshop.getId(), type, pageable);
        } else {
            page = transactionRepository.findByWorkshopIdOrderByCreatedAtDesc(workshop.getId(), pageable);
        }

        return page.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public WalletTransactionResponse getTransactionById(Long workshopId, Long transactionId) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + workshopId));

        WalletTransaction tx = transactionRepository.findByIdAndWorkshopId(transactionId, workshop.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId + " for workshop"));

        return mapToResponse(tx);
    }

    // =========================================================================
    // ADMIN OPERATIONS (Strictly for ADMIN role audit and controlled adjustment)
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<WorkshopWalletResponse> getAllWallets(Pageable pageable) {
        return walletRepository.findAllByOrderByCreatedAtDesc(pageable).map(w -> new WorkshopWalletResponse(
                w.getId(),
                w.getWorkshop().getId(),
                w.getWorkshop().getBusinessName(),
                w.getBalance(),
                w.getCurrency(),
                w.getStatus(),
                w.getUpdatedAt()
        ));
    }

    @Transactional
    public WalletTransactionResponse adminAdjustWallet(AdminWalletAdjustmentRequest request, User adminUser) {
        Workshop workshop = workshopRepository.findById(request.getWorkshopId())
                .orElseThrow(() -> new ResourceNotFoundException("Workshop not found with id: " + request.getWorkshopId()));

        String auditRef = (request.getAuditReference() != null && !request.getAuditReference().isBlank())
                ? request.getAuditReference().trim()
                : "ADMIN-ADJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String fullDesc = "Admin Adjustment [" + adminUser.getEmail() + "]: " + request.getReason().trim();

        if (request.getType() == WalletTransactionType.CREDIT) {
            return credit(
                    workshop,
                    request.getAmount(),
                    WalletReferenceType.ADMIN_CREDIT,
                    auditRef,
                    null,
                    fullDesc
            );
        } else if (request.getType() == WalletTransactionType.DEBIT) {
            return debit(
                    workshop,
                    request.getAmount(),
                    WalletReferenceType.ADMIN_DEBIT,
                    auditRef,
                    null,
                    fullDesc
            );
        } else {
            throw new IllegalArgumentException("Unsupported adjustment type: " + request.getType());
        }
    }

    public WalletTransactionResponse mapToResponse(WalletTransaction tx) {
        return new WalletTransactionResponse(
                tx.getId(),
                tx.getWallet() != null ? tx.getWallet().getId() : null,
                tx.getWorkshop() != null ? tx.getWorkshop().getId() : null,
                tx.getType(),
                tx.getReferenceType(),
                tx.getAmount(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getReferenceId(),
                tx.getIdempotencyKey(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}

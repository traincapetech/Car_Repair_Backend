package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.WalletTransaction;
import com.carservice.backend.marketplace.enums.WalletReferenceType;
import com.carservice.backend.marketplace.enums.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    List<WalletTransaction> findByWorkshopIdOrderByCreatedAtDesc(Long workshopId);

    Page<WalletTransaction> findByWorkshopIdOrderByCreatedAtDesc(Long workshopId, Pageable pageable);

    Page<WalletTransaction> findByWorkshopIdAndTypeOrderByCreatedAtDesc(Long workshopId, WalletTransactionType type, Pageable pageable);

    List<WalletTransaction> findByWorkshopIdAndTypeOrderByCreatedAtDesc(Long workshopId, WalletTransactionType type);

    Page<WalletTransaction> findByWorkshopIdAndReferenceTypeOrderByCreatedAtDesc(Long workshopId, WalletReferenceType referenceType, Pageable pageable);

    Optional<WalletTransaction> findByIdAndWorkshopId(Long id, Long workshopId);

    Optional<WalletTransaction> findFirstByWorkshopIdAndIdempotencyKey(Long workshopId, String idempotencyKey);

    boolean existsByWorkshopIdAndReferenceTypeAndReferenceId(Long workshopId, WalletReferenceType referenceType, String referenceId);

    Page<WalletTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

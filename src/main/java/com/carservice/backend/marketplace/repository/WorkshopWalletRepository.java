package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.WorkshopWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkshopWalletRepository extends JpaRepository<WorkshopWallet, Long> {
    Optional<WorkshopWallet> findByWorkshopId(Long workshopId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WorkshopWallet w WHERE w.workshop.id = :workshopId")
    Optional<WorkshopWallet> findByWorkshopIdWithLock(@Param("workshopId") Long workshopId);

    Page<WorkshopWallet> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.WorkshopWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkshopWalletRepository extends JpaRepository<WorkshopWallet, Long> {
    Optional<WorkshopWallet> findByWorkshopId(Long workshopId);
}

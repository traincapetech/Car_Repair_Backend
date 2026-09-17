package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.PlatformConfigHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformConfigHistoryRepository extends JpaRepository<PlatformConfigHistory, Long> {

    List<PlatformConfigHistory> findByConfigKeyOrderByChangedAtDesc(String configKey);

    List<PlatformConfigHistory> findAllByOrderByChangedAtDesc();
}

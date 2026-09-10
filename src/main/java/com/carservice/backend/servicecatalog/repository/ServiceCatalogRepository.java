package com.carservice.backend.servicecatalog.repository;

import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {

    List<ServiceCatalog> findAllByOrderByNameAsc();

    List<ServiceCatalog> findAllByIsActiveTrueOrderByNameAsc();

    Optional<ServiceCatalog> findByIdAndIsActiveTrue(Long id);

    Optional<ServiceCatalog> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Modifying
    @Transactional
    @Query("UPDATE ServiceCatalog s SET s.isActive = false WHERE s.name LIKE 'Active Booking Service%' OR s.name LIKE 'Inactive Booking Service%' OR s.name LIKE 'Toggle Service%' OR s.name LIKE 'Test Service%' OR s.name LIKE 'Admin Viewable%' OR s.name LIKE 'Active General Service%' OR s.name LIKE 'Brake Pad Replacement %' OR s.name LIKE 'Wheel Alignment %' OR s.name LIKE 'Periodic Maintenance %' OR s.name LIKE 'Advanced AC Service %' OR s.name LIKE 'Inactive Detailing %' OR s.name LIKE 'Existing Oil Service %'")
    int deactivateTestArtifacts();
}

package com.carservice.backend.servicecatalog.repository;

import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        SELECT s FROM ServiceCatalog s
        WHERE (:category IS NULL OR s.category = :category)
          AND (:isActive IS NULL OR s.isActive = :isActive)
          AND (:search IS NULL OR :search = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    List<ServiceCatalog> findWithFilters(
            @Param("category") ServiceCategory category,
            @Param("isActive") Boolean isActive,
            @Param("search") String search,
            Sort sort
    );

    @Modifying
    @Transactional
    @Query("UPDATE ServiceCatalog s SET s.isActive = false WHERE s.name LIKE 'Active Booking Service%' OR s.name LIKE 'Inactive Booking Service%' OR s.name LIKE 'Toggle Service%' OR s.name LIKE 'Test Service%' OR s.name LIKE 'Admin Viewable%' OR s.name LIKE 'Active General Service%' OR s.name LIKE 'Brake Pad Replacement %' OR s.name LIKE 'Wheel Alignment %' OR s.name LIKE 'Periodic Maintenance %' OR s.name LIKE 'Advanced AC Service %' OR s.name LIKE 'Inactive Detailing %' OR s.name LIKE 'Existing Oil Service %' OR s.name LIKE 'AC Overhaul %' OR s.name LIKE 'AC Service %'")
    int deactivateTestArtifacts();
}

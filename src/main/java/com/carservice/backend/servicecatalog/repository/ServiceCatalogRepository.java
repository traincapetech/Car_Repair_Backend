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

    long countByIsActiveTrue();

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
    @Query("""
        UPDATE ServiceCatalog s SET s.isActive = false
        WHERE s.name NOT IN (
            'Basic Periodic Service',
            'Standard Periodic Maintenance',
            'Comprehensive Annual Service',
            'Regular AC Service & Gas Top-Up',
            'High Performance AC Overhaul',
            'AC Cooling Diagnostic & Leak Test',
            'Tyre Puncture & Stepney Assistance',
            '3D Wheel Alignment & Dynamic Balancing',
            'Complete Tyre Replacement & Fitment',
            'Emergency Battery Jumpstart Assistance',
            'Amaron / Exide OEM Battery Replacement',
            'Front Brake Pads Replacement',
            'Rear Brake Shoes & Drum Service',
            'Complete Brake Fluid Flush & Bleeding',
            'Deep Interior Spa & Sanitization',
            'Exterior Foam Wash & 3M Carnauba Wax',
            'Full Ceramic Coating (9H Armor)',
            'OBD-II Computerized Engine Scan',
            'Engine Carbon Cleaning & Throttle De-Carb',
            'Engine Coolant Flush & Radiator Service',
            'Emergency Fuel Assistance (Doorstep)',
            'Pre-Purchase Comprehensive 100-Point Inspection',
            'Front Bumper Denting & Painting (Single Panel)',
            'Complete Clutch Overhaul & Replacement',
            'Suspension Overhaul & Bushing Kit'
        )
    """)
    int deactivateTestArtifacts();
}

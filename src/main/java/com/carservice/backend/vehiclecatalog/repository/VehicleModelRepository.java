package com.carservice.backend.vehiclecatalog.repository;

import com.carservice.backend.vehiclecatalog.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    List<VehicleModel> findAllByBrandIdAndIsActiveTrueOrderByNameAsc(Long brandId);

    @Query("SELECT m FROM VehicleModel m WHERE m.brand.normalizedName = :normalizedBrandName AND m.isActive = true ORDER BY m.name ASC")
    List<VehicleModel> findAllByBrandNormalizedNameAndIsActiveTrueOrderByNameAsc(@Param("normalizedBrandName") String normalizedBrandName);

    Optional<VehicleModel> findByBrandIdAndNormalizedName(Long brandId, String normalizedName);

    boolean existsByBrandIdAndNormalizedName(Long brandId, String normalizedName);

    long countByBrandIdAndIsActiveTrue(Long brandId);
}

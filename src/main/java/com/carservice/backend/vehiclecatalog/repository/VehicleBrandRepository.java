package com.carservice.backend.vehiclecatalog.repository;

import com.carservice.backend.vehiclecatalog.entity.VehicleBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Long> {

    List<VehicleBrand> findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    List<VehicleBrand> findAllByIsActiveTrueOrderByNameAsc();

    Optional<VehicleBrand> findByNormalizedName(String normalizedName);

    boolean existsByNormalizedName(String normalizedName);

    Optional<VehicleBrand> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT b FROM VehicleBrand b LEFT JOIN FETCH b.models WHERE b.id = :id AND b.isActive = true")
    Optional<VehicleBrand> findByIdWithModels(Long id);
}

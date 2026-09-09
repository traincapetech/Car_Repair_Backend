package com.carservice.backend.vehiclecatalog.service;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.vehiclecatalog.dto.VehicleBrandResponse;
import com.carservice.backend.vehiclecatalog.dto.VehicleModelResponse;
import com.carservice.backend.vehiclecatalog.entity.VehicleBrand;
import com.carservice.backend.vehiclecatalog.entity.VehicleModel;
import com.carservice.backend.vehiclecatalog.repository.VehicleBrandRepository;
import com.carservice.backend.vehiclecatalog.repository.VehicleModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VehicleCatalogService {

    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;

    public VehicleCatalogService(
            VehicleBrandRepository brandRepository,
            VehicleModelRepository modelRepository
    ) {
        this.brandRepository = brandRepository;
        this.modelRepository = modelRepository;
    }

    /**
     * Retrieve all active brands sorted by display priority and name.
     */
    public List<VehicleBrandResponse> getAllActiveBrands() {
        List<VehicleBrand> brands = brandRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc();
        return brands.stream()
                .map(b -> {
                    long count = modelRepository.countByBrandIdAndIsActiveTrue(b.getId());
                    return VehicleBrandResponse.fromEntity(b, (int) count);
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all active models for a specific brand ID.
     */
    public List<VehicleModelResponse> getModelsByBrandId(Long brandId) {
        VehicleBrand brand = brandRepository.findByIdAndIsActiveTrue(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle brand not found with id: " + brandId));

        return modelRepository.findAllByBrandIdAndIsActiveTrueOrderByNameAsc(brand.getId())
                .stream()
                .map(VehicleModelResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all active models by brand name (case-insensitive normalized lookup).
     */
    public List<VehicleModelResponse> getModelsByBrandName(String brandName) {
        if (brandName == null || brandName.trim().isEmpty()) {
            return List.of();
        }
        String normalized = normalize(brandName);
        return modelRepository.findAllByBrandNormalizedNameAndIsActiveTrueOrderByNameAsc(normalized)
                .stream()
                .map(VehicleModelResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Normalizes a name string for unique constraints and deduplication.
     */
    public static String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ENGLISH);
    }

    // -------------------------------------------------------------
    // Administrative & Seeding Methods
    // -------------------------------------------------------------

    @Transactional
    public VehicleBrand getOrCreateBrand(String name, String countryOfOrigin, Integer displayOrder) {
        String normalized = normalize(name);
        return brandRepository.findByNormalizedName(normalized)
                .map(existing -> {
                    if (countryOfOrigin != null && existing.getCountryOfOrigin() == null) {
                        existing.setCountryOfOrigin(countryOfOrigin);
                    }
                    if (displayOrder != null && (existing.getDisplayOrder() == null || existing.getDisplayOrder() == 999)) {
                        existing.setDisplayOrder(displayOrder);
                    }
                    return brandRepository.save(existing);
                })
                .orElseGet(() -> {
                    VehicleBrand brand = new VehicleBrand(name.trim(), normalized, countryOfOrigin, displayOrder);
                    return brandRepository.save(brand);
                });
    }

    @Transactional
    public VehicleModel getOrCreateModel(
            VehicleBrand brand,
            String modelName,
            String bodyType,
            Integer yearIntroduced,
            Integer yearDiscontinued
    ) {
        String normalized = normalize(modelName);
        return modelRepository.findByBrandIdAndNormalizedName(brand.getId(), normalized)
                .map(existing -> {
                    if (bodyType != null && existing.getBodyType() == null) {
                        existing.setBodyType(bodyType);
                    }
                    if (yearIntroduced != null && existing.getYearIntroduced() == null) {
                        existing.setYearIntroduced(yearIntroduced);
                    }
                    if (yearDiscontinued != null && existing.getYearDiscontinued() == null) {
                        existing.setYearDiscontinued(yearDiscontinued);
                    }
                    return modelRepository.save(existing);
                })
                .orElseGet(() -> {
                    VehicleModel model = new VehicleModel(
                            brand,
                            modelName.trim(),
                            normalized,
                            bodyType,
                            yearIntroduced,
                            yearDiscontinued
                    );
                    return modelRepository.save(model);
                });
    }

    @Transactional
    public void setBrandActive(Long brandId, boolean active) {
        VehicleBrand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));
        brand.setIsActive(active);
        brandRepository.save(brand);
    }

    @Transactional
    public void setModelActive(Long modelId, boolean active) {
        VehicleModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Model not found with id: " + modelId));
        model.setIsActive(active);
        modelRepository.save(model);
    }
}

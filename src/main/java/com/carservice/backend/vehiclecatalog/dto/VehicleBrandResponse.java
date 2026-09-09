package com.carservice.backend.vehiclecatalog.dto;

import com.carservice.backend.vehiclecatalog.entity.VehicleBrand;

public class VehicleBrandResponse {

    private Long id;
    private String name;
    private String normalizedName;
    private String countryOfOrigin;
    private Integer displayOrder;
    private Boolean isActive;
    private Integer modelCount;

    public VehicleBrandResponse() {
    }

    public VehicleBrandResponse(
            Long id,
            String name,
            String normalizedName,
            String countryOfOrigin,
            Integer displayOrder,
            Boolean isActive,
            Integer modelCount
    ) {
        this.id = id;
        this.name = name;
        this.normalizedName = normalizedName;
        this.countryOfOrigin = countryOfOrigin;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
        this.modelCount = modelCount;
    }

    public static VehicleBrandResponse fromEntity(VehicleBrand brand, int modelCount) {
        if (brand == null) return null;
        return new VehicleBrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getNormalizedName(),
                brand.getCountryOfOrigin(),
                brand.getDisplayOrder(),
                brand.getIsActive(),
                modelCount
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(String countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getModelCount() {
        return modelCount;
    }

    public void setModelCount(Integer modelCount) {
        this.modelCount = modelCount;
    }
}

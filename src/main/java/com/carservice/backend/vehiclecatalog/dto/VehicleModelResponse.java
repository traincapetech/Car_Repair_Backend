package com.carservice.backend.vehiclecatalog.dto;

import com.carservice.backend.vehiclecatalog.entity.VehicleModel;

public class VehicleModelResponse {

    private Long id;
    private Long brandId;
    private String brandName;
    private String name;
    private String normalizedName;
    private String bodyType;
    private Integer yearIntroduced;
    private Integer yearDiscontinued;
    private Boolean isActive;

    public VehicleModelResponse() {
    }

    public VehicleModelResponse(
            Long id,
            Long brandId,
            String brandName,
            String name,
            String normalizedName,
            String bodyType,
            Integer yearIntroduced,
            Integer yearDiscontinued,
            Boolean isActive
    ) {
        this.id = id;
        this.brandId = brandId;
        this.brandName = brandName;
        this.name = name;
        this.normalizedName = normalizedName;
        this.bodyType = bodyType;
        this.yearIntroduced = yearIntroduced;
        this.yearDiscontinued = yearDiscontinued;
        this.isActive = isActive;
    }

    public static VehicleModelResponse fromEntity(VehicleModel model) {
        if (model == null) return null;
        return new VehicleModelResponse(
                model.getId(),
                model.getBrand() != null ? model.getBrand().getId() : null,
                model.getBrand() != null ? model.getBrand().getName() : null,
                model.getName(),
                model.getNormalizedName(),
                model.getBodyType(),
                model.getYearIntroduced(),
                model.getYearDiscontinued(),
                model.getIsActive()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
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

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public Integer getYearIntroduced() {
        return yearIntroduced;
    }

    public void setYearIntroduced(Integer yearIntroduced) {
        this.yearIntroduced = yearIntroduced;
    }

    public Integer getYearDiscontinued() {
        return yearDiscontinued;
    }

    public void setYearDiscontinued(Integer yearDiscontinued) {
        this.yearDiscontinued = yearDiscontinued;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

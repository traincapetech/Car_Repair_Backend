package com.carservice.backend.admin.dto;

import com.carservice.backend.servicecatalog.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminWorkshopCapabilityResponse {
    private Long id;
    private Long serviceCatalogId;
    private String name;
    private String description;
    private ServiceCategory category;
    private BigDecimal basePrice;
    private Integer estimatedDurationMinutes;
    private Boolean isServiceActive;
    private Boolean isCapabilityActive;
    private LocalDateTime linkedAt;

    public AdminWorkshopCapabilityResponse() {
    }

    public AdminWorkshopCapabilityResponse(
            Long id,
            Long serviceCatalogId,
            String name,
            String description,
            ServiceCategory category,
            BigDecimal basePrice,
            Integer estimatedDurationMinutes,
            Boolean isServiceActive,
            Boolean isCapabilityActive,
            LocalDateTime linkedAt
    ) {
        this.id = id;
        this.serviceCatalogId = serviceCatalogId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.basePrice = basePrice;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.isServiceActive = isServiceActive;
        this.isCapabilityActive = isCapabilityActive;
        this.linkedAt = linkedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getServiceCatalogId() {
        return serviceCatalogId;
    }

    public void setServiceCatalogId(Long serviceCatalogId) {
        this.serviceCatalogId = serviceCatalogId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public void setCategory(ServiceCategory category) {
        this.category = category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Boolean getIsServiceActive() {
        return isServiceActive;
    }

    public void setIsServiceActive(Boolean serviceActive) {
        isServiceActive = serviceActive;
    }

    public Boolean getIsCapabilityActive() {
        return isCapabilityActive;
    }

    public void setIsCapabilityActive(Boolean capabilityActive) {
        isCapabilityActive = capabilityActive;
    }

    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(LocalDateTime linkedAt) {
        this.linkedAt = linkedAt;
    }
}

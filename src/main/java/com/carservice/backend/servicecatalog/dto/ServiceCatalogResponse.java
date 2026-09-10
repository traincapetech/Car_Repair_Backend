package com.carservice.backend.servicecatalog.dto;

import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.enums.DiscountType;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ServiceCatalogResponse {

    private Long id;
    private String name;
    private String description;
    private ServiceCategory category;
    private BigDecimal basePrice;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal finalPrice;
    private Integer estimatedDurationMinutes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceCatalogResponse() {
    }

    public ServiceCatalogResponse(
            Long id,
            String name,
            String description,
            ServiceCategory category,
            BigDecimal basePrice,
            Integer estimatedDurationMinutes,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, name, description, category, basePrice, DiscountType.NO_DISCOUNT, BigDecimal.ZERO, basePrice, estimatedDurationMinutes, isActive, createdAt, updatedAt);
    }

    public ServiceCatalogResponse(
            Long id,
            String name,
            String description,
            ServiceCategory category,
            BigDecimal basePrice,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal finalPrice,
            Integer estimatedDurationMinutes,
            Boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.basePrice = basePrice;
        this.discountType = discountType != null ? discountType : DiscountType.NO_DISCOUNT;
        this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
        this.finalPrice = finalPrice != null ? finalPrice : basePrice;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ServiceCatalogResponse fromEntity(ServiceCatalog entity) {
        if (entity == null) {
            return null;
        }
        return new ServiceCatalogResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getBasePrice(),
                entity.getDiscountType(),
                entity.getDiscountValue(),
                entity.calculateFinalPrice(),
                entity.getEstimatedDurationMinutes(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
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

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

package com.carservice.backend.servicecatalog.dto;

import com.carservice.backend.servicecatalog.enums.DiscountType;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateServiceCatalogRequest {

    @NotBlank(message = "Service name is required")
    @Size(min = 2, max = 100, message = "Service name must be between 2 and 100 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Service category is required")
    private ServiceCategory category;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be positive")
    @Digits(integer = 8, fraction = 2, message = "Base price format is invalid")
    private BigDecimal basePrice;

    private DiscountType discountType = DiscountType.NO_DISCOUNT;

    @DecimalMin(value = "0.00", message = "Discount value cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Discount value format is invalid")
    private BigDecimal discountValue = BigDecimal.ZERO;

    @NotNull(message = "Estimated duration is required")
    @Min(value = 1, message = "Estimated duration must be at least 1 minute")
    @Max(value = 1440, message = "Estimated duration cannot exceed 1440 minutes (24 hours)")
    private Integer estimatedDurationMinutes;

    private Boolean isActive = true;

    public CreateServiceCatalogRequest() {
    }

    public CreateServiceCatalogRequest(
            String name,
            String description,
            ServiceCategory category,
            BigDecimal basePrice,
            Integer estimatedDurationMinutes,
            Boolean isActive
    ) {
        this(name, description, category, basePrice, DiscountType.NO_DISCOUNT, BigDecimal.ZERO, estimatedDurationMinutes, isActive);
    }

    public CreateServiceCatalogRequest(
            String name,
            String description,
            ServiceCategory category,
            BigDecimal basePrice,
            DiscountType discountType,
            BigDecimal discountValue,
            Integer estimatedDurationMinutes,
            Boolean isActive
    ) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.basePrice = basePrice;
        this.discountType = discountType != null ? discountType : DiscountType.NO_DISCOUNT;
        this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.isActive = isActive != null ? isActive : true;
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
}

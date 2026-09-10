package com.carservice.backend.servicecatalog.entity;

import com.carservice.backend.servicecatalog.enums.DiscountType;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "service_catalog",
        indexes = {
                @Index(name = "idx_service_catalog_category", columnList = "category"),
                @Index(name = "idx_service_catalog_is_active", columnList = "is_active"),
                @Index(name = "idx_service_catalog_name", columnList = "name")
        }
)
public class ServiceCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceCategory category;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType = DiscountType.NO_DISCOUNT;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ServiceCatalog() {
    }

    public ServiceCatalog(
            String name,
            String description,
            ServiceCategory category,
            BigDecimal basePrice,
            Integer estimatedDurationMinutes,
            Boolean isActive
    ) {
        this(name, description, category, basePrice, DiscountType.NO_DISCOUNT, BigDecimal.ZERO, estimatedDurationMinutes, isActive);
    }

    public ServiceCatalog(
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
        this.basePrice = basePrice != null ? basePrice.setScale(2, RoundingMode.HALF_UP) : null;
        this.discountType = discountType != null ? discountType : DiscountType.NO_DISCOUNT;
        this.discountValue = discountValue != null ? discountValue.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.isActive = isActive != null ? isActive : true;
        validateDiscount();
    }

    public void validateDiscount() {
        if (this.basePrice != null && this.basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price cannot be negative");
        }
        if (this.discountValue != null && this.discountValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount value cannot be negative");
        }
        if (this.discountType == DiscountType.PERCENTAGE) {
            if (this.discountValue != null && this.discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
            }
        } else if (this.discountType == DiscountType.FIXED_AMOUNT) {
            if (this.basePrice != null && this.discountValue != null && this.discountValue.compareTo(this.basePrice) > 0) {
                throw new IllegalArgumentException("Fixed discount cannot exceed base price");
            }
        }
    }

    public BigDecimal calculateFinalPrice() {
        if (this.basePrice == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.discountType == null || this.discountType == DiscountType.NO_DISCOUNT || this.discountValue == null) {
            return this.basePrice.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.discountType == DiscountType.PERCENTAGE) {
            BigDecimal discountAmount = this.basePrice
                    .multiply(this.discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal finalPrice = this.basePrice.subtract(discountAmount);
            return finalPrice.compareTo(BigDecimal.ZERO) < 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : finalPrice.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.discountType == DiscountType.FIXED_AMOUNT) {
            BigDecimal finalPrice = this.basePrice.subtract(this.discountValue);
            return finalPrice.compareTo(BigDecimal.ZERO) < 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : finalPrice.setScale(2, RoundingMode.HALF_UP);
        }
        return this.basePrice.setScale(2, RoundingMode.HALF_UP);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.discountType == null) {
            this.discountType = DiscountType.NO_DISCOUNT;
        }
        if (this.discountValue == null) {
            this.discountValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        validateDiscount();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.discountType == null) {
            this.discountType = DiscountType.NO_DISCOUNT;
        }
        if (this.discountValue == null) {
            this.discountValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        validateDiscount();
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
        this.basePrice = basePrice != null ? basePrice.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType != null ? discountType : DiscountType.NO_DISCOUNT;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue != null ? discountValue.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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

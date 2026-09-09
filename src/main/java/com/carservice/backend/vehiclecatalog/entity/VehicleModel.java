package com.carservice.backend.vehiclecatalog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "vehicle_models",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_brand_normalized_model", columnNames = {"brand_id", "normalized_name"})
        },
        indexes = {
                @Index(name = "idx_vehicle_models_brand_id", columnList = "brand_id"),
                @Index(name = "idx_model_normalized_name", columnList = "normalized_name")
        }
)
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private VehicleBrand brand;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Column(name = "body_type", length = 50)
    private String bodyType;

    @Column(name = "year_introduced")
    private Integer yearIntroduced;

    @Column(name = "year_discontinued")
    private Integer yearDiscontinued;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public VehicleModel() {
    }

    public VehicleModel(
            VehicleBrand brand,
            String name,
            String normalizedName,
            String bodyType,
            Integer yearIntroduced,
            Integer yearDiscontinued
    ) {
        this.brand = brand;
        this.name = name;
        this.normalizedName = normalizedName;
        this.bodyType = bodyType;
        this.yearIntroduced = yearIntroduced;
        this.yearDiscontinued = yearDiscontinued;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VehicleBrand getBrand() {
        return brand;
    }

    public void setBrand(VehicleBrand brand) {
        this.brand = brand;
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

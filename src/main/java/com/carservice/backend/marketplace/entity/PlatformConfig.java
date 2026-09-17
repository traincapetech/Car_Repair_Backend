package com.carservice.backend.marketplace.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_configs")
public class PlatformConfig {

    @Id
    @Column(name = "config_key", length = 50, nullable = false)
    private String configKey;

    @Column(name = "config_value", length = 255, nullable = false)
    private String configValue;

    @Column(name = "friendly_name", length = 100)
    private String friendlyName;

    @Column(name = "data_type", length = 30)
    private String dataType; // DECIMAL, INTEGER, BOOLEAN, STRING

    @Column(name = "category", length = 50)
    private String category; // MARKETPLACE, MATCHING, PAYMENT, WALLET

    @Column(name = "unit", length = 30)
    private String unit; // INR, km, minutes, count, etc.

    @Column(name = "min_val", precision = 12, scale = 2)
    private BigDecimal minVal;

    @Column(name = "max_val", precision = 12, scale = 2)
    private BigDecimal maxVal;

    @Column(length = 500)
    private String description;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    @Column(name = "version")
    private Long version = 0L;

    public PlatformConfig() {
    }

    public PlatformConfig(String configKey, String configValue, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = 0L;
    }

    public PlatformConfig(
            String configKey,
            String configValue,
            String friendlyName,
            String dataType,
            String category,
            String unit,
            BigDecimal minVal,
            BigDecimal maxVal,
            String description,
            String updatedBy
    ) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.friendlyName = friendlyName;
        this.dataType = dataType;
        this.category = category;
        this.unit = unit;
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.description = description;
        this.updatedBy = updatedBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = 0L;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getMinVal() {
        return minVal;
    }

    public void setMinVal(BigDecimal minVal) {
        this.minVal = minVal;
    }

    public BigDecimal getMaxVal() {
        return maxVal;
    }

    public void setMaxVal(BigDecimal maxVal) {
        this.maxVal = maxVal;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

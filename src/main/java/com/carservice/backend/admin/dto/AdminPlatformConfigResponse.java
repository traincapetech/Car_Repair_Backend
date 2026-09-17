package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.entity.PlatformConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminPlatformConfigResponse {

    private String configKey;
    private String configValue;
    private String friendlyName;
    private String dataType;
    private String category;
    private String unit;
    private BigDecimal minVal;
    private BigDecimal maxVal;
    private String description;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public AdminPlatformConfigResponse() {
    }

    public AdminPlatformConfigResponse(PlatformConfig entity) {
        if (entity != null) {
            this.configKey = entity.getConfigKey();
            this.configValue = entity.getConfigValue();
            this.friendlyName = entity.getFriendlyName();
            this.dataType = entity.getDataType();
            this.category = entity.getCategory();
            this.unit = entity.getUnit();
            this.minVal = entity.getMinVal();
            this.maxVal = entity.getMaxVal();
            this.description = entity.getDescription();
            this.updatedBy = entity.getUpdatedBy();
            this.createdAt = entity.getCreatedAt();
            this.updatedAt = entity.getUpdatedAt();
            this.version = entity.getVersion();
        }
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

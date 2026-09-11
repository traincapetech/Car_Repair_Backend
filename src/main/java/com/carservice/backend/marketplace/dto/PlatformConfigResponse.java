package com.carservice.backend.marketplace.dto;

import java.time.LocalDateTime;

public class PlatformConfigResponse {

    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updatedAt;

    public PlatformConfigResponse() {
    }

    public PlatformConfigResponse(String configKey, String configValue, String description, LocalDateTime updatedAt) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.updatedAt = updatedAt;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

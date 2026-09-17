package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.entity.PlatformConfigHistory;

import java.time.LocalDateTime;

public class AdminPlatformConfigHistoryResponse {

    private Long id;
    private String configKey;
    private String oldValue;
    private String newValue;
    private String category;
    private String changedBy;
    private Long changedByUserId;
    private String reason;
    private LocalDateTime changedAt;

    public AdminPlatformConfigHistoryResponse() {
    }

    public AdminPlatformConfigHistoryResponse(PlatformConfigHistory entity) {
        if (entity != null) {
            this.id = entity.getId();
            this.configKey = entity.getConfigKey();
            this.oldValue = entity.getOldValue();
            this.newValue = entity.getNewValue();
            this.category = entity.getCategory();
            this.changedBy = entity.getChangedBy();
            this.changedByUserId = entity.getChangedByUserId();
            this.reason = entity.getReason();
            this.changedAt = entity.getChangedAt();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public Long getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(Long changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}

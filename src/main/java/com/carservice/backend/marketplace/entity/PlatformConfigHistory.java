package com.carservice.backend.marketplace.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "platform_config_history",
        indexes = {
                @Index(name = "idx_pch_key", columnList = "config_key"),
                @Index(name = "idx_pch_changed_at", columnList = "changed_at")
        }
)
public class PlatformConfigHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", length = 50, nullable = false)
    private String configKey;

    @Column(name = "old_value", length = 255)
    private String oldValue;

    @Column(name = "new_value", length = 255, nullable = false)
    private String newValue;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    public PlatformConfigHistory() {
    }

    public PlatformConfigHistory(
            String configKey,
            String oldValue,
            String newValue,
            String category,
            String changedBy,
            Long changedByUserId,
            String reason
    ) {
        this.configKey = configKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.category = category;
        this.changedBy = changedBy;
        this.changedByUserId = changedByUserId;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
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

package com.carservice.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminPlatformConfigUpdateRequest {

    @NotBlank(message = "Configuration value cannot be blank")
    @JsonAlias({"configValue"})
    private String value;

    @NotBlank(message = "A justification reason is required for updating configuration")
    @Size(min = 3, max = 500, message = "Reason must be between 3 and 500 characters")
    private String reason;

    private Long version;

    public AdminPlatformConfigUpdateRequest() {
    }

    public AdminPlatformConfigUpdateRequest(String value, String reason) {
        this.value = value;
        this.reason = reason;
    }

    public AdminPlatformConfigUpdateRequest(String value, String reason, Long version) {
        this.value = value;
        this.reason = reason;
        this.version = version;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getConfigValue() {
        return value;
    }

    public void setConfigValue(String configValue) {
        this.value = configValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

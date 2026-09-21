package com.carservice.backend.admin.dto;

import java.util.Map;

public class AdminSystemHealthResponse {

    private String status;
    private String subsystem;
    private String version;
    private String serverTime;
    private long uptimeSeconds;
    private Map<String, Object> components;

    public AdminSystemHealthResponse() {
    }

    public AdminSystemHealthResponse(String status, String subsystem, String version, String serverTime, long uptimeSeconds, Map<String, Object> components) {
        this.status = status;
        this.subsystem = subsystem;
        this.version = version;
        this.serverTime = serverTime;
        this.uptimeSeconds = uptimeSeconds;
        this.components = components;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubsystem() {
        return subsystem;
    }

    public void setSubsystem(String subsystem) {
        this.subsystem = subsystem;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public Map<String, Object> getComponents() {
        return components;
    }

    public void setComponents(Map<String, Object> components) {
        this.components = components;
    }
}

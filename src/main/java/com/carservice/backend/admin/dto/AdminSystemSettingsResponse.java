package com.carservice.backend.admin.dto;

import java.util.Map;

public class AdminSystemSettingsResponse {

    private String platformName;
    private String environment;
    private String version;
    private String databaseEngine;
    private String javaVersion;
    private String springBootVersion;
    private String serverTime;
    private String activeProfile;
    private Map<String, Object> marketplaceRules;
    private Map<String, Object> securityPolicy;
    private Map<String, Object> paymentGateway;

    public AdminSystemSettingsResponse() {
    }

    public AdminSystemSettingsResponse(
            String platformName,
            String environment,
            String version,
            String databaseEngine,
            String javaVersion,
            String springBootVersion,
            String serverTime,
            String activeProfile,
            Map<String, Object> marketplaceRules,
            Map<String, Object> securityPolicy,
            Map<String, Object> paymentGateway
    ) {
        this.platformName = platformName;
        this.environment = environment;
        this.version = version;
        this.databaseEngine = databaseEngine;
        this.javaVersion = javaVersion;
        this.springBootVersion = springBootVersion;
        this.serverTime = serverTime;
        this.activeProfile = activeProfile;
        this.marketplaceRules = marketplaceRules;
        this.securityPolicy = securityPolicy;
        this.paymentGateway = paymentGateway;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    public void setDatabaseEngine(String databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getSpringBootVersion() {
        return springBootVersion;
    }

    public void setSpringBootVersion(String springBootVersion) {
        this.springBootVersion = springBootVersion;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(String activeProfile) {
        this.activeProfile = activeProfile;
    }

    public Map<String, Object> getMarketplaceRules() {
        return marketplaceRules;
    }

    public void setMarketplaceRules(Map<String, Object> marketplaceRules) {
        this.marketplaceRules = marketplaceRules;
    }

    public Map<String, Object> getSecurityPolicy() {
        return securityPolicy;
    }

    public void setSecurityPolicy(Map<String, Object> securityPolicy) {
        this.securityPolicy = securityPolicy;
    }

    public Map<String, Object> getPaymentGateway() {
        return paymentGateway;
    }

    public void setPaymentGateway(Map<String, Object> paymentGateway) {
        this.paymentGateway = paymentGateway;
    }
}

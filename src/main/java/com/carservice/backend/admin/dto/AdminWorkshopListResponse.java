package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminWorkshopListResponse {
    private Long id;
    private String businessName;
    private String ownerName;
    private String email;
    private String phone;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal serviceRadiusKm;
    private WorkshopVerificationStatus verificationStatus;
    private Boolean isActive;
    private String statusReason;
    private long capabilitiesCount;
    private long opportunitiesCount;
    private long acceptedOpportunitiesCount;
    private long completedJobsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminWorkshopListResponse() {
    }

    public AdminWorkshopListResponse(
            Long id,
            String businessName,
            String ownerName,
            String email,
            String phone,
            String city,
            String state,
            String pincode,
            BigDecimal serviceRadiusKm,
            WorkshopVerificationStatus verificationStatus,
            Boolean isActive,
            String statusReason,
            long capabilitiesCount,
            long opportunitiesCount,
            long acceptedOpportunitiesCount,
            long completedJobsCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.serviceRadiusKm = serviceRadiusKm;
        this.verificationStatus = verificationStatus;
        this.isActive = isActive;
        this.statusReason = statusReason;
        this.capabilitiesCount = capabilitiesCount;
        this.opportunitiesCount = opportunitiesCount;
        this.acceptedOpportunitiesCount = acceptedOpportunitiesCount;
        this.completedJobsCount = completedJobsCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public BigDecimal getServiceRadiusKm() {
        return serviceRadiusKm;
    }

    public void setServiceRadiusKm(BigDecimal serviceRadiusKm) {
        this.serviceRadiusKm = serviceRadiusKm;
    }

    public WorkshopVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(WorkshopVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public long getCapabilitiesCount() {
        return capabilitiesCount;
    }

    public void setCapabilitiesCount(long capabilitiesCount) {
        this.capabilitiesCount = capabilitiesCount;
    }

    public long getOpportunitiesCount() {
        return opportunitiesCount;
    }

    public void setOpportunitiesCount(long opportunitiesCount) {
        this.opportunitiesCount = opportunitiesCount;
    }

    public long getAcceptedOpportunitiesCount() {
        return acceptedOpportunitiesCount;
    }

    public void setAcceptedOpportunitiesCount(long acceptedOpportunitiesCount) {
        this.acceptedOpportunitiesCount = acceptedOpportunitiesCount;
    }

    public long getCompletedJobsCount() {
        return completedJobsCount;
    }

    public void setCompletedJobsCount(long completedJobsCount) {
        this.completedJobsCount = completedJobsCount;
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

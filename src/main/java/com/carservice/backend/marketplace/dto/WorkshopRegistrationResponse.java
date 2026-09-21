package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WorkshopRegistrationResponse {

    private Long workshopId;
    private Long userId;
    private String businessName;
    private String ownerName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private WorkshopVerificationStatus verificationStatus;
    private Boolean isActive;
    private BigDecimal walletBalance;
    private int serviceCount;
    private String message;
    private LocalDateTime registeredAt;

    public WorkshopRegistrationResponse() {
    }

    public WorkshopRegistrationResponse(
            Long workshopId,
            Long userId,
            String businessName,
            String ownerName,
            String email,
            String phone,
            String address,
            String city,
            String state,
            String pincode,
            BigDecimal latitude,
            BigDecimal longitude,
            WorkshopVerificationStatus verificationStatus,
            Boolean isActive,
            BigDecimal walletBalance,
            int serviceCount,
            String message,
            LocalDateTime registeredAt
    ) {
        this.workshopId = workshopId;
        this.userId = userId;
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.verificationStatus = verificationStatus;
        this.isActive = isActive;
        this.walletBalance = walletBalance;
        this.serviceCount = serviceCount;
        this.message = message;
        this.registeredAt = registeredAt;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
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

    public int getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(int serviceCount) {
        this.serviceCount = serviceCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
}

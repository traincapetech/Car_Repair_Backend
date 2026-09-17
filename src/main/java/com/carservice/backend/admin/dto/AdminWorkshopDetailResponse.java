package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.enums.WalletStatus;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminWorkshopDetailResponse {
    private Long id;
    private String businessName;

    // Owner details
    private Long ownerId;
    private String ownerName;
    private String email;
    private String phone;

    // Location & Coverage
    private String address;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal serviceRadiusKm;

    // Status & Audit
    private WorkshopVerificationStatus verificationStatus;
    private Boolean isActive;
    private String statusReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Wallet Summary
    private BigDecimal walletBalance = BigDecimal.ZERO;
    private String walletCurrency = "INR";
    private WalletStatus walletStatus = WalletStatus.ACTIVE;
    private BigDecimal totalWalletCredits = BigDecimal.ZERO;
    private BigDecimal totalWalletDebits = BigDecimal.ZERO;
    private long totalWalletTransactions = 0;

    // Marketplace Metrics
    private long availableOpportunities = 0;
    private long acceptedOpportunities = 0;
    private long unlockedOpportunities = 0;
    private long transferredOpportunities = 0;
    private long lostOpportunities = 0;
    private long completedJobs = 0;
    private long totalOpportunities = 0;
    private double acceptanceRate = 0.0;

    // Bookings summary
    private long totalBookings = 0;

    // Capabilities
    private List<AdminWorkshopCapabilityResponse> capabilities = new ArrayList<>();

    public AdminWorkshopDetailResponse() {
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

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
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

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
    }

    public String getWalletCurrency() {
        return walletCurrency;
    }

    public void setWalletCurrency(String walletCurrency) {
        this.walletCurrency = walletCurrency;
    }

    public WalletStatus getStatus() {
        return walletStatus;
    }

    public void setWalletStatus(WalletStatus walletStatus) {
        this.walletStatus = walletStatus;
    }

    public WalletStatus getWalletStatus() {
        return walletStatus;
    }

    public BigDecimal getTotalWalletCredits() {
        return totalWalletCredits;
    }

    public void setTotalWalletCredits(BigDecimal totalWalletCredits) {
        this.totalWalletCredits = totalWalletCredits;
    }

    public BigDecimal getTotalWalletDebits() {
        return totalWalletDebits;
    }

    public void setTotalWalletDebits(BigDecimal totalWalletDebits) {
        this.totalWalletDebits = totalWalletDebits;
    }

    public long getTotalWalletTransactions() {
        return totalWalletTransactions;
    }

    public void setTotalWalletTransactions(long totalWalletTransactions) {
        this.totalWalletTransactions = totalWalletTransactions;
    }

    public long getAvailableOpportunities() {
        return availableOpportunities;
    }

    public void setAvailableOpportunities(long availableOpportunities) {
        this.availableOpportunities = availableOpportunities;
    }

    public long getAcceptedOpportunities() {
        return acceptedOpportunities;
    }

    public void setAcceptedOpportunities(long acceptedOpportunities) {
        this.acceptedOpportunities = acceptedOpportunities;
    }

    public long getUnlockedOpportunities() {
        return unlockedOpportunities;
    }

    public void setUnlockedOpportunities(long unlockedOpportunities) {
        this.unlockedOpportunities = unlockedOpportunities;
    }

    public long getTransferredOpportunities() {
        return transferredOpportunities;
    }

    public void setTransferredOpportunities(long transferredOpportunities) {
        this.transferredOpportunities = transferredOpportunities;
    }

    public long getLostOpportunities() {
        return lostOpportunities;
    }

    public void setLostOpportunities(long lostOpportunities) {
        this.lostOpportunities = lostOpportunities;
    }

    public long getCompletedJobs() {
        return completedJobs;
    }

    public void setCompletedJobs(long completedJobs) {
        this.completedJobs = completedJobs;
    }

    public long getTotalOpportunities() {
        return totalOpportunities;
    }

    public void setTotalOpportunities(long totalOpportunities) {
        this.totalOpportunities = totalOpportunities;
    }

    public double getAcceptanceRate() {
        return acceptanceRate;
    }

    public void setAcceptanceRate(double acceptanceRate) {
        this.acceptanceRate = acceptanceRate;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public List<AdminWorkshopCapabilityResponse> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<AdminWorkshopCapabilityResponse> capabilities) {
        this.capabilities = capabilities;
    }
}

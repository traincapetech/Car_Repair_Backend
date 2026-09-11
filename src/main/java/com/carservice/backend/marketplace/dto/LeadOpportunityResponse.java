package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.OpportunityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LeadOpportunityResponse {

    private Long id;
    private Long serviceRequestId;
    private String requestReference;
    private Long workshopId;
    private String workshopName;
    private BigDecimal feeSnapshot;
    private OpportunityStatus status;
    private LocalDateTime viewedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime paidAt;
    private LocalDateTime unlockedAt;
    private LocalDateTime transferredAt;
    private String transferReason;

    private LocalDate preferredDate;
    private String preferredTimeSlot;
    private String city;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String customerNotes;

    private String vehicleSummary;
    private List<ServiceRequestItemResponse> requestedServices = new ArrayList<>();
    private BigDecimal totalServiceAmount;

    private CustomerProfileDto customerProfile;
    private boolean customerDetailsUnlocked;
    private LeadPaymentResponse payment;

    private LocalDateTime createdAt;

    public LeadOpportunityResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Long serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public String getRequestReference() {
        return requestReference;
    }

    public void setRequestReference(String requestReference) {
        this.requestReference = requestReference;
    }

    public Long getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId) {
        this.workshopId = workshopId;
    }

    public String getWorkshopName() {
        return workshopName;
    }

    public void setWorkshopName(String workshopName) {
        this.workshopName = workshopName;
    }

    public BigDecimal getFeeSnapshot() {
        return feeSnapshot;
    }

    public void setFeeSnapshot(BigDecimal feeSnapshot) {
        this.feeSnapshot = feeSnapshot;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(OpportunityStatus status) {
        this.status = status;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(LocalDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public LocalDateTime getTransferredAt() {
        return transferredAt;
    }

    public void setTransferredAt(LocalDateTime transferredAt) {
        this.transferredAt = transferredAt;
    }

    public String getTransferReason() {
        return transferReason;
    }

    public void setTransferReason(String transferReason) {
        this.transferReason = transferReason;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }

    public String getPreferredTimeSlot() {
        return preferredTimeSlot;
    }

    public void setPreferredTimeSlot(String preferredTimeSlot) {
        this.preferredTimeSlot = preferredTimeSlot;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public String getVehicleSummary() {
        return vehicleSummary;
    }

    public void setVehicleSummary(String vehicleSummary) {
        this.vehicleSummary = vehicleSummary;
    }

    public List<ServiceRequestItemResponse> getRequestedServices() {
        return requestedServices;
    }

    public void setRequestedServices(List<ServiceRequestItemResponse> requestedServices) {
        this.requestedServices = requestedServices;
    }

    public BigDecimal getTotalServiceAmount() {
        return totalServiceAmount;
    }

    public void setTotalServiceAmount(BigDecimal totalServiceAmount) {
        this.totalServiceAmount = totalServiceAmount;
    }

    public CustomerProfileDto getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(CustomerProfileDto customerProfile) {
        this.customerProfile = customerProfile;
    }

    public boolean isCustomerDetailsUnlocked() {
        return customerDetailsUnlocked;
    }

    public void setCustomerDetailsUnlocked(boolean customerDetailsUnlocked) {
        this.customerDetailsUnlocked = customerDetailsUnlocked;
    }

    public LeadPaymentResponse getPayment() {
        return payment;
    }

    public void setPayment(LeadPaymentResponse payment) {
        this.payment = payment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

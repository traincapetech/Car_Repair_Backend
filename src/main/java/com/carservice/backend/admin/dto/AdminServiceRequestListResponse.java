package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.enums.ServiceRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminServiceRequestListResponse {

    private Long id;
    private String requestReference;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private Long vehicleId;
    private String vehicleSummary;

    private int serviceCount;
    private String serviceNamesSummary;

    private String city;
    private String address;
    private String pincode;

    private LocalDate preferredDate;
    private String preferredTimeSlot;

    private BigDecimal totalAmount;
    private ServiceRequestStatus status;

    private Long assignedWorkshopId;
    private String assignedWorkshopName;

    private long opportunityCount;
    private String bookingReference;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminServiceRequestListResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestReference() {
        return requestReference;
    }

    public void setRequestReference(String requestReference) {
        this.requestReference = requestReference;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleSummary() {
        return vehicleSummary;
    }

    public void setVehicleSummary(String vehicleSummary) {
        this.vehicleSummary = vehicleSummary;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(int serviceCount) {
        this.serviceCount = serviceCount;
    }

    public String getServiceNamesSummary() {
        return serviceNamesSummary;
    }

    public void setServiceNamesSummary(String serviceNamesSummary) {
        this.serviceNamesSummary = serviceNamesSummary;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ServiceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceRequestStatus status) {
        this.status = status;
    }

    public Long getAssignedWorkshopId() {
        return assignedWorkshopId;
    }

    public void setAssignedWorkshopId(Long assignedWorkshopId) {
        this.assignedWorkshopId = assignedWorkshopId;
    }

    public String getAssignedWorkshopName() {
        return assignedWorkshopName;
    }

    public void setAssignedWorkshopName(String assignedWorkshopName) {
        this.assignedWorkshopName = assignedWorkshopName;
    }

    public long getOpportunityCount() {
        return opportunityCount;
    }

    public void setOpportunityCount(long opportunityCount) {
        this.opportunityCount = opportunityCount;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
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

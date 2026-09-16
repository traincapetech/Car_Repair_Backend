package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.enums.ServiceRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminCustomerServiceRequestResponse {
    private Long id;
    private String requestReference;
    private AdminCustomerVehicleResponse vehicle;
    private String city;
    private String address;
    private String pincode;
    private LocalDate preferredDate;
    private String preferredTimeSlot;
    private ServiceRequestStatus status;
    private BigDecimal totalAmount;
    private String bookingReference;
    private Long assignedWorkshopId;
    private String assignedWorkshopName;
    private String requestedServices;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminCustomerServiceRequestResponse() {
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

    public AdminCustomerVehicleResponse getVehicle() {
        return vehicle;
    }

    public void setVehicle(AdminCustomerVehicleResponse vehicle) {
        this.vehicle = vehicle;
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

    public ServiceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceRequestStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
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

    public String getRequestedServices() {
        return requestedServices;
    }

    public void setRequestedServices(String requestedServices) {
        this.requestedServices = requestedServices;
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

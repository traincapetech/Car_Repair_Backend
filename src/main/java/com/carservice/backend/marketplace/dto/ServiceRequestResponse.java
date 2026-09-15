package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.ServiceRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceRequestResponse {

    private Long id;
    private String requestReference;
    private Long userId;
    private Long vehicleId;
    private String vehicleSummary;
    private String city;
    private String address;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDate preferredDate;
    private String preferredTimeSlot;
    private String customerNotes;
    private ServiceRequestStatus status;
    private BigDecimal totalAmount;
    private Long assignedWorkshopId;
    private String assignedWorkshopName;
    private String assignedWorkshopPhone;
    private String assignedWorkshopAddress;
    private String bookingReference;
    private Long bookingId;
    private String jobStatus;
    private Boolean isCancellable;
    private List<ServiceRequestItemResponse> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceRequestResponse() {
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
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

    public List<ServiceRequestItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ServiceRequestItemResponse> items) {
        this.items = items;
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

    public String getAssignedWorkshopPhone() {
        return assignedWorkshopPhone;
    }

    public void setAssignedWorkshopPhone(String assignedWorkshopPhone) {
        this.assignedWorkshopPhone = assignedWorkshopPhone;
    }

    public String getAssignedWorkshopAddress() {
        return assignedWorkshopAddress;
    }

    public void setAssignedWorkshopAddress(String assignedWorkshopAddress) {
        this.assignedWorkshopAddress = assignedWorkshopAddress;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public Boolean getIsCancellable() {
        return isCancellable;
    }

    public void setIsCancellable(Boolean cancellable) {
        isCancellable = cancellable;
    }
}

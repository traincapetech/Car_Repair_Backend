package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.enums.WorkshopJobStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminWorkshopJobListResponse {

    private Long id;
    private String jobReference;
    private WorkshopJobStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime updatedAt;

    // Workshop
    private Long workshopId;
    private String workshopName;
    private String workshopCity;

    // Customer
    private Long customerId;
    private String customerName;
    private String customerPhone;

    // Vehicle
    private Long vehicleId;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleRegistrationNumber;

    // Request & Booking
    private Long serviceRequestId;
    private String serviceRequestReference;
    private Long bookingId;
    private String bookingReference;
    private BigDecimal bookingTotalAmount;
    private int serviceCount;
    private String primaryServiceName;

    public AdminWorkshopJobListResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobReference() {
        return jobReference;
    }

    public void setJobReference(String jobReference) {
        this.jobReference = jobReference;
    }

    public WorkshopJobStatus getStatus() {
        return status;
    }

    public void setStatus(WorkshopJobStatus status) {
        this.status = status;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getWorkshopCity() {
        return workshopCity;
    }

    public void setWorkshopCity(String workshopCity) {
        this.workshopCity = workshopCity;
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

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public void setVehicleMake(String vehicleMake) {
        this.vehicleMake = vehicleMake;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    public void setVehicleRegistrationNumber(String vehicleRegistrationNumber) {
        this.vehicleRegistrationNumber = vehicleRegistrationNumber;
    }

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Long serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public String getServiceRequestReference() {
        return serviceRequestReference;
    }

    public void setServiceRequestReference(String serviceRequestReference) {
        this.serviceRequestReference = serviceRequestReference;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public BigDecimal getBookingTotalAmount() {
        return bookingTotalAmount;
    }

    public void setBookingTotalAmount(BigDecimal bookingTotalAmount) {
        this.bookingTotalAmount = bookingTotalAmount;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(int serviceCount) {
        this.serviceCount = serviceCount;
    }

    public String getPrimaryServiceName() {
        return primaryServiceName;
    }

    public void setPrimaryServiceName(String primaryServiceName) {
        this.primaryServiceName = primaryServiceName;
    }
}

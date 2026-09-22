package com.carservice.backend.admin.dto;

import com.carservice.backend.booking.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AdminBookingListResponse {

    private Long id;
    private String bookingReference;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private String timeSlot;
    private BookingStatus status;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Long vehicleId;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleRegistrationNumber;
    private String primaryServiceName;
    private int totalServicesCount;
    private BigDecimal totalAmount;
    private Long workshopId;
    private String workshopName;
    private String workshopCity;
    private String serviceRequestReference;
    private Long serviceRequestId;
    private Long currentJobId;
    private String currentJobStatus;
    private LocalDateTime createdAt;

    public AdminBookingListResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
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

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
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

    public String getPrimaryServiceName() {
        return primaryServiceName;
    }

    public void setPrimaryServiceName(String primaryServiceName) {
        this.primaryServiceName = primaryServiceName;
    }

    public int getTotalServicesCount() {
        return totalServicesCount;
    }

    public void setTotalServicesCount(int totalServicesCount) {
        this.totalServicesCount = totalServicesCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public String getServiceRequestReference() {
        return serviceRequestReference;
    }

    public void setServiceRequestReference(String serviceRequestReference) {
        this.serviceRequestReference = serviceRequestReference;
    }

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Long serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public Long getCurrentJobId() {
        return currentJobId;
    }

    public void setCurrentJobId(Long currentJobId) {
        this.currentJobId = currentJobId;
    }

    public String getCurrentJobStatus() {
        return currentJobStatus;
    }

    public void setCurrentJobStatus(String currentJobStatus) {
        this.currentJobStatus = currentJobStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

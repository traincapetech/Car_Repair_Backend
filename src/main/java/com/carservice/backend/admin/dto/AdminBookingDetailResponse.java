package com.carservice.backend.admin.dto;

import com.carservice.backend.booking.dto.BookingResponse.BookingServiceItemResponse;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.vehicle.enums.FuelType;
import com.carservice.backend.vehicle.enums.Transmission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AdminBookingDetailResponse {

    // Core Booking
    private Long id;
    private String bookingReference;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private String timeSlot;
    private BookingStatus status;
    private String customerNotes;
    private BigDecimal estimatedPrice;
    private BigDecimal totalAmount;
    private String city;
    private String address;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;

    // Customer
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDateTime customerCreatedAt;

    // Vehicle
    private Long vehicleId;
    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleRegistrationNumber;
    private FuelType vehicleFuelType;
    private Transmission vehicleTransmission;

    // Services
    private List<BookingServiceItemResponse> services = new ArrayList<>();

    // Linked Service Request
    private Long serviceRequestId;
    private String serviceRequestReference;
    private ServiceRequestStatus serviceRequestStatus;
    private LocalDate preferredDate;
    private String preferredTimeSlot;

    // Assigned Workshop
    private Long assignedWorkshopId;
    private String assignedWorkshopName;
    private String assignedWorkshopPhone;
    private String assignedWorkshopEmail;
    private String assignedWorkshopAddress;
    private String assignedWorkshopCity;
    private String assignedWorkshopState;
    private WorkshopVerificationStatus assignedWorkshopStatus;

    // Linked Workshop Job
    private Long currentJobId;
    private String currentJobReference;
    private WorkshopJobStatus currentJobStatus;

    // Workshop Routing & Matching Opportunities Pipeline
    private List<AdminMarketplaceOpportunityResponse> routingOpportunities = new ArrayList<>();

    public AdminBookingDetailResponse() {
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

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
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

    public LocalDateTime getCustomerCreatedAt() {
        return customerCreatedAt;
    }

    public void setCustomerCreatedAt(LocalDateTime customerCreatedAt) {
        this.customerCreatedAt = customerCreatedAt;
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

    public Integer getVehicleYear() {
        return vehicleYear;
    }

    public void setVehicleYear(Integer vehicleYear) {
        this.vehicleYear = vehicleYear;
    }

    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    public void setVehicleRegistrationNumber(String vehicleRegistrationNumber) {
        this.vehicleRegistrationNumber = vehicleRegistrationNumber;
    }

    public FuelType getVehicleFuelType() {
        return vehicleFuelType;
    }

    public void setVehicleFuelType(FuelType vehicleFuelType) {
        this.vehicleFuelType = vehicleFuelType;
    }

    public Transmission getVehicleTransmission() {
        return vehicleTransmission;
    }

    public void setVehicleTransmission(Transmission vehicleTransmission) {
        this.vehicleTransmission = vehicleTransmission;
    }

    public List<BookingServiceItemResponse> getServices() {
        return services;
    }

    public void setServices(List<BookingServiceItemResponse> services) {
        this.services = services;
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

    public ServiceRequestStatus getServiceRequestStatus() {
        return serviceRequestStatus;
    }

    public void setServiceRequestStatus(ServiceRequestStatus serviceRequestStatus) {
        this.serviceRequestStatus = serviceRequestStatus;
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

    public String getAssignedWorkshopPhone() {
        return assignedWorkshopPhone;
    }

    public void setAssignedWorkshopPhone(String assignedWorkshopPhone) {
        this.assignedWorkshopPhone = assignedWorkshopPhone;
    }

    public String getAssignedWorkshopEmail() {
        return assignedWorkshopEmail;
    }

    public void setAssignedWorkshopEmail(String assignedWorkshopEmail) {
        this.assignedWorkshopEmail = assignedWorkshopEmail;
    }

    public String getAssignedWorkshopAddress() {
        return assignedWorkshopAddress;
    }

    public void setAssignedWorkshopAddress(String assignedWorkshopAddress) {
        this.assignedWorkshopAddress = assignedWorkshopAddress;
    }

    public String getAssignedWorkshopCity() {
        return assignedWorkshopCity;
    }

    public void setAssignedWorkshopCity(String assignedWorkshopCity) {
        this.assignedWorkshopCity = assignedWorkshopCity;
    }

    public String getAssignedWorkshopState() {
        return assignedWorkshopState;
    }

    public void setAssignedWorkshopState(String assignedWorkshopState) {
        this.assignedWorkshopState = assignedWorkshopState;
    }

    public WorkshopVerificationStatus getAssignedWorkshopStatus() {
        return assignedWorkshopStatus;
    }

    public void setAssignedWorkshopStatus(WorkshopVerificationStatus assignedWorkshopStatus) {
        this.assignedWorkshopStatus = assignedWorkshopStatus;
    }

    public Long getCurrentJobId() {
        return currentJobId;
    }

    public void setCurrentJobId(Long currentJobId) {
        this.currentJobId = currentJobId;
    }

    public String getCurrentJobReference() {
        return currentJobReference;
    }

    public void setCurrentJobReference(String currentJobReference) {
        this.currentJobReference = currentJobReference;
    }

    public WorkshopJobStatus getCurrentJobStatus() {
        return currentJobStatus;
    }

    public void setCurrentJobStatus(WorkshopJobStatus currentJobStatus) {
        this.currentJobStatus = currentJobStatus;
    }

    public List<AdminMarketplaceOpportunityResponse> getRoutingOpportunities() {
        return routingOpportunities;
    }

    public void setRoutingOpportunities(List<AdminMarketplaceOpportunityResponse> routingOpportunities) {
        this.routingOpportunities = routingOpportunities;
    }
}

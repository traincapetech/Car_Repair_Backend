package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.dto.ServiceRequestItemResponse;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import com.carservice.backend.vehicle.enums.FuelType;
import com.carservice.backend.vehicle.enums.Transmission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminServiceRequestDetailResponse {

    private Long id;
    private String requestReference;

    // Customer authorized details
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String customerCity;
    private String customerPincode;
    private LocalDateTime customerCreatedAt;

    // Vehicle details
    private Long vehicleId;
    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;
    private String vehicleRegistrationNumber;
    private FuelType vehicleFuelType;
    private Transmission vehicleTransmission;

    // Location & Schedule
    private String city;
    private String address;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDate preferredDate;
    private String preferredTimeSlot;
    private String customerNotes;

    // Financial & Status
    private BigDecimal totalAmount;
    private ServiceRequestStatus status;

    // Assigned Workshop
    private Long assignedWorkshopId;
    private String assignedWorkshopName;
    private String assignedWorkshopPhone;
    private String assignedWorkshopAddress;
    private String assignedWorkshopCity;
    private BigDecimal assignedWorkshopRating;

    // Booking & Job
    private Long bookingId;
    private String bookingReference;
    private Long currentJobId;
    private WorkshopJobStatus currentJobStatus;

    // Line items
    private List<ServiceRequestItemResponse> items = new ArrayList<>();

    // Opportunity metrics
    private long totalOpportunities;
    private long acceptedOpportunities;
    private long transferredOpportunities;
    private long lostOpportunities;
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;
    private BigDecimal totalRefundedAmount = BigDecimal.ZERO;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminServiceRequestDetailResponse() {
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

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void setCustomerCity(String customerCity) {
        this.customerCity = customerCity;
    }

    public String getCustomerPincode() {
        return customerPincode;
    }

    public void setCustomerPincode(String customerPincode) {
        this.customerPincode = customerPincode;
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

    public String getAssignedWorkshopCity() {
        return assignedWorkshopCity;
    }

    public void setAssignedWorkshopCity(String assignedWorkshopCity) {
        this.assignedWorkshopCity = assignedWorkshopCity;
    }

    public BigDecimal getAssignedWorkshopRating() {
        return assignedWorkshopRating;
    }

    public void setAssignedWorkshopRating(BigDecimal assignedWorkshopRating) {
        this.assignedWorkshopRating = assignedWorkshopRating;
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

    public Long getCurrentJobId() {
        return currentJobId;
    }

    public void setCurrentJobId(Long currentJobId) {
        this.currentJobId = currentJobId;
    }

    public WorkshopJobStatus getCurrentJobStatus() {
        return currentJobStatus;
    }

    public void setCurrentJobStatus(WorkshopJobStatus currentJobStatus) {
        this.currentJobStatus = currentJobStatus;
    }

    public List<ServiceRequestItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ServiceRequestItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public long getTotalOpportunities() {
        return totalOpportunities;
    }

    public void setTotalOpportunities(long totalOpportunities) {
        this.totalOpportunities = totalOpportunities;
    }

    public long getAcceptedOpportunities() {
        return acceptedOpportunities;
    }

    public void setAcceptedOpportunities(long acceptedOpportunities) {
        this.acceptedOpportunities = acceptedOpportunities;
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

    public BigDecimal getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(BigDecimal totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }

    public BigDecimal getTotalRefundedAmount() {
        return totalRefundedAmount;
    }

    public void setTotalRefundedAmount(BigDecimal totalRefundedAmount) {
        this.totalRefundedAmount = totalRefundedAmount;
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

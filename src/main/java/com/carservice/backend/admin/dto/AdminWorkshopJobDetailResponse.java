package com.carservice.backend.admin.dto;

import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.marketplace.dto.ServiceRequestItemResponse;
import com.carservice.backend.marketplace.entity.MarketplaceAuditEvent;
import com.carservice.backend.marketplace.enums.OpportunityStatus;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.vehicle.enums.FuelType;
import com.carservice.backend.vehicle.enums.Transmission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminWorkshopJobDetailResponse {

    // Core Job
    private Long id;
    private String jobReference;
    private WorkshopJobStatus status;
    private String notes;
    private String cancellationReason;

    // Lifecycle Timestamps
    private LocalDateTime assignedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime vehicleReceivedAt;
    private LocalDateTime inspectionStartedAt;
    private LocalDateTime workStartedAt;
    private LocalDateTime readyForDeliveryAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime transferredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Workshop
    private Long workshopId;
    private String workshopName;
    private String workshopPhone;
    private String workshopEmail;
    private String workshopAddress;
    private String workshopCity;
    private String workshopState;
    private WorkshopVerificationStatus workshopStatus;

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

    // Linked Service Request & Booking
    private Long serviceRequestId;
    private String serviceRequestReference;
    private ServiceRequestStatus serviceRequestStatus;
    private Long bookingId;
    private String bookingReference;
    private BookingStatus bookingStatus;
    private BigDecimal bookingTotalAmount;
    private LocalDate bookingDate;
    private String timeSlot;

    // Opportunity
    private Long opportunityId;
    private BigDecimal leadFee;
    private OpportunityStatus opportunityStatus;

    // Services Requested
    private List<ServiceRequestItemResponse> services = new ArrayList<>();

    // Audit Trail
    private List<MarketplaceAuditEvent> auditEvents = new ArrayList<>();

    public AdminWorkshopJobDetailResponse() {
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getVehicleReceivedAt() {
        return vehicleReceivedAt;
    }

    public void setVehicleReceivedAt(LocalDateTime vehicleReceivedAt) {
        this.vehicleReceivedAt = vehicleReceivedAt;
    }

    public LocalDateTime getInspectionStartedAt() {
        return inspectionStartedAt;
    }

    public void setInspectionStartedAt(LocalDateTime inspectionStartedAt) {
        this.inspectionStartedAt = inspectionStartedAt;
    }

    public LocalDateTime getWorkStartedAt() {
        return workStartedAt;
    }

    public void setWorkStartedAt(LocalDateTime workStartedAt) {
        this.workStartedAt = workStartedAt;
    }

    public LocalDateTime getReadyForDeliveryAt() {
        return readyForDeliveryAt;
    }

    public void setReadyForDeliveryAt(LocalDateTime readyForDeliveryAt) {
        this.readyForDeliveryAt = readyForDeliveryAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getTransferredAt() {
        return transferredAt;
    }

    public void setTransferredAt(LocalDateTime transferredAt) {
        this.transferredAt = transferredAt;
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

    public String getWorkshopPhone() {
        return workshopPhone;
    }

    public void setWorkshopPhone(String workshopPhone) {
        this.workshopPhone = workshopPhone;
    }

    public String getWorkshopEmail() {
        return workshopEmail;
    }

    public void setWorkshopEmail(String workshopEmail) {
        this.workshopEmail = workshopEmail;
    }

    public String getWorkshopAddress() {
        return workshopAddress;
    }

    public void setWorkshopAddress(String workshopAddress) {
        this.workshopAddress = workshopAddress;
    }

    public String getWorkshopCity() {
        return workshopCity;
    }

    public void setWorkshopCity(String workshopCity) {
        this.workshopCity = workshopCity;
    }

    public String getWorkshopState() {
        return workshopState;
    }

    public void setWorkshopState(String workshopState) {
        this.workshopState = workshopState;
    }

    public WorkshopVerificationStatus getWorkshopStatus() {
        return workshopStatus;
    }

    public void setWorkshopStatus(WorkshopVerificationStatus workshopStatus) {
        this.workshopStatus = workshopStatus;
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

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public BigDecimal getBookingTotalAmount() {
        return bookingTotalAmount;
    }

    public void setBookingTotalAmount(BigDecimal bookingTotalAmount) {
        this.bookingTotalAmount = bookingTotalAmount;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Long opportunityId) {
        this.opportunityId = opportunityId;
    }

    public BigDecimal getLeadFee() {
        return leadFee;
    }

    public void setLeadFee(BigDecimal leadFee) {
        this.leadFee = leadFee;
    }

    public OpportunityStatus getOpportunityStatus() {
        return opportunityStatus;
    }

    public void setOpportunityStatus(OpportunityStatus opportunityStatus) {
        this.opportunityStatus = opportunityStatus;
    }

    public List<ServiceRequestItemResponse> getServices() {
        return services;
    }

    public void setServices(List<ServiceRequestItemResponse> services) {
        this.services = services;
    }

    public List<MarketplaceAuditEvent> getAuditEvents() {
        return auditEvents;
    }

    public void setAuditEvents(List<MarketplaceAuditEvent> auditEvents) {
        this.auditEvents = auditEvents;
    }
}

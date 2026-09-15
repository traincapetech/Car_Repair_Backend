package com.carservice.backend.marketplace.dto;

import com.carservice.backend.marketplace.enums.WorkshopJobStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkshopJobResponse {

    private Long id;
    private String jobReference;
    private Long serviceRequestId;
    private String serviceRequestReference;
    private String bookingReference;
    private Long workshopId;
    private String workshopName;
    private WorkshopJobStatus status;
    private String notes;

    private Long vehicleId;
    private String vehicleSummary;

    private CustomerUnlockedInfo customer;
    private List<ServiceRequestItemResponse> requestedServices = new ArrayList<>();
    private BigDecimal totalAmount;

    private LocalDate preferredDate;
    private String preferredTimeSlot;
    private String customerNotes;

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

    private List<WorkshopJobStatus> allowedNextStatuses = new ArrayList<>();
    private boolean canTransfer;

    public WorkshopJobResponse() {
    }

    public static class CustomerUnlockedInfo {
        private String name;
        private String phone;
        private String email;
        private String address;
        private String city;
        private String pincode;

        public CustomerUnlockedInfo() {
        }

        public CustomerUnlockedInfo(String name, String phone, String email, String address, String city, String pincode) {
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.address = address;
            this.city = city;
            this.pincode = pincode;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
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

        public String getPincode() {
            return pincode;
        }

        public void setPincode(String pincode) {
            this.pincode = pincode;
        }
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

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
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

    public CustomerUnlockedInfo getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerUnlockedInfo customer) {
        this.customer = customer;
    }

    public List<ServiceRequestItemResponse> getRequestedServices() {
        return requestedServices;
    }

    public void setRequestedServices(List<ServiceRequestItemResponse> requestedServices) {
        this.requestedServices = requestedServices;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public List<WorkshopJobStatus> getAllowedNextStatuses() {
        return allowedNextStatuses;
    }

    public void setAllowedNextStatuses(List<WorkshopJobStatus> allowedNextStatuses) {
        this.allowedNextStatuses = allowedNextStatuses;
    }

    public boolean isCanTransfer() {
        return canTransfer;
    }

    public void setCanTransfer(boolean canTransfer) {
        this.canTransfer = canTransfer;
    }
}

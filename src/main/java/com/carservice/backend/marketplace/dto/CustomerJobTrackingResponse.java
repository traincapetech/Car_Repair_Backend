package com.carservice.backend.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CustomerJobTrackingResponse {

    private String bookingReference;
    private Long bookingId;
    private String requestReference;
    private Long serviceRequestId;

    private String status;
    private String jobStatus;
    private String friendlyStatusTitle;
    private String friendlyStatusDescription;
    private int currentStep;
    private int totalSteps = 7;

    private Long vehicleId;
    private String vehicleSummary;
    private List<ServiceRequestItemResponse> services = new ArrayList<>();
    private BigDecimal totalAmount;

    private LocalDate appointmentDate;
    private String appointmentTimeSlot;
    private String customerNotes;

    private AssignedWorkshopSummary workshop;
    private boolean cancellable;
    private List<TrackingTimelineItem> timeline = new ArrayList<>();

    public CustomerJobTrackingResponse() {
    }

    public static class AssignedWorkshopSummary {
        private Long id;
        private String name;
        private String address;
        private String city;
        private String phone;

        public AssignedWorkshopSummary() {
        }

        public AssignedWorkshopSummary(Long id, String name, String address, String city, String phone) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.city = city;
            this.phone = phone;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    public static class TrackingTimelineItem {
        private String stage;
        private String label;
        private String description;
        private LocalDateTime timestamp;
        private boolean completed;
        private boolean current;

        public TrackingTimelineItem() {
        }

        public TrackingTimelineItem(String stage, String label, String description, LocalDateTime timestamp, boolean completed, boolean current) {
            this.stage = stage;
            this.label = label;
            this.description = description;
            this.timestamp = timestamp;
            this.completed = completed;
            this.current = current;
        }

        public String getStage() {
            return stage;
        }

        public void setStage(String stage) {
            this.stage = stage;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public boolean isCurrent() {
            return current;
        }

        public void setCurrent(boolean current) {
            this.current = current;
        }
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

    public String getRequestReference() {
        return requestReference;
    }

    public void setRequestReference(String requestReference) {
        this.requestReference = requestReference;
    }

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Long serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getFriendlyStatusTitle() {
        return friendlyStatusTitle;
    }

    public void setFriendlyStatusTitle(String friendlyStatusTitle) {
        this.friendlyStatusTitle = friendlyStatusTitle;
    }

    public String getFriendlyStatusDescription() {
        return friendlyStatusDescription;
    }

    public void setFriendlyStatusDescription(String friendlyStatusDescription) {
        this.friendlyStatusDescription = friendlyStatusDescription;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public int getStageNumber() {
        return currentStep;
    }

    public String getStageTitle() {
        return friendlyStatusTitle;
    }

    public String getAssignedWorkshopName() {
        return workshop != null ? workshop.getName() : null;
    }

    public String getAssignedWorkshopPhone() {
        return workshop != null ? workshop.getPhone() : null;
    }

    public String getAssignedWorkshopAddress() {
        if (workshop == null) return null;
        return workshop.getAddress() != null ? workshop.getAddress() + (workshop.getCity() != null ? ", " + workshop.getCity() : "") : workshop.getCity();
    }

    public boolean getIsCancellable() {
        return cancellable;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
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

    public List<ServiceRequestItemResponse> getServices() {
        return services;
    }

    public void setServices(List<ServiceRequestItemResponse> services) {
        this.services = services;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTimeSlot() {
        return appointmentTimeSlot;
    }

    public void setAppointmentTimeSlot(String appointmentTimeSlot) {
        this.appointmentTimeSlot = appointmentTimeSlot;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public AssignedWorkshopSummary getWorkshop() {
        return workshop;
    }

    public void setWorkshop(AssignedWorkshopSummary workshop) {
        this.workshop = workshop;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    public void setCancellable(boolean cancellable) {
        this.cancellable = cancellable;
    }

    public List<TrackingTimelineItem> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<TrackingTimelineItem> timeline) {
        this.timeline = timeline;
    }
}

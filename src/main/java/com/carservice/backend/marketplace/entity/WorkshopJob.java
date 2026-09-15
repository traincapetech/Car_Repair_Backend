package com.carservice.backend.marketplace.entity;

import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(
        name = "workshop_jobs",
        indexes = {
                @Index(name = "idx_wj_reference", columnList = "job_reference"),
                @Index(name = "idx_wj_workshop_id", columnList = "workshop_id"),
                @Index(name = "idx_wj_service_request_id", columnList = "service_request_id"),
                @Index(name = "idx_wj_status", columnList = "status"),
                @Index(name = "idx_wj_workshop_status", columnList = "workshop_id, status")
        }
)
public class WorkshopJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_reference", length = 32, unique = true, nullable = false)
    private String jobReference;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_request_id", nullable = false, unique = true)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false, unique = true)
    private LeadOpportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkshopJobStatus status = WorkshopJobStatus.ASSIGNED;

    @Column(length = 1000)
    private String notes;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "vehicle_received_at")
    private LocalDateTime vehicleReceivedAt;

    @Column(name = "inspection_started_at")
    private LocalDateTime inspectionStartedAt;

    @Column(name = "work_started_at")
    private LocalDateTime workStartedAt;

    @Column(name = "ready_for_delivery_at")
    private LocalDateTime readyForDeliveryAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WorkshopJob() {
    }

    public WorkshopJob(ServiceRequest serviceRequest, Workshop workshop, LeadOpportunity opportunity) {
        this.serviceRequest = serviceRequest;
        this.workshop = workshop;
        this.opportunity = opportunity;
        this.status = WorkshopJobStatus.ASSIGNED;
        LocalDateTime now = LocalDateTime.now();
        this.assignedAt = now;
        this.jobReference = generateJobReference(LocalDate.now());
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.assignedAt == null) {
            this.assignedAt = now;
        }
        if (this.jobReference == null || this.jobReference.isBlank()) {
            this.jobReference = generateJobReference(LocalDate.now());
        }
        if (this.status == null) {
            this.status = WorkshopJobStatus.ASSIGNED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static String generateJobReference(LocalDate date) {
        String dateStr = (date != null ? date : LocalDate.now()).format(DateTimeFormatter.BASIC_ISO_DATE);
        int randomNum = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "JOB-" + dateStr + "-" + randomNum;
    }

    /**
     * Strict state machine verification for job execution transitions.
     */
    public boolean canTransitionTo(WorkshopJobStatus target) {
        if (target == null || target == this.status) {
            return false;
        }

        switch (this.status) {
            case ASSIGNED:
                return target == WorkshopJobStatus.CONFIRMED
                        || target == WorkshopJobStatus.TRANSFERRED
                        || target == WorkshopJobStatus.CANCELLED;

            case CONFIRMED:
                return target == WorkshopJobStatus.VEHICLE_RECEIVED
                        || target == WorkshopJobStatus.TRANSFERRED
                        || target == WorkshopJobStatus.CANCELLED;

            case VEHICLE_RECEIVED:
                return target == WorkshopJobStatus.INSPECTION
                        || target == WorkshopJobStatus.WORK_IN_PROGRESS;

            case INSPECTION:
                return target == WorkshopJobStatus.WORK_IN_PROGRESS;

            case WORK_IN_PROGRESS:
                return target == WorkshopJobStatus.READY_FOR_DELIVERY;

            case READY_FOR_DELIVERY:
                return target == WorkshopJobStatus.COMPLETED;

            case COMPLETED:
            case CANCELLED:
            case TRANSFERRED:
            default:
                // Terminal states cannot transition further
                return false;
        }
    }

    /**
     * Checks if the job has physically started (vehicle received or beyond).
     * When true, customer cancellation is strictly disallowed.
     */
    public boolean isPhysicalWorkStarted() {
        return this.status == WorkshopJobStatus.VEHICLE_RECEIVED
                || this.status == WorkshopJobStatus.INSPECTION
                || this.status == WorkshopJobStatus.WORK_IN_PROGRESS
                || this.status == WorkshopJobStatus.READY_FOR_DELIVERY
                || this.status == WorkshopJobStatus.COMPLETED;
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

    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public LeadOpportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(LeadOpportunity opportunity) {
        this.opportunity = opportunity;
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

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

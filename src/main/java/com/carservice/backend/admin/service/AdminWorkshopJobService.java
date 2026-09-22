package com.carservice.backend.admin.service;

import com.carservice.backend.admin.dto.AdminUpdateJobStatusRequest;
import com.carservice.backend.admin.dto.AdminWorkshopJobDetailResponse;
import com.carservice.backend.admin.dto.AdminWorkshopJobListResponse;
import com.carservice.backend.admin.dto.AdminWorkshopJobSummaryResponse;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.common.notification.NotificationService;
import com.carservice.backend.common.notification.enums.NotificationType;
import com.carservice.backend.marketplace.dto.ServiceRequestItemResponse;
import com.carservice.backend.marketplace.entity.LeadOpportunity;
import com.carservice.backend.marketplace.entity.MarketplaceAuditEvent;
import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.entity.WorkshopJob;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import com.carservice.backend.marketplace.repository.MarketplaceAuditEventRepository;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.marketplace.repository.WorkshopJobRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.vehicle.entity.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminWorkshopJobService {

    private static final Logger log = LoggerFactory.getLogger(AdminWorkshopJobService.class);

    private final WorkshopJobRepository workshopJobRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final BookingRepository bookingRepository;
    private final MarketplaceAuditEventRepository auditEventRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AdminWorkshopJobService(
            WorkshopJobRepository workshopJobRepository,
            ServiceRequestRepository serviceRequestRepository,
            BookingRepository bookingRepository,
            MarketplaceAuditEventRepository auditEventRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.workshopJobRepository = workshopJobRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.bookingRepository = bookingRepository;
        this.auditEventRepository = auditEventRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    /**
     * Real-time operational KPI metrics for workshop jobs calculated from database records.
     */
    public AdminWorkshopJobSummaryResponse getJobSummary() {
        long total = workshopJobRepository.count();
        long assigned = workshopJobRepository.countByStatus(WorkshopJobStatus.ASSIGNED);
        long confirmed = workshopJobRepository.countByStatus(WorkshopJobStatus.CONFIRMED);
        long inProgress = workshopJobRepository.countByStatusIn(List.of(
                WorkshopJobStatus.VEHICLE_RECEIVED,
                WorkshopJobStatus.INSPECTION,
                WorkshopJobStatus.WORK_IN_PROGRESS,
                WorkshopJobStatus.READY_FOR_DELIVERY
        ));
        long completed = workshopJobRepository.countByStatus(WorkshopJobStatus.COMPLETED);
        long cancelled = workshopJobRepository.countByStatus(WorkshopJobStatus.CANCELLED);
        long transferred = workshopJobRepository.countByStatus(WorkshopJobStatus.TRANSFERRED);

        return new AdminWorkshopJobSummaryResponse(
                total,
                assigned,
                confirmed,
                inProgress,
                completed,
                cancelled,
                transferred
        );
    }

    /**
     * Paginated, searchable, and filtered list of operational workshop jobs.
     */
    public Page<AdminWorkshopJobListResponse> getWorkshopJobs(
            String search,
            WorkshopJobStatus status,
            Long workshopId,
            Long customerId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<WorkshopJob> page = workshopJobRepository.findWorkshopJobsWithFilter(
                search,
                status,
                workshopId,
                customerId,
                startDate,
                endDate,
                pageable
        );

        return page.map(this::mapToListResponse);
    }

    /**
     * Complete 360° operational dossier for a workshop job.
     */
    public AdminWorkshopJobDetailResponse getJobDetail(Long id) {
        WorkshopJob job = workshopJobRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop job not found with id: " + id));

        AdminWorkshopJobDetailResponse response = new AdminWorkshopJobDetailResponse();
        response.setId(job.getId());
        response.setJobReference(job.getJobReference());
        response.setStatus(job.getStatus());
        response.setNotes(job.getNotes());
        response.setCancellationReason(job.getCancellationReason());

        // Lifecycle Timestamps
        response.setAssignedAt(job.getAssignedAt());
        response.setConfirmedAt(job.getConfirmedAt());
        response.setVehicleReceivedAt(job.getVehicleReceivedAt());
        response.setInspectionStartedAt(job.getInspectionStartedAt());
        response.setWorkStartedAt(job.getWorkStartedAt());
        response.setReadyForDeliveryAt(job.getReadyForDeliveryAt());
        response.setCompletedAt(job.getCompletedAt());
        response.setCancelledAt(job.getCancelledAt());
        response.setTransferredAt(job.getTransferredAt());
        response.setCreatedAt(job.getCreatedAt());

        // Workshop Dossier
        if (job.getWorkshop() != null) {
            Workshop w = job.getWorkshop();
            response.setWorkshopId(w.getId());
            response.setWorkshopName(w.getBusinessName());
            response.setWorkshopPhone(w.getPhone());
            response.setWorkshopEmail(w.getEmail());
            response.setWorkshopAddress(w.getAddress());
            response.setWorkshopCity(w.getCity());
            response.setWorkshopState(w.getState());
            response.setWorkshopStatus(w.getVerificationStatus());
        }

        // Service Request, Customer, Vehicle, and Booking Dossier
        ServiceRequest sr = job.getServiceRequest();
        if (sr != null) {
            response.setServiceRequestId(sr.getId());
            response.setServiceRequestReference(sr.getRequestReference());
            response.setServiceRequestStatus(sr.getStatus());

            if (sr.getUser() != null) {
                User u = sr.getUser();
                response.setCustomerId(u.getId());
                response.setCustomerName(u.getName());
                response.setCustomerEmail(u.getEmail());
                response.setCustomerPhone(u.getPhone());
                response.setCustomerCreatedAt(u.getCreatedAt());
            }

            if (sr.getVehicle() != null) {
                Vehicle v = sr.getVehicle();
                response.setVehicleId(v.getId());
                response.setVehicleMake(v.getMake());
                response.setVehicleModel(v.getModel());
                response.setVehicleYear(v.getYear());
                response.setVehicleRegistrationNumber(v.getRegistrationNumber());
                response.setVehicleFuelType(v.getFuelType());
                response.setVehicleTransmission(v.getTransmission());
            }

            Booking b = sr.getBooking();
            if (b != null) {
                response.setBookingId(b.getId());
                response.setBookingReference(b.getBookingReference());
                response.setBookingStatus(b.getStatus());
                response.setBookingTotalAmount(b.getTotalAmount());
                response.setBookingDate(b.getBookingDate());
                response.setTimeSlot(b.getTimeSlot());
            }

            // Services line items
            if (sr.getItems() != null) {
                List<ServiceRequestItemResponse> items = sr.getItems().stream()
                        .map(item -> new ServiceRequestItemResponse(
                                item.getId(),
                                item.getServiceCatalog() != null ? item.getServiceCatalog().getId() : null,
                                item.getServiceNameSnapshot(),
                                item.getBasePriceSnapshot(),
                                item.getDiscountTypeSnapshot(),
                                item.getDiscountValueSnapshot(),
                                item.getFinalPriceSnapshot()
                        ))
                        .collect(Collectors.toList());
                response.setServices(items);
            }

            // Audit Trail
            List<MarketplaceAuditEvent> events = auditEventRepository.findByServiceRequestIdOrderByCreatedAtDesc(sr.getId());
            response.setAuditEvents(events);
        }

        // Opportunity
        if (job.getOpportunity() != null) {
            LeadOpportunity opp = job.getOpportunity();
            response.setOpportunityId(opp.getId());
            response.setLeadFee(opp.getFeeSnapshot());
            response.setOpportunityStatus(opp.getStatus());
        }

        return response;
    }

    /**
     * Administrative status update with audit trail and notification dispatch.
     */
    @Transactional
    public AdminWorkshopJobDetailResponse updateJobStatus(User adminUser, Long id, AdminUpdateJobStatusRequest request) {
        WorkshopJob job = workshopJobRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop job not found with id: " + id));

        WorkshopJobStatus previousStatus = job.getStatus();
        WorkshopJobStatus newStatus = request.getStatus();

        if (previousStatus == newStatus) {
            return getJobDetail(job.getId());
        }

        LocalDateTime now = LocalDateTime.now();
        job.setStatus(newStatus);

        if (request.getNotes() != null) {
            job.setNotes(request.getNotes().trim());
        }

        switch (newStatus) {
            case CONFIRMED -> job.setConfirmedAt(now);
            case VEHICLE_RECEIVED -> job.setVehicleReceivedAt(now);
            case INSPECTION -> job.setInspectionStartedAt(now);
            case WORK_IN_PROGRESS -> job.setWorkStartedAt(now);
            case READY_FOR_DELIVERY -> job.setReadyForDeliveryAt(now);
            case COMPLETED -> job.setCompletedAt(now);
            case CANCELLED -> {
                job.setCancelledAt(now);
                if (request.getReason() != null) {
                    job.setCancellationReason(request.getReason().trim());
                }
            }
            case TRANSFERRED -> job.setTransferredAt(now);
            case ASSIGNED -> job.setAssignedAt(now);
        }

        WorkshopJob savedJob = workshopJobRepository.save(job);

        // Synchronize linked ServiceRequest and Booking states
        ServiceRequest sr = savedJob.getServiceRequest();
        if (sr != null) {
            if (newStatus == WorkshopJobStatus.COMPLETED) {
                sr.setStatus(ServiceRequestStatus.COMPLETED);
            } else if (newStatus == WorkshopJobStatus.CANCELLED) {
                sr.setStatus(ServiceRequestStatus.CANCELLED);
            } else if (newStatus == WorkshopJobStatus.WORK_IN_PROGRESS
                    || newStatus == WorkshopJobStatus.INSPECTION
                    || newStatus == WorkshopJobStatus.VEHICLE_RECEIVED
                    || newStatus == WorkshopJobStatus.READY_FOR_DELIVERY) {
                sr.setStatus(ServiceRequestStatus.IN_PROGRESS);
            }
            serviceRequestRepository.save(sr);

            Booking b = sr.getBooking();
            if (b != null) {
                if (newStatus == WorkshopJobStatus.COMPLETED) {
                    b.setStatus(BookingStatus.COMPLETED);
                } else if (newStatus == WorkshopJobStatus.CANCELLED) {
                    b.setStatus(BookingStatus.CANCELLED);
                    b.setCancelledAt(now);
                } else if (newStatus == WorkshopJobStatus.WORK_IN_PROGRESS
                        || newStatus == WorkshopJobStatus.INSPECTION
                        || newStatus == WorkshopJobStatus.VEHICLE_RECEIVED
                        || newStatus == WorkshopJobStatus.READY_FOR_DELIVERY) {
                    b.setStatus(BookingStatus.IN_PROGRESS);
                }
                bookingRepository.save(b);
            }
        }

        // Immutable Audit Log
        auditService.record(
                adminUser,
                "WORKSHOP_JOB_STATUS_CHANGED",
                "WORKSHOP_JOB",
                savedJob.getId().toString(),
                "Admin updated job #" + savedJob.getJobReference() + " status from " + previousStatus + " to " + newStatus,
                "SUCCESS",
                Map.of("status", previousStatus.name()),
                Map.of("status", newStatus.name(), "notes", request.getNotes() != null ? request.getNotes() : ""),
                Map.of("jobReference", savedJob.getJobReference())
        );

        // Realtime + in-app notification dispatch
        if (sr != null && sr.getUser() != null) {
            try {
                NotificationType notifType = switch (newStatus) {
                    case COMPLETED -> NotificationType.BOOKING_COMPLETED;
                    case CANCELLED -> NotificationType.BOOKING_CANCELLED;
                    default -> NotificationType.SYSTEM_ALERT;
                };

                notificationService.sendNotification(
                        sr.getUser().getId(),
                        "CUSTOMER",
                        notifType,
                        "Job Status Updated: " + newStatus,
                        "Your vehicle service job #" + savedJob.getJobReference() + " status has been updated to " + newStatus,
                        "WORKSHOP_JOB",
                        savedJob.getId(),
                        Map.of("jobReference", savedJob.getJobReference(), "status", newStatus.name())
                );
            } catch (Exception e) {
                log.warn("Notice: Failed to dispatch customer notification for job status update #{}: {}", savedJob.getId(), e.getMessage());
            }
        }

        return getJobDetail(savedJob.getId());
    }

    private AdminWorkshopJobListResponse mapToListResponse(WorkshopJob job) {
        AdminWorkshopJobListResponse dto = new AdminWorkshopJobListResponse();
        dto.setId(job.getId());
        dto.setJobReference(job.getJobReference());
        dto.setStatus(job.getStatus());
        dto.setAssignedAt(job.getAssignedAt());

        if (job.getWorkshop() != null) {
            Workshop w = job.getWorkshop();
            dto.setWorkshopId(w.getId());
            dto.setWorkshopName(w.getBusinessName());
            dto.setWorkshopCity(w.getCity());
        }

        ServiceRequest sr = job.getServiceRequest();
        if (sr != null) {
            dto.setServiceRequestId(sr.getId());
            dto.setServiceRequestReference(sr.getRequestReference());

            if (sr.getUser() != null) {
                dto.setCustomerId(sr.getUser().getId());
                dto.setCustomerName(sr.getUser().getName());
                dto.setCustomerPhone(sr.getUser().getPhone());
            }

            if (sr.getVehicle() != null) {
                dto.setVehicleId(sr.getVehicle().getId());
                dto.setVehicleMake(sr.getVehicle().getMake());
                dto.setVehicleModel(sr.getVehicle().getModel());
                dto.setVehicleRegistrationNumber(sr.getVehicle().getRegistrationNumber());
            }

            Booking b = sr.getBooking();
            if (b != null) {
                dto.setBookingId(b.getId());
                dto.setBookingReference(b.getBookingReference());
                dto.setBookingTotalAmount(b.getTotalAmount());
                dto.setPrimaryServiceName(b.getServiceNameSnapshot());
            }

            if (sr.getItems() != null) {
                dto.setServiceCount(sr.getItems().size());
            }
        }

        return dto;
    }
}

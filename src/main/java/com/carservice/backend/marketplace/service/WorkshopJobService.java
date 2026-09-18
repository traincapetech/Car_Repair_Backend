package com.carservice.backend.marketplace.service;

import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.common.notification.NotificationService;
import com.carservice.backend.marketplace.dto.CustomerJobTrackingResponse;
import com.carservice.backend.marketplace.dto.ServiceRequestItemResponse;
import com.carservice.backend.marketplace.dto.UpdateJobStatusRequest;
import com.carservice.backend.marketplace.dto.WorkshopJobResponse;
import com.carservice.backend.marketplace.entity.LeadOpportunity;
import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.entity.WorkshopJob;
import com.carservice.backend.marketplace.enums.MarketplaceEventType;
import com.carservice.backend.marketplace.enums.OpportunityStatus;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import com.carservice.backend.marketplace.repository.LeadOpportunityRepository;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.marketplace.repository.WorkshopJobRepository;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.vehicle.entity.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkshopJobService {

    private static final Logger log = LoggerFactory.getLogger(WorkshopJobService.class);

    private final WorkshopJobRepository workshopJobRepository;
    private final WorkshopRepository workshopRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final BookingRepository bookingRepository;
    private final LeadOpportunityRepository leadOpportunityRepository;
    private final MarketplaceAuditService auditService;
    private final NotificationService notificationService;

    public WorkshopJobService(
            WorkshopJobRepository workshopJobRepository,
            WorkshopRepository workshopRepository,
            ServiceRequestRepository serviceRequestRepository,
            BookingRepository bookingRepository,
            LeadOpportunityRepository leadOpportunityRepository,
            MarketplaceAuditService auditService,
            NotificationService notificationService
    ) {
        this.workshopJobRepository = workshopJobRepository;
        this.workshopRepository = workshopRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.bookingRepository = bookingRepository;
        this.leadOpportunityRepository = leadOpportunityRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    private Workshop resolveWorkshop(User currentUser) {
        return workshopRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No workshop partner account found for user: " + currentUser.getEmail()));
    }

    @Transactional
    public WorkshopJob createJobForAssignedOpportunity(ServiceRequest serviceRequest, Workshop workshop, LeadOpportunity opportunity) {
        Optional<WorkshopJob> existing = workshopJobRepository.findByServiceRequestId(serviceRequest.getId());
        if (existing.isPresent()) {
            WorkshopJob current = existing.get();
            if (current.getStatus() == WorkshopJobStatus.TRANSFERRED || current.getStatus() == WorkshopJobStatus.CANCELLED) {
                log.info("Reassigning workshop job #{} to workshop '{}' after previous status was {}",
                        current.getId(), workshop.getBusinessName(), current.getStatus());
                current.setWorkshop(workshop);
                current.setOpportunity(opportunity);
                current.setStatus(WorkshopJobStatus.ASSIGNED);
                current.setAssignedAt(LocalDateTime.now());
                current.setVehicleReceivedAt(null);
                current.setInspectionStartedAt(null);
                current.setWorkStartedAt(null);
                current.setReadyForDeliveryAt(null);
                current.setCompletedAt(null);
                current.setCancelledAt(null);
                current.setTransferredAt(null);
                current.setCancellationReason(null);
                WorkshopJob savedJob = workshopJobRepository.save(current);
                serviceRequest.setCurrentJob(savedJob);
                serviceRequestRepository.save(serviceRequest);

                auditService.recordEvent(
                        MarketplaceEventType.JOB_STATUS_CHANGED,
                        serviceRequest.getId(),
                        opportunity.getId(),
                        workshop.getId(),
                        null,
                        "Workshop job #" + savedJob.getJobReference() + " reassigned in status ASSIGNED to " + workshop.getBusinessName(),
                        "{\"jobReference\": \"" + savedJob.getJobReference() + "\", \"status\": \"ASSIGNED\"}"
                );

                notificationService.notifyCustomer(
                        serviceRequest.getUser().getId(),
                        "WORKSHOP_ASSIGNED",
                        "Service Centre Assigned",
                        workshop.getBusinessName() + " has been assigned to your service request " + serviceRequest.getRequestReference(),
                        Map.of("workshopName", workshop.getBusinessName(), "requestReference", serviceRequest.getRequestReference())
                );

                return savedJob;
            } else {
                return current;
            }
        }

        WorkshopJob job = new WorkshopJob(serviceRequest, workshop, opportunity);
        WorkshopJob savedJob = workshopJobRepository.save(job);
        serviceRequest.setCurrentJob(savedJob);
        serviceRequestRepository.save(serviceRequest);

        auditService.recordEvent(
                MarketplaceEventType.JOB_STATUS_CHANGED,
                serviceRequest.getId(),
                opportunity.getId(),
                workshop.getId(),
                null,
                "Workshop job #" + savedJob.getJobReference() + " initialized in status ASSIGNED for " + workshop.getBusinessName(),
                "{\"jobReference\": \"" + savedJob.getJobReference() + "\", \"status\": \"ASSIGNED\"}"
        );

        notificationService.notifyCustomer(
                serviceRequest.getUser().getId(),
                "WORKSHOP_ASSIGNED",
                "Service Centre Assigned",
                workshop.getBusinessName() + " has been assigned to your service request " + serviceRequest.getRequestReference(),
                Map.of("workshopName", workshop.getBusinessName(), "requestReference", serviceRequest.getRequestReference())
        );

        return savedJob;
    }

    @Transactional(readOnly = true)
    public List<WorkshopJobResponse> getWorkshopJobs(User currentUser, String statusFilter) {
        Workshop workshop = resolveWorkshop(currentUser);
        List<WorkshopJob> jobs;

        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
            try {
                WorkshopJobStatus status = WorkshopJobStatus.valueOf(statusFilter.toUpperCase());
                jobs = workshopJobRepository.findByWorkshopIdAndStatusOrderByCreatedAtDesc(workshop.getId(), status);
            } catch (IllegalArgumentException e) {
                jobs = workshopJobRepository.findByWorkshopIdOrderByCreatedAtDesc(workshop.getId());
            }
        } else {
            jobs = workshopJobRepository.findByWorkshopIdOrderByCreatedAtDesc(workshop.getId());
        }

        return jobs.stream().map(this::mapToJobResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkshopJobResponse getJobDetails(User currentUser, Long jobId) {
        WorkshopJob job = workshopJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop job not found with id: " + jobId));

        if (currentUser.getRole() != UserRole.ADMIN) {
            Workshop workshop = resolveWorkshop(currentUser);
            if (!job.getWorkshop().getId().equals(workshop.getId())) {
                throw new IllegalArgumentException("You are not authorized to view this workshop job");
            }
        }

        return mapToJobResponse(job);
    }

    @Transactional
    public WorkshopJobResponse updateJobStatus(User currentUser, Long jobId, UpdateJobStatusRequest request) {
        WorkshopJob job = workshopJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop job not found with id: " + jobId));

        Workshop workshop = null;
        if (currentUser.getRole() != UserRole.ADMIN) {
            workshop = resolveWorkshop(currentUser);
            if (!job.getWorkshop().getId().equals(workshop.getId())) {
                throw new IllegalArgumentException("You are not authorized to update this workshop job");
            }
        } else {
            workshop = job.getWorkshop();
        }

        WorkshopJobStatus newStatus = request.getStatus();

        // 1. Strict State Transition Validation
        if (!job.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Invalid status transition from " + job.getStatus() + " to " + newStatus);
        }

        WorkshopJobStatus oldStatus = job.getStatus();
        job.setStatus(newStatus);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            job.setNotes(request.getNotes().trim());
        }

        LocalDateTime now = LocalDateTime.now();
        ServiceRequest sr = job.getServiceRequest();
        Booking booking = sr.getBooking();
        LeadOpportunity opp = job.getOpportunity();

        // 2. Stage-Specific Timestamps & Cascade Updates
        switch (newStatus) {
            case CONFIRMED:
                job.setConfirmedAt(now);
                if (booking != null && booking.getStatus() == BookingStatus.PENDING) {
                    booking.setStatus(BookingStatus.CONFIRMED);
                    bookingRepository.save(booking);
                }
                break;

            case VEHICLE_RECEIVED:
                job.setVehicleReceivedAt(now);
                sr.setStatus(ServiceRequestStatus.IN_PROGRESS);
                serviceRequestRepository.save(sr);
                if (booking != null) {
                    booking.setStatus(BookingStatus.IN_PROGRESS);
                    bookingRepository.save(booking);
                }
                break;

            case INSPECTION:
                job.setInspectionStartedAt(now);
                break;

            case WORK_IN_PROGRESS:
                job.setWorkStartedAt(now);
                if (sr.getStatus() != ServiceRequestStatus.IN_PROGRESS) {
                    sr.setStatus(ServiceRequestStatus.IN_PROGRESS);
                    serviceRequestRepository.save(sr);
                }
                if (booking != null && booking.getStatus() != BookingStatus.IN_PROGRESS) {
                    booking.setStatus(BookingStatus.IN_PROGRESS);
                    bookingRepository.save(booking);
                }
                break;

            case READY_FOR_DELIVERY:
                job.setReadyForDeliveryAt(now);
                break;

            case COMPLETED:
                job.setCompletedAt(now);
                sr.setStatus(ServiceRequestStatus.COMPLETED);
                serviceRequestRepository.save(sr);
                if (booking != null) {
                    booking.setStatus(BookingStatus.COMPLETED);
                    bookingRepository.save(booking);
                }
                if (opp != null) {
                    opp.setStatus(OpportunityStatus.COMPLETED);
                    leadOpportunityRepository.save(opp);
                }
                break;

            case CANCELLED:
                job.setCancelledAt(now);
                break;

            case TRANSFERRED:
                job.setTransferredAt(now);
                break;
        }

        WorkshopJob savedJob = workshopJobRepository.save(job);

        // 3. Audit Logging
        MarketplaceEventType eventType = (newStatus == WorkshopJobStatus.COMPLETED)
                ? MarketplaceEventType.SERVICE_COMPLETED
                : MarketplaceEventType.JOB_STATUS_CHANGED;

        auditService.recordEvent(
                eventType,
                sr.getId(),
                opp != null ? opp.getId() : null,
                workshop.getId(),
                currentUser.getId(),
                "Job #" + savedJob.getJobReference() + " status transitioned from " + oldStatus + " to " + newStatus,
                "{\"jobReference\": \"" + savedJob.getJobReference() + "\", \"from\": \"" + oldStatus + "\", \"to\": \"" + newStatus + "\", \"notes\": \"" + (job.getNotes() != null ? job.getNotes() : "") + "\"}"
        );

        // 4. Notification to customer
        notificationService.notifyCustomer(
                sr.getUser().getId(),
                "JOB_STATUS_UPDATE",
                "Service Status Update: " + newStatus,
                "Your vehicle service status is now: " + getFriendlyTitleForStatus(newStatus),
                Map.of("requestReference", sr.getRequestReference(), "status", newStatus.name())
        );

        return mapToJobResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public List<CustomerJobTrackingResponse> getActiveJobsForCustomer(User currentUser) {
        List<WorkshopJob> activeJobs = workshopJobRepository.findActiveJobsByCustomerUserId(currentUser.getId());
        return activeJobs.stream().map(this::mapToCustomerTrackingResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerJobTrackingResponse getCustomerJobTracking(User currentUser, Long serviceRequestId) {
        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + serviceRequestId));

        if (!sr.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("You are not authorized to track this service request");
        }

        WorkshopJob job = workshopJobRepository.findByServiceRequestId(serviceRequestId).orElse(null);
        return mapToCustomerTrackingResponse(sr, job);
    }

    public WorkshopJobResponse mapToJobResponse(WorkshopJob job) {
        WorkshopJobResponse res = new WorkshopJobResponse();
        res.setId(job.getId());
        res.setJobReference(job.getJobReference());
        res.setStatus(job.getStatus());
        res.setNotes(job.getNotes());
        res.setWorkshopId(job.getWorkshop().getId());
        res.setWorkshopName(job.getWorkshop().getBusinessName());

        ServiceRequest sr = job.getServiceRequest();
        res.setServiceRequestId(sr.getId());
        res.setServiceRequestReference(sr.getRequestReference());
        res.setBookingReference(sr.getBookingReference());
        res.setTotalAmount(sr.getTotalAmount());
        res.setPreferredDate(sr.getPreferredDate());
        res.setPreferredTimeSlot(sr.getPreferredTimeSlot());
        res.setCustomerNotes(sr.getCustomerNotes());

        Vehicle v = sr.getVehicle();
        if (v != null) {
            res.setVehicleId(v.getId());
            res.setVehicleSummary(v.getMake() + " " + v.getModel() + " (" + v.getYear() + ") - " + v.getRegistrationNumber());
        }

        // Unlocked Customer PII (authorized since workshop won this job)
        User u = sr.getUser();
        res.setCustomer(new WorkshopJobResponse.CustomerUnlockedInfo(
                u.getName(),
                u.getPhone(),
                u.getEmail(),
                sr.getAddress(),
                sr.getCity(),
                sr.getPincode()
        ));

        List<ServiceRequestItemResponse> items = sr.getItems().stream()
                .map(i -> new ServiceRequestItemResponse(
                        i.getId(),
                        i.getServiceCatalog() != null ? i.getServiceCatalog().getId() : null,
                        i.getServiceNameSnapshot(),
                        i.getBasePriceSnapshot(),
                        i.getDiscountTypeSnapshot(),
                        i.getDiscountValueSnapshot(),
                        i.getFinalPriceSnapshot()
                ))
                .collect(Collectors.toList());
        res.setRequestedServices(items);

        res.setAssignedAt(job.getAssignedAt());
        res.setConfirmedAt(job.getConfirmedAt());
        res.setVehicleReceivedAt(job.getVehicleReceivedAt());
        res.setInspectionStartedAt(job.getInspectionStartedAt());
        res.setWorkStartedAt(job.getWorkStartedAt());
        res.setReadyForDeliveryAt(job.getReadyForDeliveryAt());
        res.setCompletedAt(job.getCompletedAt());
        res.setCancelledAt(job.getCancelledAt());
        res.setTransferredAt(job.getTransferredAt());
        res.setCreatedAt(job.getCreatedAt());
        res.setUpdatedAt(job.getUpdatedAt());

        // Allowed next stages
        List<WorkshopJobStatus> nextStatuses = new ArrayList<>();
        for (WorkshopJobStatus candidate : WorkshopJobStatus.values()) {
            if (job.canTransitionTo(candidate) && candidate != WorkshopJobStatus.CANCELLED) {
                nextStatuses.add(candidate);
            }
        }
        res.setAllowedNextStatuses(nextStatuses);
        res.setCanTransfer(job.getStatus() == WorkshopJobStatus.ASSIGNED || job.getStatus() == WorkshopJobStatus.CONFIRMED);

        return res;
    }

    public CustomerJobTrackingResponse mapToCustomerTrackingResponse(WorkshopJob job) {
        return mapToCustomerTrackingResponse(job.getServiceRequest(), job);
    }

    public CustomerJobTrackingResponse mapToCustomerTrackingResponse(ServiceRequest sr, WorkshopJob job) {
        CustomerJobTrackingResponse res = new CustomerJobTrackingResponse();
        res.setServiceRequestId(sr.getId());
        res.setRequestReference(sr.getRequestReference());
        res.setBookingReference(sr.getBookingReference());
        if (sr.getBooking() != null) {
            res.setBookingId(sr.getBooking().getId());
        }
        res.setStatus(sr.getStatus().name());

        Vehicle v = sr.getVehicle();
        if (v != null) {
            res.setVehicleId(v.getId());
            res.setVehicleSummary(v.getMake() + " " + v.getModel() + " (" + v.getYear() + ") - " + v.getRegistrationNumber());
        }

        res.setTotalAmount(sr.getTotalAmount());
        res.setAppointmentDate(sr.getPreferredDate());
        res.setAppointmentTimeSlot(sr.getPreferredTimeSlot());
        res.setCustomerNotes(sr.getCustomerNotes());

        List<ServiceRequestItemResponse> items = sr.getItems().stream()
                .map(i -> new ServiceRequestItemResponse(
                        i.getId(),
                        i.getServiceCatalog() != null ? i.getServiceCatalog().getId() : null,
                        i.getServiceNameSnapshot(),
                        i.getBasePriceSnapshot(),
                        i.getDiscountTypeSnapshot(),
                        i.getDiscountValueSnapshot(),
                        i.getFinalPriceSnapshot()
                ))
                .collect(Collectors.toList());
        res.setServices(items);

        // Assigned Workshop (safe customer view)
        Workshop w = sr.getAssignedWorkshop();
        if (w != null) {
            res.setWorkshop(new CustomerJobTrackingResponse.AssignedWorkshopSummary(
                    w.getId(),
                    w.getBusinessName(),
                    w.getAddress(),
                    w.getCity(),
                    w.getPhone()
            ));
        }

        // Job Status & Progress Mapping
        WorkshopJobStatus jStatus = job != null ? job.getStatus() : null;
        if (jStatus != null) {
            res.setJobStatus(jStatus.name());
        }

        int step = calculateCustomerStep(sr.getStatus(), jStatus);
        res.setCurrentStep(step);
        res.setFriendlyStatusTitle(getFriendlyTitle(sr.getStatus(), jStatus));
        res.setFriendlyStatusDescription(getFriendlyDescription(sr.getStatus(), jStatus));

        // Cancellable calculation
        boolean canCancel = (sr.getStatus() == ServiceRequestStatus.SUBMITTED
                || sr.getStatus() == ServiceRequestStatus.MATCHED
                || sr.getStatus() == ServiceRequestStatus.RE_MATCHING
                || (sr.getStatus() == ServiceRequestStatus.ACCEPTED && (jStatus == null || jStatus == WorkshopJobStatus.ASSIGNED || jStatus == WorkshopJobStatus.CONFIRMED)));
        res.setCancellable(canCancel);

        // Timeline milestones
        res.setTimeline(buildCustomerTimeline(sr, job, step));

        return res;
    }

    private int calculateCustomerStep(ServiceRequestStatus srStatus, WorkshopJobStatus jStatus) {
        if (srStatus == ServiceRequestStatus.CANCELLED || jStatus == WorkshopJobStatus.CANCELLED) {
            return -1;
        }
        if (jStatus == null) {
            if (srStatus == ServiceRequestStatus.SUBMITTED || srStatus == ServiceRequestStatus.MATCHED || srStatus == ServiceRequestStatus.RE_MATCHING) {
                return 1;
            }
            if (srStatus == ServiceRequestStatus.ACCEPTED) {
                return 2;
            }
            return 1;
        }

        switch (jStatus) {
            case ASSIGNED:
                return 2;
            case CONFIRMED:
                return 3;
            case VEHICLE_RECEIVED:
                return 4;
            case INSPECTION:
            case WORK_IN_PROGRESS:
                return 5;
            case READY_FOR_DELIVERY:
                return 6;
            case COMPLETED:
                return 7;
            case TRANSFERRED:
                return 1; // back to matching
            default:
                return 1;
        }
    }

    private String getFriendlyTitle(ServiceRequestStatus srStatus, WorkshopJobStatus jStatus) {
        if (srStatus == ServiceRequestStatus.CANCELLED || jStatus == WorkshopJobStatus.CANCELLED) {
            return "Booking Cancelled";
        }
        if (srStatus == ServiceRequestStatus.RE_MATCHING || jStatus == WorkshopJobStatus.TRANSFERRED) {
            return "Matching with another service centre";
        }
        if (jStatus == null) {
            if (srStatus == ServiceRequestStatus.SUBMITTED || srStatus == ServiceRequestStatus.MATCHED) {
                return "Finding a service centre";
            }
            if (srStatus == ServiceRequestStatus.ACCEPTED) {
                return "Service centre assigned";
            }
            return "Booking Placed";
        }

        switch (jStatus) {
            case ASSIGNED:
                return "Service centre assigned";
            case CONFIRMED:
                return "Appointment confirmed by service centre";
            case VEHICLE_RECEIVED:
                return "Vehicle received at workshop";
            case INSPECTION:
                return "Vehicle inspection in progress";
            case WORK_IN_PROGRESS:
                return "Service in progress";
            case READY_FOR_DELIVERY:
                return "Your vehicle is ready for delivery";
            case COMPLETED:
                return "Service completed";
            default:
                return "Appointment Scheduled";
        }
    }

    private String getFriendlyDescription(ServiceRequestStatus srStatus, WorkshopJobStatus jStatus) {
        if (srStatus == ServiceRequestStatus.CANCELLED || jStatus == WorkshopJobStatus.CANCELLED) {
            return "This booking was cancelled. Any workshop fees have been settled.";
        }
        if (srStatus == ServiceRequestStatus.RE_MATCHING || jStatus == WorkshopJobStatus.TRANSFERRED) {
            return "Your previous service centre was at peak capacity. We are re-matching your vehicle with another top-rated local workshop.";
        }
        if (jStatus == null) {
            if (srStatus == ServiceRequestStatus.SUBMITTED || srStatus == ServiceRequestStatus.MATCHED) {
                return "We are locating authorized workshops matching your vehicle and selected services.";
            }
            if (srStatus == ServiceRequestStatus.ACCEPTED) {
                return "An authorized workshop has accepted your booking and will contact you shortly.";
            }
            return "Your appointment has been registered.";
        }

        switch (jStatus) {
            case ASSIGNED:
                return "An authorized workshop has accepted your booking. You can view their location and contact details.";
            case CONFIRMED:
                return "The workshop has confirmed your service time slot. Please arrive at the workshop at the scheduled time.";
            case VEHICLE_RECEIVED:
                return "Your car has arrived at the workshop and has been checked into the service bay.";
            case INSPECTION:
                return "Technicians are performing a multi-point digital diagnostic inspection.";
            case WORK_IN_PROGRESS:
                return "Maintenance and repair operations are currently underway using genuine parts.";
            case READY_FOR_DELIVERY:
                return "Service is finished and final quality inspections are complete. Your vehicle is ready for pickup.";
            case COMPLETED:
                return "All service procedures have been completed successfully. Thank you for using our platform!";
            default:
                return "Appointment is in progress.";
        }
    }

    private String getFriendlyTitleForStatus(WorkshopJobStatus status) {
        switch (status) {
            case CONFIRMED:
                return "Appointment Confirmed";
            case VEHICLE_RECEIVED:
                return "Vehicle Received at Service Centre";
            case INSPECTION:
                return "Inspection in Progress";
            case WORK_IN_PROGRESS:
                return "Work in Progress";
            case READY_FOR_DELIVERY:
                return "Vehicle Ready for Delivery";
            case COMPLETED:
                return "Service Completed";
            default:
                return status.name();
        }
    }

    private List<CustomerJobTrackingResponse.TrackingTimelineItem> buildCustomerTimeline(ServiceRequest sr, WorkshopJob job, int currentStep) {
        List<CustomerJobTrackingResponse.TrackingTimelineItem> timeline = new ArrayList<>();

        timeline.add(new CustomerJobTrackingResponse.TrackingTimelineItem(
                "BOOKED",
                "Booking Placed",
                "Service requested for " + sr.getPreferredDate() + " (" + sr.getPreferredTimeSlot() + ")",
                sr.getCreatedAt(),
                currentStep >= 1,
                currentStep == 1
        ));

        timeline.add(new CustomerJobTrackingResponse.TrackingTimelineItem(
                "ASSIGNED",
                "Workshop Assigned",
                sr.getAssignedWorkshop() != null ? sr.getAssignedWorkshop().getBusinessName() + " assigned" : "Awaiting workshop acceptance",
                job != null ? job.getAssignedAt() : null,
                currentStep >= 2,
                currentStep == 2
        ));

        timeline.add(new CustomerJobTrackingResponse.TrackingTimelineItem(
                "CONFIRMED",
                "Appointment Confirmed",
                "Workshop confirmed time slot with customer",
                job != null ? job.getConfirmedAt() : null,
                currentStep >= 3,
                currentStep == 3
        ));

        timeline.add(new CustomerJobTrackingResponse.TrackingTimelineItem(
                "RECEIVED",
                "Vehicle Received",
                "Vehicle checked into workshop bay",
                job != null ? job.getVehicleReceivedAt() : null,
                currentStep >= 4,
                currentStep == 4
        ));

        timeline.add(new CustomerJobTrackingResponse.TrackingTimelineItem(
                "IN_PROGRESS",
                "Service in Progress",
                "Technicians performing requested repairs",
                job != null ? (job.getWorkStartedAt() != null ? job.getWorkStartedAt() : job.getInspectionStartedAt()) : null,
                currentStep >= 5,
                currentStep == 5
        ));

        timeline.add(new CustomerJobTrackingResponse.TrackingTimelineItem(
                "READY",
                "Ready for Pickup",
                "Quality audit complete, car ready for delivery",
                job != null ? job.getReadyForDeliveryAt() : null,
                currentStep >= 6,
                currentStep == 6
        ));

        timeline.add(new CustomerJobTrackingResponse.TrackingTimelineItem(
                "COMPLETED",
                "Service Completed",
                "Vehicle handed over and settled",
                job != null ? job.getCompletedAt() : null,
                currentStep >= 7,
                currentStep == 7
        ));

        return timeline;
    }
}

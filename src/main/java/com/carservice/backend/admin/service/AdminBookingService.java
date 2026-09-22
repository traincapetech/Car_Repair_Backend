package com.carservice.backend.admin.service;

import com.carservice.backend.admin.dto.*;
import com.carservice.backend.booking.dto.BookingResponse.BookingServiceItemResponse;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.common.notification.NotificationService;
import com.carservice.backend.common.notification.enums.NotificationType;
import com.carservice.backend.marketplace.entity.LeadOpportunity;
import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.entity.WorkshopJob;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import com.carservice.backend.marketplace.repository.LeadOpportunityRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminBookingService {

    private static final Logger log = LoggerFactory.getLogger(AdminBookingService.class);

    private final BookingRepository bookingRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WorkshopJobRepository workshopJobRepository;
    private final LeadOpportunityRepository leadOpportunityRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AdminBookingService(
            BookingRepository bookingRepository,
            ServiceRequestRepository serviceRequestRepository,
            WorkshopJobRepository workshopJobRepository,
            LeadOpportunityRepository leadOpportunityRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.workshopJobRepository = workshopJobRepository;
        this.leadOpportunityRepository = leadOpportunityRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    /**
     * Real-time operational KPI summary calculated from actual database records.
     */
    public AdminBookingSummaryResponse getBookingSummary() {
        long total = bookingRepository.count();
        long pending = bookingRepository.countByStatus(BookingStatus.PENDING);
        long confirmed = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long inProgress = bookingRepository.countByStatus(BookingStatus.IN_PROGRESS);
        long completed = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long cancelled = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        BigDecimal revenue = bookingRepository.sumTotalAmountNonCancelled();

        return new AdminBookingSummaryResponse(
                total,
                pending,
                confirmed,
                inProgress,
                completed,
                cancelled,
                revenue != null ? revenue : BigDecimal.ZERO
        );
    }

    /**
     * Paginated, searchable, and filtered list of bookings.
     */
    public Page<AdminBookingListResponse> getBookings(
            String search,
            BookingStatus status,
            Long workshopId,
            Long customerId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<Booking> page = bookingRepository.findBookingsWithFilter(
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
     * Comprehensive 360° booking dossier including workshop routing and opportunity pipeline.
     */
    public AdminBookingDetailResponse getBookingDetail(Long id) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        AdminBookingDetailResponse response = new AdminBookingDetailResponse();
        response.setId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setBookingDate(booking.getBookingDate());
        response.setBookingTime(booking.getBookingTime());
        response.setTimeSlot(booking.getTimeSlot());
        response.setStatus(booking.getStatus());
        response.setCustomerNotes(booking.getCustomerNotes());
        response.setEstimatedPrice(booking.getEstimatedPrice());
        response.setTotalAmount(booking.getTotalAmount());
        response.setCity(booking.getCity());
        response.setAddress(booking.getAddress());
        response.setPincode(booking.getPincode());
        response.setLatitude(booking.getLatitude());
        response.setLongitude(booking.getLongitude());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        response.setCancelledAt(booking.getCancelledAt());

        // Customer Dossier
        if (booking.getUser() != null) {
            User u = booking.getUser();
            response.setCustomerId(u.getId());
            response.setCustomerName(u.getName());
            response.setCustomerEmail(u.getEmail());
            response.setCustomerPhone(u.getPhone());
            response.setCustomerCreatedAt(u.getCreatedAt());
        }

        // Vehicle Dossier
        if (booking.getVehicle() != null) {
            Vehicle v = booking.getVehicle();
            response.setVehicleId(v.getId());
            response.setVehicleMake(v.getMake());
            response.setVehicleModel(v.getModel());
            response.setVehicleYear(v.getYear());
            response.setVehicleRegistrationNumber(v.getRegistrationNumber());
            response.setVehicleFuelType(v.getFuelType());
            response.setVehicleTransmission(v.getTransmission());
        }

        // Services Line Items
        if (booking.getBookingServices() != null) {
            List<BookingServiceItemResponse> serviceResponses = booking.getBookingServices().stream()
                    .map(bs -> new BookingServiceItemResponse(
                            bs.getServiceCatalog() != null ? bs.getServiceCatalog().getId() : null,
                            bs.getServiceNameSnapshot(),
                            bs.getBasePriceSnapshot(),
                            bs.getDiscountTypeSnapshot(),
                            bs.getDiscountValueSnapshot(),
                            bs.getFinalPriceSnapshot()
                    ))
                    .collect(Collectors.toList());
            response.setServices(serviceResponses);
        }

        // Service Request Linkage
        ServiceRequest sr = booking.getServiceRequest();
        if (sr != null) {
            response.setServiceRequestId(sr.getId());
            response.setServiceRequestReference(sr.getRequestReference());
            response.setServiceRequestStatus(sr.getStatus());
            response.setPreferredDate(sr.getPreferredDate());
            response.setPreferredTimeSlot(sr.getPreferredTimeSlot());

            // Assigned Workshop
            if (sr.getAssignedWorkshop() != null) {
                Workshop w = sr.getAssignedWorkshop();
                response.setAssignedWorkshopId(w.getId());
                response.setAssignedWorkshopName(w.getBusinessName());
                response.setAssignedWorkshopPhone(w.getPhone());
                response.setAssignedWorkshopEmail(w.getEmail());
                response.setAssignedWorkshopAddress(w.getAddress());
                response.setAssignedWorkshopCity(w.getCity());
                response.setAssignedWorkshopState(w.getState());
                response.setAssignedWorkshopStatus(w.getVerificationStatus());
            }

            // Current Job
            if (sr.getCurrentJob() != null) {
                WorkshopJob j = sr.getCurrentJob();
                response.setCurrentJobId(j.getId());
                response.setCurrentJobReference(j.getJobReference());
                response.setCurrentJobStatus(j.getStatus());
            }

            // Workshop Routing & Matching Opportunities Pipeline
            List<LeadOpportunity> opportunities = leadOpportunityRepository.findByServiceRequestIdWithDetails(sr.getId());
            List<AdminMarketplaceOpportunityResponse> oppList = opportunities.stream()
                    .map(this::mapToOpportunityResponse)
                    .collect(Collectors.toList());
            response.setRoutingOpportunities(oppList);
        }

        return response;
    }

    /**
     * Administrative cancellation of a booking with complete audit and notification hooks.
     */
    @Transactional
    public AdminBookingDetailResponse cancelBooking(User adminUser, Long id, AdminCancelBookingRequest request) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed booking.");
        }

        String previousStatus = booking.getStatus().name();
        String reason = (request != null && request.getReason() != null) ? request.getReason().trim() : "Cancelled by Platform Administrator";

        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setUpdatedAt(now);
        Booking savedBooking = bookingRepository.save(booking);

        // Synchronize linked ServiceRequest and WorkshopJob
        ServiceRequest sr = savedBooking.getServiceRequest();
        if (sr != null) {
            sr.setStatus(ServiceRequestStatus.CANCELLED);
            serviceRequestRepository.save(sr);

            if (sr.getCurrentJob() != null) {
                WorkshopJob job = sr.getCurrentJob();
                job.setStatus(WorkshopJobStatus.CANCELLED);
                job.setCancelledAt(now);
                job.setCancellationReason(reason);
                workshopJobRepository.save(job);
            }
        }

        // Immutable Audit Log
        auditService.record(
                adminUser,
                "BOOKING_CANCELLED",
                "BOOKING",
                savedBooking.getId().toString(),
                "Admin cancelled booking #" + savedBooking.getBookingReference() + ": " + reason,
                "SUCCESS",
                Map.of("status", previousStatus),
                Map.of("status", BookingStatus.CANCELLED.name(), "reason", reason),
                Map.of("bookingReference", savedBooking.getBookingReference(), "cancelledAt", now.toString())
        );

        // Persistent in-app + realtime notification to customer
        if (savedBooking.getUser() != null) {
            try {
                notificationService.sendNotification(
                        savedBooking.getUser().getId(),
                        "CUSTOMER",
                        NotificationType.BOOKING_CANCELLED,
                        "Booking Cancelled",
                        "Your booking #" + savedBooking.getBookingReference() + " has been cancelled by administration: " + reason,
                        "BOOKING",
                        savedBooking.getId(),
                        Map.of("bookingReference", savedBooking.getBookingReference(), "status", "CANCELLED")
                );
            } catch (Exception e) {
                log.warn("Notice: Failed to dispatch customer notification for booking #{}: {}", savedBooking.getId(), e.getMessage());
            }
        }

        return getBookingDetail(savedBooking.getId());
    }

    private AdminBookingListResponse mapToListResponse(Booking b) {
        AdminBookingListResponse dto = new AdminBookingListResponse();
        dto.setId(b.getId());
        dto.setBookingReference(b.getBookingReference());
        dto.setBookingDate(b.getBookingDate());
        dto.setBookingTime(b.getBookingTime());
        dto.setTimeSlot(b.getTimeSlot());
        dto.setStatus(b.getStatus());
        dto.setTotalAmount(b.getTotalAmount());
        dto.setPrimaryServiceName(b.getServiceNameSnapshot());
        dto.setCreatedAt(b.getCreatedAt());

        if (b.getBookingServices() != null) {
            dto.setTotalServicesCount(b.getBookingServices().size());
        }

        if (b.getUser() != null) {
            dto.setCustomerId(b.getUser().getId());
            dto.setCustomerName(b.getUser().getName());
            dto.setCustomerEmail(b.getUser().getEmail());
            dto.setCustomerPhone(b.getUser().getPhone());
        }

        if (b.getVehicle() != null) {
            dto.setVehicleId(b.getVehicle().getId());
            dto.setVehicleMake(b.getVehicle().getMake());
            dto.setVehicleModel(b.getVehicle().getModel());
            dto.setVehicleRegistrationNumber(b.getVehicle().getRegistrationNumber());
        }

        ServiceRequest sr = b.getServiceRequest();
        if (sr != null) {
            dto.setServiceRequestId(sr.getId());
            dto.setServiceRequestReference(sr.getRequestReference());
            if (sr.getAssignedWorkshop() != null) {
                dto.setWorkshopId(sr.getAssignedWorkshop().getId());
                dto.setWorkshopName(sr.getAssignedWorkshop().getBusinessName());
                dto.setWorkshopCity(sr.getAssignedWorkshop().getCity());
            }
            if (sr.getCurrentJob() != null) {
                dto.setCurrentJobId(sr.getCurrentJob().getId());
                dto.setCurrentJobStatus(sr.getCurrentJob().getStatus().name());
            }
        }

        return dto;
    }

    private AdminMarketplaceOpportunityResponse mapToOpportunityResponse(LeadOpportunity o) {
        AdminMarketplaceOpportunityResponse dto = new AdminMarketplaceOpportunityResponse();
        dto.setId(o.getId());
        dto.setStatus(o.getStatus());
        dto.setFeeSnapshot(o.getFeeSnapshot());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setAcceptedAt(o.getAcceptedAt());
        dto.setPaidAt(o.getPaidAt());
        dto.setCustomerDetailsUnlocked(o.isCustomerDetailsUnlocked());

        if (o.getWorkshop() != null) {
            Workshop w = o.getWorkshop();
            dto.setWorkshopId(w.getId());
            dto.setWorkshopName(w.getBusinessName());
            dto.setWorkshopPhone(w.getPhone());
            dto.setWorkshopCity(w.getCity());
        }

        return dto;
    }
}

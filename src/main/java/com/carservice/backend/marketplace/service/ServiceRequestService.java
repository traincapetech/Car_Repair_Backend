package com.carservice.backend.marketplace.service;

import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.enums.BookingTimeSlot;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.common.notification.NotificationService;
import com.carservice.backend.marketplace.dto.CreateServiceRequestRequest;
import com.carservice.backend.marketplace.dto.ServiceRequestItemResponse;
import com.carservice.backend.marketplace.dto.ServiceRequestResponse;
import com.carservice.backend.marketplace.entity.LeadOpportunity;
import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.ServiceRequestItem;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.entity.WorkshopJob;
import com.carservice.backend.marketplace.entity.WorkshopPayment;
import com.carservice.backend.marketplace.enums.MarketplaceEventType;
import com.carservice.backend.marketplace.enums.OpportunityStatus;
import com.carservice.backend.marketplace.enums.PaymentStatus;
import com.carservice.backend.marketplace.enums.RefundReason;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import com.carservice.backend.marketplace.repository.LeadOpportunityRepository;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.marketplace.repository.WorkshopPaymentRepository;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.vehicle.entity.Vehicle;
import com.carservice.backend.vehicle.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServiceRequestService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestService.class);

    private final ServiceRequestRepository serviceRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final LeadOpportunityRepository leadOpportunityRepository;
    private final MatchingEngineService matchingEngineService;
    private final PlatformConfigService platformConfigService;
    private final MarketplaceAuditService auditService;
    private final BookingRepository bookingRepository;
    private final WorkshopRefundService refundService;
    private final WorkshopPaymentRepository workshopPaymentRepository;
    private final NotificationService notificationService;

    public ServiceRequestService(
            ServiceRequestRepository serviceRequestRepository,
            VehicleRepository vehicleRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            LeadOpportunityRepository leadOpportunityRepository,
            MatchingEngineService matchingEngineService,
            PlatformConfigService platformConfigService,
            MarketplaceAuditService auditService,
            BookingRepository bookingRepository,
            WorkshopRefundService refundService,
            WorkshopPaymentRepository workshopPaymentRepository,
            NotificationService notificationService
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.leadOpportunityRepository = leadOpportunityRepository;
        this.matchingEngineService = matchingEngineService;
        this.platformConfigService = platformConfigService;
        this.auditService = auditService;
        this.bookingRepository = bookingRepository;
        this.refundService = refundService;
        this.workshopPaymentRepository = workshopPaymentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ServiceRequestResponse createServiceRequest(User currentUser, CreateServiceRequestRequest request) {
        // 1. Validate vehicle
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        if (!vehicle.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("You can only request services for vehicles registered under your account");
        }

        // 2. Validate services
        if (request.getServiceIds() == null || request.getServiceIds().isEmpty()) {
            throw new IllegalArgumentException("At least one service must be selected");
        }

        List<ServiceCatalog> services = serviceCatalogRepository.findAllById(request.getServiceIds());
        if (services.size() != request.getServiceIds().size()) {
            throw new IllegalArgumentException("One or more selected services could not be found");
        }

        for (ServiceCatalog service : services) {
            if (!Boolean.TRUE.equals(service.getIsActive())) {
                throw new IllegalArgumentException("Service '" + service.getName() + "' is currently inactive");
            }
        }

        // 3. Create ServiceRequest entity
        ServiceRequest serviceRequest = new ServiceRequest(
                currentUser,
                vehicle,
                request.getCity(),
                request.getAddress(),
                request.getPincode(),
                request.getLatitude(),
                request.getLongitude(),
                request.getPreferredDate(),
                request.getPreferredTimeSlot(),
                request.getCustomerNotes()
        );

        // 4. Attach items with snapshots
        for (ServiceCatalog catalogItem : services) {
            BigDecimal finalPrice = catalogItem.calculateFinalPrice();
            ServiceRequestItem item = new ServiceRequestItem(
                    serviceRequest,
                    catalogItem,
                    catalogItem.getName(),
                    catalogItem.getBasePrice(),
                    catalogItem.getDiscountType(),
                    catalogItem.getDiscountValue(),
                    finalPrice
            );
            serviceRequest.addItem(item);
        }

        serviceRequest.calculateTotalAmount();
        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);

        // 5. Create linked booking
        LocalDate bDate = request.getPreferredDate() != null ? request.getPreferredDate() : LocalDate.now().plusDays(1);
        String bookingRef = Booking.generateBookingReference(bDate);
        while (bookingRepository.existsByBookingReference(bookingRef)) {
            bookingRef = Booking.generateBookingReference(bDate);
        }

        Booking booking = new Booking();
        booking.setBookingReference(bookingRef);
        booking.setUser(currentUser);
        booking.setVehicle(vehicle);
        booking.setBookingDate(bDate);
        String slot = request.getPreferredTimeSlot() != null ? request.getPreferredTimeSlot() : "10:00 - 11:00";
        booking.setTimeSlot(slot);
        booking.setBookingTime(BookingTimeSlot.fromSlot(slot).orElse(BookingTimeSlot.SLOT_10_11).getStartTime());
        booking.setStatus(BookingStatus.PENDING);
        booking.setCity(request.getCity());
        booking.setAddress(request.getAddress());
        booking.setPincode(request.getPincode());
        booking.setLatitude(request.getLatitude());
        booking.setLongitude(request.getLongitude());
        booking.setCustomerNotes(request.getCustomerNotes());
        booking.setTotalAmount(savedRequest.getTotalAmount());

        for (ServiceCatalog s : services) {
            BigDecimal finalPrice = s.calculateFinalPrice();
            com.carservice.backend.booking.entity.BookingService lineItem =
                    new com.carservice.backend.booking.entity.BookingService(
                            booking,
                            s,
                            s.getName(),
                            s.getBasePrice(),
                            s.getDiscountType() != null ? s.getDiscountType() : com.carservice.backend.servicecatalog.enums.DiscountType.NO_DISCOUNT,
                            s.getDiscountValue() != null ? s.getDiscountValue() : BigDecimal.ZERO,
                            finalPrice
                    );
            booking.addBookingService(lineItem);
        }
        if (!services.isEmpty()) {
            ServiceCatalog primary = services.get(0);
            booking.setService(primary);
            booking.setServiceNameSnapshot(primary.getName());
            booking.setServicePriceSnapshot(primary.getBasePrice());
            booking.setEstimatedPrice(savedRequest.getTotalAmount());
        }

        booking.setServiceRequest(savedRequest);
        booking.setServiceRequestReference(savedRequest.getRequestReference());
        Booking savedBooking = bookingRepository.save(booking);

        savedRequest.setBooking(savedBooking);
        savedRequest.setBookingReference(savedBooking.getBookingReference());
        savedRequest = serviceRequestRepository.save(savedRequest);

        auditService.recordEvent(
                MarketplaceEventType.REQUEST_CREATED,
                savedRequest.getId(),
                null,
                null,
                currentUser.getId(),
                "Customer created service request " + savedRequest.getRequestReference(),
                "{\"totalAmount\": " + savedRequest.getTotalAmount() + ", \"itemsCount\": " + services.size() + "}"
        );

        // 6. Match eligible workshops if marketplace is enabled
        if (!platformConfigService.isMarketplaceEnabled()) {
            log.info("Marketplace matching is disabled by configuration. Request {} remains in SUBMITTED state.",
                    savedRequest.getRequestReference());
            return mapToResponse(savedRequest);
        }

        List<Workshop> matchedWorkshops = matchingEngineService.findEligibleWorkshops(savedRequest, Collections.emptySet());
        int maxWorkshops = platformConfigService.getMaxWorkshopsPerRequest();
        if (matchedWorkshops.size() > maxWorkshops) {
            matchedWorkshops = matchedWorkshops.subList(0, maxWorkshops);
        }
        BigDecimal currentFee = platformConfigService.getLeadAcceptanceFee();

        if (!matchedWorkshops.isEmpty()) {
            for (Workshop workshop : matchedWorkshops) {
                LeadOpportunity opportunity = new LeadOpportunity(savedRequest, workshop, currentFee);
                LeadOpportunity savedOpportunity = leadOpportunityRepository.save(opportunity);

                auditService.recordEvent(
                        MarketplaceEventType.OPPORTUNITY_CREATED,
                        savedRequest.getId(),
                        savedOpportunity.getId(),
                        workshop.getId(),
                        null,
                        "Lead opportunity generated for workshop " + workshop.getBusinessName() + " with fee ₹" + currentFee,
                        "{\"feeSnapshot\": " + currentFee + "}"
                );
            }

            savedRequest.setStatus(ServiceRequestStatus.MATCHED);
            savedRequest = serviceRequestRepository.save(savedRequest);

            auditService.recordEvent(
                    MarketplaceEventType.WORKSHOPS_MATCHED,
                    savedRequest.getId(),
                    null,
                    null,
                    null,
                    "Matched " + matchedWorkshops.size() + " workshops for service request " + savedRequest.getRequestReference(),
                    "{\"matchedCount\": " + matchedWorkshops.size() + "}"
            );
        }

        return mapToResponse(savedRequest);
    }

    @Transactional
    public ServiceRequest createAndMatchServiceRequestForBooking(Booking booking, List<ServiceCatalog> services) {
        ServiceRequest serviceRequest = new ServiceRequest(
                booking.getUser(),
                booking.getVehicle(),
                booking.getCity() != null && !booking.getCity().isBlank() ? booking.getCity() : "New Delhi",
                booking.getAddress() != null && !booking.getAddress().isBlank() ? booking.getAddress() : "Customer Address",
                booking.getPincode() != null && !booking.getPincode().isBlank() ? booking.getPincode() : "110001",
                booking.getLatitude(),
                booking.getLongitude(),
                booking.getBookingDate(),
                booking.getTimeSlot(),
                booking.getCustomerNotes()
        );
        serviceRequest.setBooking(booking);
        serviceRequest.setBookingReference(booking.getBookingReference());

        for (ServiceCatalog catalogItem : services) {
            BigDecimal finalPrice = catalogItem.calculateFinalPrice();
            ServiceRequestItem item = new ServiceRequestItem(
                    serviceRequest,
                    catalogItem,
                    catalogItem.getName(),
                    catalogItem.getBasePrice(),
                    catalogItem.getDiscountType(),
                    catalogItem.getDiscountValue(),
                    finalPrice
            );
            serviceRequest.addItem(item);
        }

        serviceRequest.calculateTotalAmount();
        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);

        auditService.recordEvent(
                MarketplaceEventType.REQUEST_CREATED,
                savedRequest.getId(),
                null,
                null,
                booking.getUser().getId(),
                "Marketplace service request " + savedRequest.getRequestReference() + " created for booking " + booking.getBookingReference(),
                "{\"totalAmount\": " + savedRequest.getTotalAmount() + ", \"itemsCount\": " + services.size() + "}"
        );

        // Match eligible workshops if marketplace is enabled
        if (!platformConfigService.isMarketplaceEnabled()) {
            log.info("Marketplace matching is disabled by configuration. Request {} remains in SUBMITTED state.",
                    savedRequest.getRequestReference());
            return savedRequest;
        }

        List<Workshop> matchedWorkshops = matchingEngineService.findEligibleWorkshops(savedRequest, Collections.emptySet());
        int maxWorkshops = platformConfigService.getMaxWorkshopsPerRequest();
        if (matchedWorkshops.size() > maxWorkshops) {
            matchedWorkshops = matchedWorkshops.subList(0, maxWorkshops);
        }
        BigDecimal currentFee = platformConfigService.getLeadAcceptanceFee();

        if (!matchedWorkshops.isEmpty()) {
            for (Workshop workshop : matchedWorkshops) {
                LeadOpportunity opportunity = new LeadOpportunity(savedRequest, workshop, currentFee);
                LeadOpportunity savedOpportunity = leadOpportunityRepository.save(opportunity);

                auditService.recordEvent(
                        MarketplaceEventType.OPPORTUNITY_CREATED,
                        savedRequest.getId(),
                        savedOpportunity.getId(),
                        workshop.getId(),
                        null,
                        "Lead opportunity generated for workshop " + workshop.getBusinessName() + " with fee ₹" + currentFee,
                        "{\"feeSnapshot\": " + currentFee + "}"
                );
            }

            savedRequest.setStatus(ServiceRequestStatus.MATCHED);
            savedRequest = serviceRequestRepository.save(savedRequest);

            auditService.recordEvent(
                    MarketplaceEventType.WORKSHOPS_MATCHED,
                    savedRequest.getId(),
                    null,
                    null,
                    null,
                    "Matched " + matchedWorkshops.size() + " workshops for service request " + savedRequest.getRequestReference(),
                    "{\"matchedCount\": " + matchedWorkshops.size() + "}"
            );
        }

        return savedRequest;
    }

    public boolean isCancellable(ServiceRequest request) {
        if (request == null) return false;
        if (request.getStatus() == ServiceRequestStatus.CANCELLED || request.getStatus() == ServiceRequestStatus.COMPLETED) {
            return false;
        }
        if (request.getCurrentJob() != null && request.getCurrentJob().isPhysicalWorkStarted()) {
            return false;
        }
        return true;
    }

    @Transactional
    public ServiceRequestResponse cancelServiceRequest(User currentUser, Long requestId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + requestId));

        if (!request.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("You are not authorized to cancel this service request");
        }

        if (!isCancellable(request)) {
            String currentStage = request.getCurrentJob() != null
                    ? request.getCurrentJob().getStatus().name()
                    : request.getStatus().name();
            throw new IllegalStateException("Cannot cancel service request once vehicle intake has occurred or work is in progress. Current stage: " + currentStage);
        }

        request.setStatus(ServiceRequestStatus.CANCELLED);

        // Cancel linked booking
        if (request.getBooking() != null) {
            Booking booking = request.getBooking();
            if (booking.getStatus() != BookingStatus.CANCELLED && booking.getStatus() != BookingStatus.COMPLETED) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledAt(LocalDateTime.now());
                bookingRepository.save(booking);
            }
        }

        // Cancel linked job if any
        if (request.getCurrentJob() != null) {
            WorkshopJob job = request.getCurrentJob();
            job.setStatus(WorkshopJobStatus.CANCELLED);
            job.setCancelledAt(LocalDateTime.now());
            job.setCancellationReason("Customer cancelled service request");
        }

        // Cancel opportunities & refund winning workshop if paid
        List<LeadOpportunity> opportunities = leadOpportunityRepository.findByServiceRequestId(request.getId());
        for (LeadOpportunity opp : opportunities) {
            if (opp.getStatus() == OpportunityStatus.AVAILABLE
                    || opp.getStatus() == OpportunityStatus.VIEWED
                    || opp.getStatus() == OpportunityStatus.PAYMENT_PENDING) {
                opp.setStatus(OpportunityStatus.CANCELLED);
                leadOpportunityRepository.save(opp);
            } else if (opp.getStatus() == OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED
                    || opp.getStatus() == OpportunityStatus.ACCEPTED) {
                opp.setStatus(OpportunityStatus.CANCELLED);
                leadOpportunityRepository.save(opp);

                Optional<WorkshopPayment> paymentOpt = workshopPaymentRepository.findFirstByOpportunityIdAndPaymentStatusOrderByCreatedAtDesc(opp.getId(), PaymentStatus.SUCCESS);
                if (paymentOpt.isPresent()) {
                    WorkshopPayment payment = paymentOpt.get();
                    try {
                        refundService.createRefundRecord(payment, RefundReason.CUSTOMER_CANCELLED);
                    } catch (Exception ex) {
                        // Refund logged, avoid failing overall cancellation
                    }
                }
            }
        }

        ServiceRequest saved = serviceRequestRepository.save(request);

        auditService.recordEvent(
                MarketplaceEventType.REQUEST_CANCELLED,
                saved.getId(),
                null,
                request.getAssignedWorkshop() != null ? request.getAssignedWorkshop().getId() : null,
                currentUser.getId(),
                "Customer cancelled service request " + saved.getRequestReference(),
                null
        );

        notificationService.sendNotification(
                currentUser.getId(),
                "SERVICE_CANCELLED",
                "Your service request " + saved.getRequestReference() + " has been successfully cancelled.",
                Map.of("requestId", saved.getId(), "requestReference", saved.getRequestReference())
        );

        if (request.getAssignedWorkshop() != null && request.getAssignedWorkshop().getUser() != null) {
            notificationService.sendNotification(
                    request.getAssignedWorkshop().getUser().getId(),
                    "JOB_CANCELLED",
                    "Service request " + saved.getRequestReference() + " was cancelled by the customer before vehicle intake. Any paid acceptance fee has been refunded.",
                    Map.of("requestId", saved.getId(), "requestReference", saved.getRequestReference())
            );
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse getServiceRequest(User currentUser, Long requestId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + requestId));

        if (!request.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("You are not authorized to view this service request");
        }

        return mapToResponse(request);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> getCustomerRequests(User currentUser) {
        return serviceRequestRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> getAllRequestsForAdmin() {
        return serviceRequestRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ServiceRequestResponse mapToResponse(ServiceRequest request) {
        ServiceRequestResponse response = new ServiceRequestResponse();
        response.setId(request.getId());
        response.setRequestReference(request.getRequestReference());
        response.setUserId(request.getUser().getId());
        response.setVehicleId(request.getVehicle().getId());

        Vehicle v = request.getVehicle();
        response.setVehicleSummary(v.getMake() + " " + v.getModel() + " (" + v.getYear() + ") - "
                + v.getRegistrationNumber() + " [" + v.getFuelType() + "]");

        response.setCity(request.getCity());
        response.setAddress(request.getAddress());
        response.setPincode(request.getPincode());
        response.setLatitude(request.getLatitude());
        response.setLongitude(request.getLongitude());
        response.setPreferredDate(request.getPreferredDate());
        response.setPreferredTimeSlot(request.getPreferredTimeSlot());
        response.setCustomerNotes(request.getCustomerNotes());
        response.setStatus(request.getStatus());
        response.setTotalAmount(request.getTotalAmount());

        response.setBookingReference(request.getBookingReference());
        if (request.getBooking() != null) {
            response.setBookingId(request.getBooking().getId());
        }

        if (request.getAssignedWorkshop() != null) {
            response.setAssignedWorkshopId(request.getAssignedWorkshop().getId());
            response.setAssignedWorkshopName(request.getAssignedWorkshop().getBusinessName());
            response.setAssignedWorkshopPhone(request.getAssignedWorkshop().getPhone());
            String fullAddress = request.getAssignedWorkshop().getAddress();
            if (request.getAssignedWorkshop().getCity() != null) {
                fullAddress += ", " + request.getAssignedWorkshop().getCity();
            }
            response.setAssignedWorkshopAddress(fullAddress);
        }

        if (request.getCurrentJob() != null) {
            response.setJobStatus(request.getCurrentJob().getStatus().name());
        }

        response.setIsCancellable(isCancellable(request));

        List<ServiceRequestItemResponse> items = request.getItems().stream()
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
        response.setItems(items);

        response.setCreatedAt(request.getCreatedAt());
        response.setUpdatedAt(request.getUpdatedAt());
        return response;
    }
}

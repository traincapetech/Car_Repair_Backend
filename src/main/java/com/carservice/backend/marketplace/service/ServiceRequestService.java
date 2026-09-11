package com.carservice.backend.marketplace.service;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.dto.CreateServiceRequestRequest;
import com.carservice.backend.marketplace.dto.ServiceRequestItemResponse;
import com.carservice.backend.marketplace.dto.ServiceRequestResponse;
import com.carservice.backend.marketplace.entity.LeadOpportunity;
import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.ServiceRequestItem;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.MarketplaceEventType;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.repository.LeadOpportunityRepository;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.vehicle.entity.Vehicle;
import com.carservice.backend.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final LeadOpportunityRepository leadOpportunityRepository;
    private final MatchingEngineService matchingEngineService;
    private final PlatformConfigService platformConfigService;
    private final MarketplaceAuditService auditService;

    public ServiceRequestService(
            ServiceRequestRepository serviceRequestRepository,
            VehicleRepository vehicleRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            LeadOpportunityRepository leadOpportunityRepository,
            MatchingEngineService matchingEngineService,
            PlatformConfigService platformConfigService,
            MarketplaceAuditService auditService
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.leadOpportunityRepository = leadOpportunityRepository;
        this.matchingEngineService = matchingEngineService;
        this.platformConfigService = platformConfigService;
        this.auditService = auditService;
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

        auditService.recordEvent(
                MarketplaceEventType.REQUEST_CREATED,
                savedRequest.getId(),
                null,
                null,
                currentUser.getId(),
                "Customer created service request " + savedRequest.getRequestReference(),
                "{\"totalAmount\": " + savedRequest.getTotalAmount() + ", \"itemsCount\": " + services.size() + "}"
        );

        // 5. Match eligible workshops
        List<Workshop> matchedWorkshops = matchingEngineService.findEligibleWorkshops(savedRequest, Collections.emptySet());
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

        if (request.getAssignedWorkshop() != null) {
            response.setAssignedWorkshopId(request.getAssignedWorkshop().getId());
            response.setAssignedWorkshopName(request.getAssignedWorkshop().getBusinessName());
        }

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

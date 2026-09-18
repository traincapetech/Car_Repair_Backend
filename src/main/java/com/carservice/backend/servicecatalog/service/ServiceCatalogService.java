package com.carservice.backend.servicecatalog.service;

import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.booking.repository.BookingServiceRepository;
import com.carservice.backend.common.exception.ServiceCatalogAlreadyExistsException;
import com.carservice.backend.common.exception.ServiceCatalogNotFoundException;
import com.carservice.backend.marketplace.repository.ServiceRequestItemRepository;
import com.carservice.backend.marketplace.repository.WorkshopServiceRepository;
import com.carservice.backend.servicecatalog.dto.CreateServiceCatalogRequest;
import com.carservice.backend.servicecatalog.dto.ServiceCatalogResponse;
import com.carservice.backend.servicecatalog.dto.UpdateServiceCatalogRequest;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ServiceCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ServiceCatalogService.class);

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRequestItemRepository serviceRequestItemRepository;
    private final WorkshopServiceRepository workshopServiceRepository;

    public ServiceCatalogService(
            ServiceCatalogRepository serviceCatalogRepository,
            BookingServiceRepository bookingServiceRepository,
            BookingRepository bookingRepository,
            ServiceRequestItemRepository serviceRequestItemRepository,
            WorkshopServiceRepository workshopServiceRepository
    ) {
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.bookingServiceRepository = bookingServiceRepository;
        this.bookingRepository = bookingRepository;
        this.serviceRequestItemRepository = serviceRequestItemRepository;
        this.workshopServiceRepository = workshopServiceRepository;
    }

    @Transactional
    public ServiceCatalogResponse createService(CreateServiceCatalogRequest request) {
        String trimmedName = request.getName().trim();

        if (serviceCatalogRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ServiceCatalogAlreadyExistsException(
                    "Service with name '" + trimmedName + "' already exists"
            );
        }

        ServiceCatalog service = new ServiceCatalog();
        service.setName(trimmedName);
        service.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        service.setCategory(request.getCategory());
        service.setBasePrice(request.getBasePrice());
        if (request.getDiscountType() != null) {
            service.setDiscountType(request.getDiscountType());
        }
        if (request.getDiscountValue() != null) {
            service.setDiscountValue(request.getDiscountValue());
        }
        service.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        service.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        service.validateDiscount();

        ServiceCatalog saved = serviceCatalogRepository.save(service);
        log.info("Service catalog package created: ID {}, Name '{}', Category {}, Base Price {}, Final Price {}",
                saved.getId(), saved.getName(), saved.getCategory(), saved.getBasePrice(), saved.calculateFinalPrice());
        return ServiceCatalogResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> getAllServices() {
        return serviceCatalogRepository.findAllByOrderByNameAsc().stream()
                .map(ServiceCatalogResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> getActiveServices() {
        return serviceCatalogRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(ServiceCatalogResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> getAdminServices(
            String search,
            ServiceCategory category,
            String status,
            String sortBy,
            String sortDir
    ) {
        Boolean isActive = null;
        if ("ACTIVE".equalsIgnoreCase(status)) {
            isActive = true;
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            isActive = false;
        }

        String searchPattern = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        String sortField = "name";
        if ("basePrice".equalsIgnoreCase(sortBy) || "price".equalsIgnoreCase(sortBy)) {
            sortField = "basePrice";
        } else if ("createdAt".equalsIgnoreCase(sortBy) || "createdDate".equalsIgnoreCase(sortBy)) {
            sortField = "createdAt";
        } else if ("updatedAt".equalsIgnoreCase(sortBy)) {
            sortField = "updatedAt";
        } else if ("estimatedDurationMinutes".equalsIgnoreCase(sortBy) || "duration".equalsIgnoreCase(sortBy)) {
            sortField = "estimatedDurationMinutes";
        } else if ("name".equalsIgnoreCase(sortBy)) {
            sortField = "name";
        }

        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField);

        return serviceCatalogRepository.findWithFilters(category, isActive, searchPattern, sort).stream()
                .map(ServiceCatalogResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceCatalogResponse getServiceForUser(User user, Long id) {
        ServiceCatalog service;
        if (user != null && UserRole.ADMIN.equals(user.getRole())) {
            service = serviceCatalogRepository.findById(id)
                    .orElseThrow(() -> new ServiceCatalogNotFoundException("Service not found with id: " + id));
        } else {
            service = serviceCatalogRepository.findByIdAndIsActiveTrue(id)
                    .orElseThrow(() -> new ServiceCatalogNotFoundException("Service not found with id: " + id));
        }
        return ServiceCatalogResponse.fromEntity(service);
    }

    @Transactional
    public ServiceCatalogResponse updateService(Long id, UpdateServiceCatalogRequest request) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ServiceCatalogNotFoundException("Service not found with id: " + id));

        String trimmedName = request.getName().trim();
        if (!service.getName().equalsIgnoreCase(trimmedName)) {
            if (serviceCatalogRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
                throw new ServiceCatalogAlreadyExistsException(
                        "Service with name '" + trimmedName + "' already exists"
                );
            }
        }

        log.info("Updating service ID {}: oldPrice={}, oldDiscount={}:{}",
                id, service.getBasePrice(), service.getDiscountType(), service.getDiscountValue());

        service.setName(trimmedName);
        service.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        service.setCategory(request.getCategory());
        service.setBasePrice(request.getBasePrice());
        if (request.getDiscountType() != null) {
            service.setDiscountType(request.getDiscountType());
        }
        if (request.getDiscountValue() != null) {
            service.setDiscountValue(request.getDiscountValue());
        }
        service.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }
        service.validateDiscount();

        ServiceCatalog updated = serviceCatalogRepository.save(service);
        log.info("Updated service ID {}: newPrice={}, newDiscount={}:{}, finalPrice={}",
                id, updated.getBasePrice(), updated.getDiscountType(), updated.getDiscountValue(), updated.calculateFinalPrice());
        return ServiceCatalogResponse.fromEntity(updated);
    }

    @Transactional
    public ServiceCatalogResponse activateService(Long id) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ServiceCatalogNotFoundException("Service not found with id: " + id));

        service.setIsActive(true);
        ServiceCatalog updated = serviceCatalogRepository.save(service);
        log.info("Service ID {} ('{}') activated by admin.", id, service.getName());
        return ServiceCatalogResponse.fromEntity(updated);
    }

    @Transactional
    public ServiceCatalogResponse deactivateService(Long id) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ServiceCatalogNotFoundException("Service not found with id: " + id));

        service.setIsActive(false);
        ServiceCatalog updated = serviceCatalogRepository.save(service);
        log.info("Service ID {} ('{}') deactivated by admin.", id, service.getName());
        return ServiceCatalogResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteService(Long id) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ServiceCatalogNotFoundException("Service not found with id: " + id));

        boolean hasBookingLines = bookingServiceRepository.existsByServiceCatalogId(id);
        boolean hasBookings = bookingRepository.existsByServiceId(id);
        boolean hasServiceRequests = serviceRequestItemRepository.existsByServiceCatalogId(id);
        boolean hasWorkshopServices = workshopServiceRepository.existsByServiceCatalogId(id);

        if (hasBookingLines || hasBookings || hasServiceRequests || hasWorkshopServices) {
            log.warn("Cannot delete service ID {} ('{}') - referenced by historical bookings, line items, or service requests. Deactivate instead.",
                    id, service.getName());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete service '" + service.getName() + "' because it is referenced by historical bookings or service requests. Please deactivate the service instead."
            );
        }

        serviceCatalogRepository.delete(service);
        log.info("Service ID {} ('{}') permanently deleted by admin.", id, service.getName());
    }
}

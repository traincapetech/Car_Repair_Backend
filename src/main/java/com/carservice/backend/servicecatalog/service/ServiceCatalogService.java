package com.carservice.backend.servicecatalog.service;

import com.carservice.backend.common.exception.ServiceCatalogAlreadyExistsException;
import com.carservice.backend.common.exception.ServiceCatalogNotFoundException;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.servicecatalog.dto.CreateServiceCatalogRequest;
import com.carservice.backend.servicecatalog.dto.ServiceCatalogResponse;
import com.carservice.backend.servicecatalog.dto.UpdateServiceCatalogRequest;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceCatalogService {

    private final ServiceCatalogRepository serviceCatalogRepository;

    public ServiceCatalogService(ServiceCatalogRepository serviceCatalogRepository) {
        this.serviceCatalogRepository = serviceCatalogRepository;
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
        service.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        service.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        ServiceCatalog saved = serviceCatalogRepository.save(service);
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

        service.setName(trimmedName);
        service.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        service.setCategory(request.getCategory());
        service.setBasePrice(request.getBasePrice());
        service.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        ServiceCatalog updated = serviceCatalogRepository.save(service);
        return ServiceCatalogResponse.fromEntity(updated);
    }

    @Transactional
    public ServiceCatalogResponse activateService(Long id) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ServiceCatalogNotFoundException("Service not found with id: " + id));

        service.setIsActive(true);
        ServiceCatalog updated = serviceCatalogRepository.save(service);
        return ServiceCatalogResponse.fromEntity(updated);
    }

    @Transactional
    public ServiceCatalogResponse deactivateService(Long id) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ServiceCatalogNotFoundException("Service not found with id: " + id));

        service.setIsActive(false);
        ServiceCatalog updated = serviceCatalogRepository.save(service);
        return ServiceCatalogResponse.fromEntity(updated);
    }
}

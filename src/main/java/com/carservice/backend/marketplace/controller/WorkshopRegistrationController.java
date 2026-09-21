package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.WorkshopRegistrationRequest;
import com.carservice.backend.marketplace.dto.WorkshopRegistrationResponse;
import com.carservice.backend.marketplace.service.WorkshopRegistrationService;
import com.carservice.backend.servicecatalog.dto.ServiceCatalogResponse;
import com.carservice.backend.servicecatalog.service.ServiceCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WorkshopRegistrationController {

    private final WorkshopRegistrationService registrationService;
    private final ServiceCatalogService serviceCatalogService;

    public WorkshopRegistrationController(
            WorkshopRegistrationService registrationService,
            ServiceCatalogService serviceCatalogService
    ) {
        this.registrationService = registrationService;
        this.serviceCatalogService = serviceCatalogService;
    }

    @PostMapping({"/api/v1/workshops/register", "/api/v1/auth/register/workshop"})
    public ResponseEntity<ApiResponse<WorkshopRegistrationResponse>> registerWorkshop(
            @Valid @RequestBody WorkshopRegistrationRequest request
    ) {
        WorkshopRegistrationResponse response = registrationService.registerWorkshop(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Workshop registered successfully. Awaiting administrative review.", response));
    }

    @GetMapping({"/api/v1/workshops/services", "/api/v1/services/public"})
    public ResponseEntity<ApiResponse<List<ServiceCatalogResponse>>> getAvailableServices() {
        List<ServiceCatalogResponse> activeServices = serviceCatalogService.getActiveServices();
        return ResponseEntity.ok(
                ApiResponse.success("Available services fetched successfully", activeServices)
        );
    }
}

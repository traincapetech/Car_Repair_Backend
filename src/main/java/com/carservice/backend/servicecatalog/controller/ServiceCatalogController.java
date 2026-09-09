package com.carservice.backend.servicecatalog.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.servicecatalog.dto.CreateServiceCatalogRequest;
import com.carservice.backend.servicecatalog.dto.ServiceCatalogResponse;
import com.carservice.backend.servicecatalog.dto.UpdateServiceCatalogRequest;
import com.carservice.backend.servicecatalog.service.ServiceCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceCatalogResponse>>> getServices(
            Authentication authentication,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        User currentUser = (User) authentication.getPrincipal();

        List<ServiceCatalogResponse> responses;
        if (UserRole.ADMIN.equals(currentUser.getRole())) {
            if (Boolean.TRUE.equals(activeOnly)) {
                responses = serviceCatalogService.getActiveServices();
            } else {
                responses = serviceCatalogService.getAllServices();
            }
        } else {
            responses = serviceCatalogService.getActiveServices();
        }

        return ResponseEntity.ok(
                ApiResponse.success("Services fetched successfully", responses)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> getService(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        ServiceCatalogResponse response = serviceCatalogService.getServiceForUser(currentUser, id);

        return ResponseEntity.ok(
                ApiResponse.success("Service fetched successfully", response)
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> createService(
            @Valid @RequestBody CreateServiceCatalogRequest request
    ) {
        ServiceCatalogResponse response = serviceCatalogService.createService(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceCatalogRequest request
    ) {
        ServiceCatalogResponse response = serviceCatalogService.updateService(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Service updated successfully", response)
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> activateService(
            @PathVariable Long id
    ) {
        ServiceCatalogResponse response = serviceCatalogService.activateService(id);

        return ResponseEntity.ok(
                ApiResponse.success("Service activated successfully", response)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> deactivateService(
            @PathVariable Long id
    ) {
        ServiceCatalogResponse response = serviceCatalogService.deactivateService(id);

        return ResponseEntity.ok(
                ApiResponse.success("Service deactivated successfully", response)
        );
    }
}

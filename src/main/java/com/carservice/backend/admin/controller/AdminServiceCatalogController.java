package com.carservice.backend.admin.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.servicecatalog.dto.CreateServiceCatalogRequest;
import com.carservice.backend.servicecatalog.dto.ServiceCatalogResponse;
import com.carservice.backend.servicecatalog.dto.UpdateServiceCatalogRequest;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import com.carservice.backend.servicecatalog.service.ServiceCatalogService;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/services")
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    public AdminServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceCatalogResponse>>> getServices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false, defaultValue = "ASC") String direction
    ) {
        List<ServiceCatalogResponse> responses = serviceCatalogService.getAdminServices(
                search,
                category,
                status,
                sort,
                direction
        );

        return ResponseEntity.ok(
                ApiResponse.success("Admin services fetched successfully", responses)
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
                ApiResponse.success("Service retrieved successfully", response)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> createService(
            @Valid @RequestBody CreateServiceCatalogRequest request
    ) {
        ServiceCatalogResponse response = serviceCatalogService.createService(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service created successfully", response));
    }

    @PutMapping("/{id}")
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
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> activateService(
            @PathVariable Long id
    ) {
        ServiceCatalogResponse response = serviceCatalogService.activateService(id);

        return ResponseEntity.ok(
                ApiResponse.success("Service activated successfully", response)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> deactivateService(
            @PathVariable Long id
    ) {
        ServiceCatalogResponse response = serviceCatalogService.deactivateService(id);

        return ResponseEntity.ok(
                ApiResponse.success("Service deactivated successfully", response)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @PathVariable Long id
    ) {
        serviceCatalogService.deleteService(id);

        return ResponseEntity.ok(
                ApiResponse.success("Service deleted successfully", null)
        );
    }
}

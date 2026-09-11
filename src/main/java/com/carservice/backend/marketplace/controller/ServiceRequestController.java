package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.CreateServiceRequestRequest;
import com.carservice.backend.marketplace.dto.ServiceRequestResponse;
import com.carservice.backend.marketplace.service.ServiceRequestService;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-requests")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> createServiceRequest(
            Authentication authentication,
            @Valid @RequestBody CreateServiceRequestRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        ServiceRequestResponse response = serviceRequestService.createServiceRequest(currentUser, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service request submitted successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getMyServiceRequests(
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        List<ServiceRequestResponse> list = serviceRequestService.getCustomerRequests(currentUser);

        return ResponseEntity.ok(ApiResponse.success("Service requests fetched successfully", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> getServiceRequest(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        ServiceRequestResponse response = serviceRequestService.getServiceRequest(currentUser, id);

        return ResponseEntity.ok(ApiResponse.success("Service request fetched successfully", response));
    }
}

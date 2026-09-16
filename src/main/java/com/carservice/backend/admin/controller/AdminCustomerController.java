package com.carservice.backend.admin.controller;

import com.carservice.backend.admin.dto.*;
import com.carservice.backend.admin.service.AdminCustomerService;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(AdminCustomerService adminCustomerService) {
        this.adminCustomerService = adminCustomerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminCustomerListResponse>>> getCustomers(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "DESC") String direction
    ) {
        Page<AdminCustomerListResponse> customerPage = adminCustomerService.getCustomers(page, size, search, status, sort, direction);
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customerPage));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<AdminCustomerDetailResponse>> getCustomerDetail(
            @PathVariable Long customerId
    ) {
        AdminCustomerDetailResponse detail = adminCustomerService.getCustomerDetail(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer details retrieved successfully", detail));
    }

    @GetMapping("/{customerId}/vehicles")
    public ResponseEntity<ApiResponse<List<AdminCustomerVehicleResponse>>> getCustomerVehicles(
            @PathVariable Long customerId
    ) {
        List<AdminCustomerVehicleResponse> vehicles = adminCustomerService.getCustomerVehicles(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer vehicles retrieved successfully", vehicles));
    }

    @GetMapping("/{customerId}/bookings")
    public ResponseEntity<ApiResponse<Page<AdminCustomerBookingResponse>>> getCustomerBookings(
            @PathVariable Long customerId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "DESC") String direction
    ) {
        Page<AdminCustomerBookingResponse> bookings = adminCustomerService.getCustomerBookings(customerId, page, size, status, sort, direction);
        return ResponseEntity.ok(ApiResponse.success("Customer bookings retrieved successfully", bookings));
    }

    @GetMapping("/{customerId}/service-requests")
    public ResponseEntity<ApiResponse<Page<AdminCustomerServiceRequestResponse>>> getCustomerServiceRequests(
            @PathVariable Long customerId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) ServiceRequestStatus status,
            @RequestParam(required = false, defaultValue = "DESC") String direction
    ) {
        Page<AdminCustomerServiceRequestResponse> requests = adminCustomerService.getCustomerServiceRequests(customerId, page, size, status, direction);
        return ResponseEntity.ok(ApiResponse.success("Customer service requests retrieved successfully", requests));
    }

    @PutMapping("/{customerId}/status")
    public ResponseEntity<ApiResponse<AdminCustomerDetailResponse>> updateCustomerStatus(
            @PathVariable Long customerId,
            @Valid @RequestBody UpdateCustomerStatusRequest request,
            Authentication authentication
    ) {
        User adminUser = (authentication != null && authentication.getPrincipal() instanceof User)
                ? (User) authentication.getPrincipal()
                : null;

        AdminCustomerDetailResponse updated = adminCustomerService.updateCustomerStatus(customerId, request, adminUser);
        String action = Boolean.TRUE.equals(request.getIsActive()) ? "reactivated" : "deactivated";
        return ResponseEntity.ok(ApiResponse.success("Customer account " + action + " successfully", updated));
    }
}

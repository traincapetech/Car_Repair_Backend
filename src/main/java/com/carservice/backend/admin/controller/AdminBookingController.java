package com.carservice.backend.admin.controller;

import com.carservice.backend.admin.dto.AdminBookingDetailResponse;
import com.carservice.backend.admin.dto.AdminBookingListResponse;
import com.carservice.backend.admin.dto.AdminBookingSummaryResponse;
import com.carservice.backend.admin.dto.AdminCancelBookingRequest;
import com.carservice.backend.admin.service.AdminBookingService;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    public AdminBookingController(AdminBookingService adminBookingService) {
        this.adminBookingService = adminBookingService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminBookingSummaryResponse>> getBookingSummary() {
        AdminBookingSummaryResponse summary = adminBookingService.getBookingSummary();
        return ResponseEntity.ok(ApiResponse.success("Booking summary metrics retrieved successfully", summary));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminBookingListResponse>>> getBookings(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long workshopId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "DESC") String direction
    ) {
        BookingStatus bookingStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                bookingStatus = BookingStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Sort.Direction sortDir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sort == null || sort.trim().isEmpty()) ? "createdAt" : sort.trim();
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(sortDir, sortProperty));

        Page<AdminBookingListResponse> result = adminBookingService.getBookings(
                search,
                bookingStatus,
                workshopId,
                customerId,
                startDate,
                endDate,
                pageRequest
        );
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminBookingDetailResponse>> getBookingDetail(
            @PathVariable Long id
    ) {
        AdminBookingDetailResponse response = adminBookingService.getBookingDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Booking 360 detail retrieved successfully", response));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AdminBookingDetailResponse>> cancelBooking(
            @AuthenticationPrincipal User adminUser,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminCancelBookingRequest request
    ) {
        AdminBookingDetailResponse response = adminBookingService.cancelBooking(adminUser, id, request);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully by administration", response));
    }
}

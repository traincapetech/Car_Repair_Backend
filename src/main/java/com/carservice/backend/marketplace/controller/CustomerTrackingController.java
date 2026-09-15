package com.carservice.backend.marketplace.controller;

import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.CustomerJobTrackingResponse;
import com.carservice.backend.marketplace.service.WorkshopJobService;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/tracking")
@PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
public class CustomerTrackingController {

    private final WorkshopJobService workshopJobService;
    private final BookingRepository bookingRepository;

    public CustomerTrackingController(WorkshopJobService workshopJobService, BookingRepository bookingRepository) {
        this.workshopJobService = workshopJobService;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CustomerJobTrackingResponse>>> getActiveJobs(
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        List<CustomerJobTrackingResponse> activeList = workshopJobService.getActiveJobsForCustomer(currentUser);

        return ResponseEntity.ok(ApiResponse.success("Active repair jobs retrieved successfully", activeList));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerJobTrackingResponse>> getTrackingById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        java.util.Optional<Booking> bookingOpt = bookingRepository.findById(id);
        if (bookingOpt.isPresent()) {
            return getTrackingForBooking(authentication, id);
        }
        return getTrackingForRequest(authentication, id);
    }

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<CustomerJobTrackingResponse>> getTrackingForRequest(
            Authentication authentication,
            @PathVariable Long requestId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        CustomerJobTrackingResponse tracking = workshopJobService.getCustomerJobTracking(currentUser, requestId);

        return ResponseEntity.ok(ApiResponse.success("Job tracking details retrieved successfully", tracking));
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<CustomerJobTrackingResponse>> getTrackingForBooking(
            Authentication authentication,
            @PathVariable Long bookingId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("You are not authorized to track this booking");
        }

        if (booking.getServiceRequest() != null) {
            CustomerJobTrackingResponse tracking = workshopJobService.getCustomerJobTracking(currentUser, booking.getServiceRequest().getId());
            return ResponseEntity.ok(ApiResponse.success("Job tracking details retrieved successfully", tracking));
        }

        // Return empty or baseline tracking response if no ServiceRequest linked
        CustomerJobTrackingResponse fallback = new CustomerJobTrackingResponse();
        fallback.setBookingId(booking.getId());
        fallback.setBookingReference(booking.getBookingReference());
        fallback.setStatus(booking.getStatus().name());
        fallback.setCurrentStep(1);
        fallback.setFriendlyStatusTitle("Booking Scheduled");
        fallback.setFriendlyStatusDescription("Your service booking has been placed.");
        fallback.setCancellable(true);

        return ResponseEntity.ok(ApiResponse.success("Job tracking details retrieved successfully", fallback));
    }
}

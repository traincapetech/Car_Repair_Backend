package com.carservice.backend.booking.controller;

import com.carservice.backend.booking.dto.BookingResponse;
import com.carservice.backend.booking.dto.CreateBookingRequest;
import com.carservice.backend.booking.service.BookingService;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            Authentication authentication,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        BookingResponse response = bookingService.createBooking(currentUser, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        List<BookingResponse> responses = bookingService.getMyBookings(currentUser);

        return ResponseEntity.ok(
                ApiResponse.success("Bookings fetched successfully", responses)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getMyBooking(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        BookingResponse response = bookingService.getMyBooking(currentUser, id);

        return ResponseEntity.ok(
                ApiResponse.success("Booking fetched successfully", response)
        );
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        BookingResponse response = bookingService.cancelBooking(currentUser, id);

        return ResponseEntity.ok(
                ApiResponse.success("Booking cancelled successfully", response)
        );
    }
}

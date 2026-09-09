package com.carservice.backend.booking.service;

import com.carservice.backend.booking.dto.BookingResponse;
import com.carservice.backend.booking.dto.CreateBookingRequest;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.common.exception.BookingConflictException;
import com.carservice.backend.common.exception.BookingNotFoundException;
import com.carservice.backend.common.exception.InvalidBookingStateException;
import com.carservice.backend.common.exception.ServiceCatalogNotFoundException;
import com.carservice.backend.common.exception.VehicleNotFoundException;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.vehicle.entity.Vehicle;
import com.carservice.backend.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;

    public BookingService(
            BookingRepository bookingRepository,
            VehicleRepository vehicleRepository,
            ServiceCatalogRepository serviceCatalogRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
    }

    @Transactional
    public BookingResponse createBooking(User user, CreateBookingRequest request) {
        LocalDateTime bookingDateTime = LocalDateTime.of(request.getBookingDate(), request.getBookingTime());
        if (bookingDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking date and time cannot be in the past");
        }

        Vehicle vehicle = vehicleRepository.findByIdAndUserId(request.getVehicleId(), user.getId())
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        ServiceCatalog service = serviceCatalogRepository.findByIdAndIsActiveTrue(request.getServiceId())
                .orElseThrow(() -> new ServiceCatalogNotFoundException("Service not found with id: " + request.getServiceId()));

        boolean hasConflict = bookingRepository.existsByUserIdAndVehicleIdAndServiceIdAndBookingDateAndBookingTimeAndStatusNot(
                user.getId(),
                vehicle.getId(),
                service.getId(),
                request.getBookingDate(),
                request.getBookingTime(),
                BookingStatus.CANCELLED
        );

        if (hasConflict) {
            throw new BookingConflictException(
                    "You already have an active booking for this vehicle and service at the requested date and time"
            );
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setService(service);
        booking.setBookingDate(request.getBookingDate());
        booking.setBookingTime(request.getBookingTime());
        booking.setStatus(BookingStatus.PENDING);
        booking.setCustomerNotes(request.getCustomerNotes() != null ? request.getCustomerNotes().trim() : null);
        booking.setEstimatedPrice(service.getBasePrice());

        Booking saved = bookingRepository.save(booking);
        return BookingResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(User user) {
        List<Booking> bookings = bookingRepository.findAllByUserIdWithDetailsOrderByCreatedAtDesc(user.getId());
        return bookings.stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getMyBooking(User user, Long id) {
        Booking booking = bookingRepository.findByIdAndUserIdWithDetails(id, user.getId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        return BookingResponse.fromEntity(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(User user, Long id) {
        Booking booking = bookingRepository.findByIdAndUserIdWithDetails(id, user.getId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException(
                    "Booking cannot be cancelled in its current status: " + booking.getStatus()
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);
        return BookingResponse.fromEntity(updated);
    }
}

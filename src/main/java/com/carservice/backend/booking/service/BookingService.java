package com.carservice.backend.booking.service;

import com.carservice.backend.booking.dto.BookingResponse;
import com.carservice.backend.booking.dto.CreateBookingRequest;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.enums.BookingTimeSlot;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.common.exception.BookingConflictException;
import com.carservice.backend.common.exception.BookingNotFoundException;
import com.carservice.backend.common.exception.InvalidBookingException;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        LocalTime bookingTime = request.getBookingTime();
        String timeSlot = request.getTimeSlot();

        if (timeSlot != null && !timeSlot.trim().isEmpty()) {
            String trimmedSlot = timeSlot.trim();
            if (!BookingTimeSlot.isValid(trimmedSlot)) {
                throw new InvalidBookingException(
                        "Invalid time slot: " + trimmedSlot + ". Supported slots: " + BookingTimeSlot.getAllSlots()
                );
            }
            timeSlot = trimmedSlot;
            if (bookingTime == null) {
                bookingTime = BookingTimeSlot.fromSlot(trimmedSlot)
                        .orElse(BookingTimeSlot.SLOT_10_11)
                        .getStartTime();
            }
        } else if (bookingTime != null) {
            timeSlot = BookingTimeSlot.fromBookingTime(bookingTime).getSlot();
        } else {
            throw new InvalidBookingException("Booking time or time slot is required");
        }

        LocalDateTime bookingDateTime = LocalDateTime.of(request.getBookingDate(), bookingTime);
        if (bookingDateTime.isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Booking date and time cannot be in the past");
        }

        Vehicle vehicle = vehicleRepository.findByIdAndUserId(request.getVehicleId(), user.getId())
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        List<Long> requestedServiceIds = request.getEffectiveServiceIds();
        if (requestedServiceIds == null || requestedServiceIds.isEmpty()) {
            throw new InvalidBookingException("At least one service must be selected");
        }
        for (Long sId : requestedServiceIds) {
            if (sId == null || sId <= 0) {
                throw new InvalidBookingException("Invalid service ID: " + sId);
            }
        }

        List<Long> distinctServiceIds = requestedServiceIds.stream().distinct().toList();
        List<ServiceCatalog> services = serviceCatalogRepository.findAllById(distinctServiceIds);

        if (services.size() != distinctServiceIds.size()) {
            Set<Long> foundIds = services.stream().map(ServiceCatalog::getId).collect(Collectors.toSet());
            Long missingId = distinctServiceIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new ServiceCatalogNotFoundException("Service not found with id: " + missingId);
        }

        for (ServiceCatalog s : services) {
            if (!Boolean.TRUE.equals(s.getIsActive())) {
                throw new ServiceCatalogNotFoundException("Service not found with id: " + s.getId());
            }
        }

        Map<Long, ServiceCatalog> serviceMap = services.stream()
                .collect(Collectors.toMap(ServiceCatalog::getId, s -> s));
        List<ServiceCatalog> orderedServices = distinctServiceIds.stream()
                .map(serviceMap::get)
                .toList();

        boolean hasConflict = bookingRepository.existsActiveConflict(
                vehicle.getId(),
                request.getBookingDate(),
                timeSlot,
                bookingTime
        );

        if (hasConflict) {
            throw new BookingConflictException(
                    "You already have an active booking for this vehicle and service at the requested date and time"
            );
        }

        String bookingReference = Booking.generateBookingReference(request.getBookingDate());
        while (bookingRepository.existsByBookingReference(bookingReference)) {
            bookingReference = Booking.generateBookingReference(request.getBookingDate());
        }

        Booking booking = new Booking();
        booking.setBookingReference(bookingReference);
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setBookingDate(request.getBookingDate());
        booking.setBookingTime(bookingTime);
        booking.setTimeSlot(timeSlot);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCustomerNotes(request.getCustomerNotes() != null ? request.getCustomerNotes().trim() : null);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ServiceCatalog s : orderedServices) {
            BigDecimal finalPrice = s.calculateFinalPrice();
            com.carservice.backend.booking.entity.BookingService lineItem =
                    new com.carservice.backend.booking.entity.BookingService(
                            booking,
                            s,
                            s.getName(),
                            s.getBasePrice(),
                            s.getDiscountType() != null ? s.getDiscountType() : com.carservice.backend.servicecatalog.enums.DiscountType.NO_DISCOUNT,
                            s.getDiscountValue() != null ? s.getDiscountValue() : BigDecimal.ZERO,
                            finalPrice
                    );
            booking.addBookingService(lineItem);
            totalAmount = totalAmount.add(finalPrice);
        }

        booking.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));

        // Legacy compatibility: primary service snapshot
        ServiceCatalog primaryService = orderedServices.get(0);
        booking.setService(primaryService);
        booking.setServiceNameSnapshot(primaryService.getName());
        booking.setServicePriceSnapshot(primaryService.getBasePrice());
        booking.setEstimatedPrice(totalAmount.setScale(2, RoundingMode.HALF_UP));

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

    @Transactional(readOnly = true)
    public BookingResponse getMyBookingByReference(User user, String reference) {
        Booking booking = bookingRepository.findByBookingReferenceAndUserId(reference, user.getId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with reference: " + reference));

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
        booking.setCancelledAt(LocalDateTime.now());
        Booking updated = bookingRepository.save(booking);
        return BookingResponse.fromEntity(updated);
    }
}

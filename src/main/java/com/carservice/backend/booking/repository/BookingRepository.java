package com.carservice.backend.booking.repository;

import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b JOIN FETCH b.vehicle JOIN FETCH b.service WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Booking> findAllByUserIdWithDetailsOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.vehicle JOIN FETCH b.service WHERE b.id = :id AND b.user.id = :userId")
    Optional<Booking> findByIdAndUserIdWithDetails(@Param("id") Long id, @Param("userId") Long userId);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    List<Booking> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndVehicleIdAndServiceIdAndBookingDateAndBookingTimeAndStatusNot(
            Long userId,
            Long vehicleId,
            Long serviceId,
            LocalDate bookingDate,
            LocalTime bookingTime,
            BookingStatus status
    );
}

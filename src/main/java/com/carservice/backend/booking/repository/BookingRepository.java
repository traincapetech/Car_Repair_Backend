package com.carservice.backend.booking.repository;

import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {


    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.vehicle v
        LEFT JOIN FETCH b.bookingServices bs
        LEFT JOIN FETCH bs.serviceCatalog sc
        LEFT JOIN FETCH b.service s
        LEFT JOIN FETCH b.serviceRequest sr
        LEFT JOIN FETCH sr.assignedWorkshop aw
        LEFT JOIN FETCH sr.currentJob cj
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findAllByUserIdWithDetailsOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.vehicle v
        LEFT JOIN FETCH b.bookingServices bs
        LEFT JOIN FETCH bs.serviceCatalog sc
        LEFT JOIN FETCH b.service s
        LEFT JOIN FETCH b.serviceRequest sr
        LEFT JOIN FETCH sr.assignedWorkshop aw
        LEFT JOIN FETCH sr.currentJob cj
        WHERE b.id = :id AND b.user.id = :userId
    """)
    Optional<Booking> findByIdAndUserIdWithDetails(@Param("id") Long id, @Param("userId") Long userId);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.vehicle v
        LEFT JOIN FETCH b.bookingServices bs
        LEFT JOIN FETCH bs.serviceCatalog sc
        LEFT JOIN FETCH b.service s
        LEFT JOIN FETCH b.serviceRequest sr
        LEFT JOIN FETCH sr.assignedWorkshop aw
        LEFT JOIN FETCH sr.currentJob cj
        WHERE b.bookingReference = :bookingReference AND b.user.id = :userId
    """)
    Optional<Booking> findByBookingReferenceAndUserId(@Param("bookingReference") String bookingReference, @Param("userId") Long userId);

    boolean existsByBookingReference(String bookingReference);

    List<Booking> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndVehicleIdAndServiceIdAndBookingDateAndBookingTimeAndStatusNot(
            Long userId,
            Long vehicleId,
            Long serviceId,
            LocalDate bookingDate,
            LocalTime bookingTime,
            BookingStatus status
    );

    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.vehicle.id = :vehicleId
          AND b.bookingDate = :bookingDate
          AND b.status != com.carservice.backend.booking.enums.BookingStatus.CANCELLED
          AND (
            (:timeSlot IS NOT NULL AND b.timeSlot = :timeSlot)
            OR
            (:bookingTime IS NOT NULL AND b.bookingTime = :bookingTime)
          )
    """)
    boolean existsActiveConflict(
            @Param("vehicleId") Long vehicleId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("timeSlot") String timeSlot,
            @Param("bookingTime") LocalTime bookingTime
    );

    @Query(value = "SELECT b FROM Booking b WHERE b.user.id = :userId AND (:status IS NULL OR b.status = :status)",
           countQuery = "SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND (:status IS NULL OR b.status = :status)")
    @EntityGraph(attributePaths = {"vehicle", "service"})
    Page<Booking> findByUserIdAndOptionalStatus(@Param("userId") Long userId, @Param("status") BookingStatus status, Pageable pageable);

    @Query("SELECT b.user.id, COUNT(b) FROM Booking b WHERE b.user.id IN :userIds GROUP BY b.user.id")
    List<Object[]> countBookingsByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("SELECT b.status, COUNT(b) FROM Booking b WHERE b.user.id = :userId GROUP BY b.status")
    List<Object[]> countBookingsByStatusForUser(@Param("userId") Long userId);

    @Query(value = """
        SELECT b FROM Booking b
        JOIN FETCH b.vehicle v
        LEFT JOIN FETCH b.service s
        LEFT JOIN FETCH b.serviceRequest sr
        WHERE sr.assignedWorkshop.id = :workshopId
          AND (:status IS NULL OR b.status = :status)
        ORDER BY b.createdAt DESC
    """,
    countQuery = """
        SELECT COUNT(b) FROM Booking b
        JOIN b.serviceRequest sr
        WHERE sr.assignedWorkshop.id = :workshopId
          AND (:status IS NULL OR b.status = :status)
    """)
    Page<Booking> findBookingsByWorkshopIdAndOptionalStatus(
            @Param("workshopId") Long workshopId,
            @Param("status") BookingStatus status,
            Pageable pageable
    );

    boolean existsByServiceId(Long serviceId);
}


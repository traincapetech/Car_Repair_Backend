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

    long countByUserId(Long userId);

    long countByStatus(BookingStatus status);

    long countByStatusIn(Collection<BookingStatus> statuses);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0.00) FROM Booking b WHERE b.status != com.carservice.backend.booking.enums.BookingStatus.CANCELLED")
    java.math.BigDecimal sumTotalAmountNonCancelled();

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user u
        JOIN FETCH b.vehicle v
        LEFT JOIN FETCH b.bookingServices bs
        LEFT JOIN FETCH bs.serviceCatalog sc
        LEFT JOIN FETCH b.service s
        LEFT JOIN FETCH b.serviceRequest sr
        LEFT JOIN FETCH sr.assignedWorkshop aw
        LEFT JOIN FETCH sr.currentJob cj
        WHERE b.id = :id
    """)
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    @Query(value = """
        SELECT b FROM Booking b
        JOIN FETCH b.user u
        JOIN FETCH b.vehicle v
        LEFT JOIN FETCH b.service s
        LEFT JOIN FETCH b.serviceRequest sr
        LEFT JOIN FETCH sr.assignedWorkshop w
        WHERE (:search IS NULL OR :search = '' OR
               LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.make) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
               (b.serviceNameSnapshot IS NOT NULL AND LOWER(b.serviceNameSnapshot) LIKE LOWER(CONCAT('%', :search, '%'))) OR
               (w IS NOT NULL AND LOWER(w.businessName) LIKE LOWER(CONCAT('%', :search, '%'))))
          AND (:status IS NULL OR b.status = :status)
          AND (:workshopId IS NULL OR (sr.assignedWorkshop IS NOT NULL AND sr.assignedWorkshop.id = :workshopId))
          AND (:customerId IS NULL OR b.user.id = :customerId)
          AND (:startDate IS NULL OR b.createdAt >= :startDate)
          AND (:endDate IS NULL OR b.createdAt <= :endDate)
    """,
    countQuery = """
        SELECT COUNT(b) FROM Booking b
        LEFT JOIN b.user u
        LEFT JOIN b.vehicle v
        LEFT JOIN b.serviceRequest sr
        LEFT JOIN sr.assignedWorkshop w
        WHERE (:search IS NULL OR :search = '' OR
               LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.make) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
               (b.serviceNameSnapshot IS NOT NULL AND LOWER(b.serviceNameSnapshot) LIKE LOWER(CONCAT('%', :search, '%'))) OR
               (w IS NOT NULL AND LOWER(w.businessName) LIKE LOWER(CONCAT('%', :search, '%'))))
          AND (:status IS NULL OR b.status = :status)
          AND (:workshopId IS NULL OR (sr.assignedWorkshop IS NOT NULL AND sr.assignedWorkshop.id = :workshopId))
          AND (:customerId IS NULL OR b.user.id = :customerId)
          AND (:startDate IS NULL OR b.createdAt >= :startDate)
          AND (:endDate IS NULL OR b.createdAt <= :endDate)
    """)
    Page<Booking> findBookingsWithFilter(
            @Param("search") String search,
            @Param("status") BookingStatus status,
            @Param("workshopId") Long workshopId,
            @Param("customerId") Long customerId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(b) FROM Booking b LEFT JOIN b.serviceRequest sr WHERE (:from IS NULL OR b.createdAt >= :from) AND (:to IS NULL OR b.createdAt <= :to) AND (:status IS NULL OR b.status = :status) AND (:workshopId IS NULL OR (sr.assignedWorkshop IS NOT NULL AND sr.assignedWorkshop.id = :workshopId))")
    long countBookingsFiltered(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to, @Param("status") BookingStatus status, @Param("workshopId") Long workshopId);

    @Query("SELECT b.status, COUNT(b) FROM Booking b LEFT JOIN b.serviceRequest sr WHERE (:from IS NULL OR b.createdAt >= :from) AND (:to IS NULL OR b.createdAt <= :to) AND (:workshopId IS NULL OR (sr.assignedWorkshop IS NOT NULL AND sr.assignedWorkshop.id = :workshopId)) GROUP BY b.status")
    java.util.List<Object[]> countBookingsGroupedByStatus(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to, @Param("workshopId") Long workshopId);

    @Query(value = """
        SELECT DATE(b.created_at) as b_date, COUNT(*) as cnt, COALESCE(SUM(b.total_amount), 0) as total_amt
        FROM bookings b
        LEFT JOIN service_requests sr ON b.service_request_id = sr.id
        WHERE (:from IS NULL OR b.created_at >= :from)
          AND (:to IS NULL OR b.created_at <= :to)
          AND (:workshopId IS NULL OR sr.assigned_workshop_id = :workshopId)
        GROUP BY DATE(b.created_at)
        ORDER BY b_date ASC
    """, nativeQuery = true)
    java.util.List<Object[]> countBookingsByDayNative(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to, @Param("workshopId") Long workshopId);
}


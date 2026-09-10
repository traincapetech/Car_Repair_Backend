package com.carservice.backend.booking.repository;

import com.carservice.backend.booking.entity.BookingService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingServiceRepository extends JpaRepository<BookingService, Long> {

    List<BookingService> findByBookingId(Long bookingId);

    boolean existsByBookingIdAndServiceCatalogId(Long bookingId, Long serviceCatalogId);
}

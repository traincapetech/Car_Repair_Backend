package com.carservice.backend.booking.repository;

import com.carservice.backend.booking.entity.BookingService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingServiceRepository extends JpaRepository<BookingService, Long> {

    List<BookingService> findByBookingId(Long bookingId);

    boolean existsByBookingIdAndServiceCatalogId(Long bookingId, Long serviceCatalogId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT bs.serviceNameSnapshot, COUNT(bs), COALESCE(SUM(bs.finalPriceSnapshot), 0)
        FROM BookingService bs
        GROUP BY bs.serviceNameSnapshot
        ORDER BY COUNT(bs) DESC
    """)
    List<Object[]> findMostBookedServices(org.springframework.data.domain.Pageable pageable);

    boolean existsByServiceCatalogId(Long serviceCatalogId);
}

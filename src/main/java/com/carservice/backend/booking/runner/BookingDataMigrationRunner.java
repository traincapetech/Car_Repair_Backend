package com.carservice.backend.booking.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class BookingDataMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookingDataMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public BookingDataMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1. Backfill service_catalog discount fields if null
            int catalogUpdated = jdbcTemplate.update(
                    "UPDATE service_catalog SET discount_type = 'NO_DISCOUNT', discount_value = 0.00 WHERE discount_type IS NULL"
            );
            if (catalogUpdated > 0) {
                log.info("Backfilled discount fields for {} service_catalog records.", catalogUpdated);
            }

            // 2. Backfill total_amount on bookings if null
            int totalAmountUpdated = jdbcTemplate.update(
                    "UPDATE bookings SET total_amount = COALESCE(service_price_snapshot, estimated_price, 0.00) WHERE total_amount IS NULL"
            );
            if (totalAmountUpdated > 0) {
                log.info("Backfilled total_amount for {} historical bookings.", totalAmountUpdated);
            }

            // 3. Migrate historical single-service bookings into booking_services line items
            int lineItemsInserted = jdbcTemplate.update("""
                INSERT INTO booking_services (
                    booking_id,
                    service_id,
                    service_name_snapshot,
                    base_price_snapshot,
                    discount_type_snapshot,
                    discount_value_snapshot,
                    final_price_snapshot,
                    created_at
                )
                SELECT
                    b.id,
                    b.service_id,
                    COALESCE(b.service_name_snapshot, s.name, 'Service'),
                    COALESCE(b.service_price_snapshot, b.estimated_price, s.base_price, 0.00),
                    'NO_DISCOUNT',
                    0.00,
                    COALESCE(b.service_price_snapshot, b.estimated_price, s.base_price, 0.00),
                    b.created_at
                FROM bookings b
                LEFT JOIN service_catalog s ON b.service_id = s.id
                WHERE b.service_id IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM booking_services bs WHERE bs.booking_id = b.id
                  )
            """);

            if (lineItemsInserted > 0) {
                log.info("Migrated {} historical single-service bookings into booking_services line items.", lineItemsInserted);
            }
        } catch (Exception e) {
            log.warn("BookingDataMigrationRunner notice: {}", e.getMessage());
        }
    }
}

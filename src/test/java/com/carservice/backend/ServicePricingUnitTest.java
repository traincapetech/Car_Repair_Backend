package com.carservice.backend;

import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.enums.DiscountType;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

public class ServicePricingUnitTest {

    @Test
    @DisplayName("1. No discount calculates final price equal to base price")
    void testNoDiscount() {
        ServiceCatalog service = new ServiceCatalog(
                "Oil Change",
                "Standard synthetic oil replacement",
                ServiceCategory.GENERAL_SERVICE,
                new BigDecimal("1499.00"),
                DiscountType.NO_DISCOUNT,
                BigDecimal.ZERO,
                60,
                true
        );

        assertEquals(new BigDecimal("1499.00"), service.calculateFinalPrice());
    }

    @Test
    @DisplayName("2. Percentage discount calculates final price correctly with HALF_UP rounding")
    void testPercentageDiscount() {
        // 1499.00 - 10% = 1499.00 - 149.90 = 1349.10
        ServiceCatalog service = new ServiceCatalog(
                "AC Cooling Service",
                "Complete AC service",
                ServiceCategory.AC_SERVICE,
                new BigDecimal("1499.00"),
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                60,
                true
        );

        assertEquals(new BigDecimal("1349.10"), service.calculateFinalPrice());

        // 1999.00 - 15% = 1999.00 - 299.85 = 1699.15
        service.setBasePrice(new BigDecimal("1999.00"));
        service.setDiscountValue(new BigDecimal("15.00"));
        assertEquals(new BigDecimal("1699.15"), service.calculateFinalPrice());
    }

    @Test
    @DisplayName("3. 100% discount calculates final price of zero")
    void testHundredPercentDiscount() {
        ServiceCatalog service = new ServiceCatalog(
                "Free Diagnostic",
                "Introductory free diagnostic",
                ServiceCategory.DIAGNOSTICS,
                new BigDecimal("500.00"),
                DiscountType.PERCENTAGE,
                new BigDecimal("100.00"),
                30,
                true
        );

        assertEquals(new BigDecimal("0.00"), service.calculateFinalPrice());
    }

    @Test
    @DisplayName("4. Fixed amount discount calculates final price correctly")
    void testFixedAmountDiscount() {
        // 1499.00 - 200.00 = 1299.00
        ServiceCatalog service = new ServiceCatalog(
                "Wheel Alignment",
                "Laser alignment",
                ServiceCategory.WHEEL_ALIGNMENT,
                new BigDecimal("1499.00"),
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("200.00"),
                45,
                true
        );

        assertEquals(new BigDecimal("1299.00"), service.calculateFinalPrice());
    }

    @Test
    @DisplayName("5. Fixed amount discount equal to base price calculates final price of zero")
    void testFixedDiscountEqualToBasePrice() {
        ServiceCatalog service = new ServiceCatalog(
                "Free Inspection",
                "Free brake inspection",
                ServiceCategory.BRAKE_SERVICE,
                new BigDecimal("299.00"),
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("299.00"),
                20,
                true
        );

        assertEquals(new BigDecimal("0.00"), service.calculateFinalPrice());
    }

    @Test
    @DisplayName("6. Validation rejects negative base price")
    void testValidation_NegativeBasePrice() {
        ServiceCatalog service = new ServiceCatalog();
        service.setBasePrice(new BigDecimal("-100.00"));

        assertThrows(IllegalArgumentException.class, service::validateDiscount);
    }

    @Test
    @DisplayName("7. Validation rejects negative discount value")
    void testValidation_NegativeDiscountValue() {
        ServiceCatalog service = new ServiceCatalog();
        service.setBasePrice(new BigDecimal("1000.00"));
        service.setDiscountType(DiscountType.PERCENTAGE);
        service.setDiscountValue(new BigDecimal("-5.00"));

        assertThrows(IllegalArgumentException.class, service::validateDiscount);
    }

    @Test
    @DisplayName("8. Validation rejects percentage discount greater than 100%")
    void testValidation_PercentageGreaterThan100() {
        ServiceCatalog service = new ServiceCatalog();
        service.setBasePrice(new BigDecimal("1000.00"));
        service.setDiscountType(DiscountType.PERCENTAGE);
        service.setDiscountValue(new BigDecimal("105.00"));

        assertThrows(IllegalArgumentException.class, service::validateDiscount);
    }

    @Test
    @DisplayName("9. Validation rejects fixed discount greater than base price")
    void testValidation_FixedDiscountGreaterThanBasePrice() {
        ServiceCatalog service = new ServiceCatalog();
        service.setBasePrice(new BigDecimal("1000.00"));
        service.setDiscountType(DiscountType.FIXED_AMOUNT);
        service.setDiscountValue(new BigDecimal("1200.00"));

        assertThrows(IllegalArgumentException.class, service::validateDiscount);
    }
}

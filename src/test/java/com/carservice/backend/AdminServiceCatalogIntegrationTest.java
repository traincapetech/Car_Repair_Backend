package com.carservice.backend;

import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.booking.repository.BookingServiceRepository;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.enums.DiscountType;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import com.carservice.backend.vehicle.entity.Vehicle;
import com.carservice.backend.vehicle.enums.FuelType;
import com.carservice.backend.vehicle.enums.Transmission;
import com.carservice.backend.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class AdminServiceCatalogIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingServiceRepository bookingServiceRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User customerUser;
    private User partnerUser;

    private String adminToken;
    private String customerToken;
    private String partnerToken;

    private Vehicle customerVehicle;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Admin User
        adminUser = new User();
        adminUser.setName("Admin Tester");
        adminUser.setEmail("admin." + suffix + "@carservice.com");
        adminUser.setPhone("9" + (System.currentTimeMillis() % 1000000000L));
        adminUser.setPassword(passwordEncoder.encode("AdminPass@123"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);

        // Customer User
        customerUser = new User();
        customerUser.setName("Customer Tester");
        customerUser.setEmail("customer." + suffix + "@test.com");
        customerUser.setPhone("8" + ((System.currentTimeMillis() + 1) % 1000000000L));
        customerUser.setPassword(passwordEncoder.encode("CustPass@123"));
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setIsActive(true);
        customerUser = userRepository.save(customerUser);
        customerToken = jwtService.generateAccessToken(customerUser);

        // Partner User
        partnerUser = new User();
        partnerUser.setName("Partner Tester");
        partnerUser.setEmail("partner." + suffix + "@workshop.com");
        partnerUser.setPhone("7" + ((System.currentTimeMillis() + 2) % 1000000000L));
        partnerUser.setPassword(passwordEncoder.encode("PartPass@123"));
        partnerUser.setRole(UserRole.PARTNER);
        partnerUser.setIsActive(true);
        partnerUser = userRepository.save(partnerUser);
        partnerToken = jwtService.generateAccessToken(partnerUser);

        // Vehicle for Customer
        customerVehicle = new Vehicle(
                customerUser,
                "Hyundai",
                "i20",
                2022,
                "KA" + (10 + (int)(Math.random() * 80)) + "AB" + (1000 + (int)(Math.random() * 8999)),
                FuelType.PETROL,
                Transmission.MANUAL
        );
        customerVehicle = vehicleRepository.save(customerVehicle);
    }

    @AfterEach
    void tearDown() {
        serviceCatalogRepository.deactivateTestArtifacts();
    }

    @Test
    @DisplayName("1. ADMIN can create service")
    void testAdminCanCreateService() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String name = "Test Service " + suffix;
        String jsonPayload = String.format("""
                {
                    "name": "%s",
                    "description": "Comprehensive brake disc & pad replacement",
                    "category": "BRAKE_SERVICE",
                    "basePrice": 2999.00,
                    "discountType": "FIXED_AMOUNT",
                    "discountValue": 300.00,
                    "estimatedDurationMinutes": 90,
                    "isActive": true
                }
                """, name);

        mockMvc.perform(post("/api/v1/admin/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.basePrice").value(2999.00))
                .andExpect(jsonPath("$.data.discountValue").value(300.00))
                .andExpect(jsonPath("$.data.finalPrice").value(2699.00))
                .andExpect(jsonPath("$.data.category").value("BRAKE_SERVICE"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("2. ADMIN can update service")
    void testAdminCanUpdateService() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Test Service " + suffix,
                "Initial description",
                ServiceCategory.PERIODIC_SERVICE,
                new BigDecimal("2500.00"),
                DiscountType.NO_DISCOUNT,
                BigDecimal.ZERO,
                60,
                true
        );
        service = serviceCatalogRepository.save(service);

        String updatePayload = String.format("""
                {
                    "name": "%s",
                    "description": "Updated maintenance package description",
                    "category": "PERIODIC_SERVICE",
                    "basePrice": 3200.00,
                    "discountType": "PERCENTAGE",
                    "discountValue": 10.00,
                    "estimatedDurationMinutes": 75,
                    "isActive": true
                }
                """, service.getName());

        mockMvc.perform(put("/api/v1/admin/services/" + service.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.basePrice").value(3200.00))
                .andExpect(jsonPath("$.data.discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$.data.discountValue").value(10.00))
                .andExpect(jsonPath("$.data.finalPrice").value(2880.00));
    }

    @Test
    @DisplayName("3. ADMIN can activate service")
    void testAdminCanActivateService() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Test Service " + suffix,
                "Description",
                ServiceCategory.AC_SERVICE,
                new BigDecimal("1800.00"),
                60,
                false
        );
        service = serviceCatalogRepository.save(service);

        mockMvc.perform(patch("/api/v1/admin/services/" + service.getId() + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));

        ServiceCatalog updated = serviceCatalogRepository.findById(service.getId()).orElseThrow();
        assertTrue(updated.getIsActive());
    }

    @Test
    @DisplayName("4. ADMIN can deactivate service")
    void testAdminCanDeactivateService() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Test Service " + suffix,
                "Description",
                ServiceCategory.BATTERY_SERVICE,
                new BigDecimal("4500.00"),
                45,
                true
        );
        service = serviceCatalogRepository.save(service);

        mockMvc.perform(patch("/api/v1/admin/services/" + service.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));

        ServiceCatalog updated = serviceCatalogRepository.findById(service.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
    }

    @Test
    @DisplayName("5. CUSTOMER cannot create service (403 Forbidden)")
    void testCustomerCannotCreateService() throws Exception {
        String jsonPayload = """
                {
                    "name": "Customer Created Service",
                    "description": "Hack attempt",
                    "category": "ENGINE_SERVICE",
                    "basePrice": 100.00,
                    "estimatedDurationMinutes": 30
                }
                """;

        mockMvc.perform(post("/api/v1/admin/services")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. PARTNER cannot create service (403 Forbidden)")
    void testPartnerCannotCreateService() throws Exception {
        String jsonPayload = """
                {
                    "name": "Partner Created Service",
                    "description": "Hack attempt",
                    "category": "ENGINE_SERVICE",
                    "basePrice": 100.00,
                    "estimatedDurationMinutes": 30
                }
                """;

        mockMvc.perform(post("/api/v1/admin/services")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Unauthenticated request rejected (401 Unauthorized)")
    void testUnauthenticatedRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Invalid price rejected (negative or 0 base price)")
    void testInvalidPriceRejected() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String invalidNegativePricePayload = String.format("""
                {
                    "name": "Test Service %s",
                    "description": "Testing invalid negative price",
                    "category": "GENERAL_SERVICE",
                    "basePrice": -100.00,
                    "estimatedDurationMinutes": 60
                }
                """, suffix);

        mockMvc.perform(post("/api/v1/admin/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidNegativePricePayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. Negative discount rejected")
    void testNegativeDiscountRejected() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String negativeDiscountPayload = String.format("""
                {
                    "name": "Test Service %s",
                    "description": "Testing negative discount",
                    "category": "GENERAL_SERVICE",
                    "basePrice": 1000.00,
                    "discountType": "FIXED_AMOUNT",
                    "discountValue": -50.00,
                    "estimatedDurationMinutes": 60
                }
                """, suffix);

        mockMvc.perform(post("/api/v1/admin/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativeDiscountPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("10. Discount greater than price rejected")
    void testDiscountGreaterThanPriceRejected() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String excessiveDiscountPayload = String.format("""
                {
                    "name": "Test Service %s",
                    "description": "Testing excessive fixed discount",
                    "category": "GENERAL_SERVICE",
                    "basePrice": 1000.00,
                    "discountType": "FIXED_AMOUNT",
                    "discountValue": 1500.00,
                    "estimatedDurationMinutes": 60
                }
                """, suffix);

        mockMvc.perform(post("/api/v1/admin/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(excessiveDiscountPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. Invalid duration rejected")
    void testInvalidDurationRejected() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String zeroDurationPayload = String.format("""
                {
                    "name": "Test Service %s",
                    "description": "Testing zero duration",
                    "category": "GENERAL_SERVICE",
                    "basePrice": 1000.00,
                    "estimatedDurationMinutes": 0
                }
                """, suffix);

        mockMvc.perform(post("/api/v1/admin/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zeroDurationPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("12. Invalid category rejected")
    void testInvalidCategoryRejected() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String invalidCategoryPayload = String.format("""
                {
                    "name": "Test Service %s",
                    "description": "Testing invalid category",
                    "category": "NON_EXISTENT_CATEGORY",
                    "basePrice": 1000.00,
                    "estimatedDurationMinutes": 60
                }
                """, suffix);

        mockMvc.perform(post("/api/v1/admin/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCategoryPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("13. Customer catalog returns only active services")
    void testCustomerCatalogReturnsOnlyActiveServices() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog active = new ServiceCatalog(
                "Test Service Active " + suffix,
                "Active package",
                ServiceCategory.WHEEL_ALIGNMENT,
                new BigDecimal("800.00"),
                45,
                true
        );
        serviceCatalogRepository.save(active);

        ServiceCatalog inactive = new ServiceCatalog(
                "Test Service Inactive " + suffix,
                "Inactive package",
                ServiceCategory.WHEEL_ALIGNMENT,
                new BigDecimal("900.00"),
                45,
                false
        );
        serviceCatalogRepository.save(inactive);

        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", hasItem(active.getName())))
                .andExpect(jsonPath("$.data[*].name", not(hasItem(inactive.getName()))));
    }

    @Test
    @DisplayName("14. Existing bookings retain historical price snapshots after admin price change")
    void testHistoricalBookingPriceSnapshotPreserved() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Test Service " + suffix,
                "Snapshot test service",
                ServiceCategory.PERIODIC_SERVICE,
                new BigDecimal("2999.00"),
                DiscountType.NO_DISCOUNT,
                BigDecimal.ZERO,
                90,
                true
        );
        service = serviceCatalogRepository.save(service);

        // Customer creates booking
        LocalDate futureDate = LocalDate.now().plusDays(5);
        String bookingPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "10:00"
                }
                """, customerVehicle.getId(), service.getId(), futureDate);

        String createBookingResponse = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(2999.00))
                .andReturn().getResponse().getContentAsString();

        // Extract booking ID
        com.fasterxml.jackson.databind.JsonNode jsonNode =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(createBookingResponse);
        Long bookingId = jsonNode.path("data").path("id").asLong();

        // Admin updates service catalog price to ₹3,499
        String updatePayload = String.format("""
                {
                    "name": "%s",
                    "description": "Price hiked to 3499",
                    "category": "PERIODIC_SERVICE",
                    "basePrice": 3499.00,
                    "discountType": "NO_DISCOUNT",
                    "discountValue": 0.00,
                    "estimatedDurationMinutes": 90,
                    "isActive": true
                }
                """, service.getName());

        mockMvc.perform(put("/api/v1/admin/services/" + service.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basePrice").value(3499.00));

        // Historical booking MUST still have ₹2,999 snapshot
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(2999.00))
                .andExpect(jsonPath("$.data.services[0].finalPrice").value(2999.00));
    }

    @Test
    @DisplayName("15. Multi-service booking remains compatible")
    void testMultiServiceBookingRemainsCompatible() throws Exception {
        String suffix1 = UUID.randomUUID().toString().substring(0, 6);
        String suffix2 = UUID.randomUUID().toString().substring(0, 6);

        ServiceCatalog service1 = new ServiceCatalog(
                "Test Service A " + suffix1,
                "AC Cooling",
                ServiceCategory.AC_SERVICE,
                new BigDecimal("1500.00"),
                60,
                true
        );
        service1 = serviceCatalogRepository.save(service1);

        ServiceCatalog service2 = new ServiceCatalog(
                "Test Service B " + suffix2,
                "Alignment",
                ServiceCategory.WHEEL_ALIGNMENT,
                new BigDecimal("800.00"),
                45,
                true
        );
        service2 = serviceCatalogRepository.save(service2);

        LocalDate futureDate = LocalDate.now().plusDays(6);
        String multiBookingPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d, %d],
                    "bookingDate": "%s",
                    "bookingTime": "11:00"
                }
                """, customerVehicle.getId(), service1.getId(), service2.getId(), futureDate);

        String createRes = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(multiBookingPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(2300.00))
                .andExpect(jsonPath("$.data.services", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        Long bookingId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(createRes)
                .path("data").path("id").asLong();

        // Admin updates service1 price
        String updatePayload = String.format("""
                {
                    "name": "%s",
                    "description": "Price changed",
                    "category": "AC_SERVICE",
                    "basePrice": 2200.00,
                    "discountType": "NO_DISCOUNT",
                    "discountValue": 0.00,
                    "estimatedDurationMinutes": 60,
                    "isActive": true
                }
                """, service1.getName());

        mockMvc.perform(put("/api/v1/admin/services/" + service1.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk());

        // Multi-service booking still has total ₹2,300 with line 1 = ₹1,500 and line 2 = ₹800
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(2300.00))
                .andExpect(jsonPath("$.data.services", hasSize(2)))
                .andExpect(jsonPath("$.data.services[0].finalPrice").value(1500.00))
                .andExpect(jsonPath("$.data.services[1].finalPrice").value(800.00));
    }

    @Test
    @DisplayName("16. Deactivated service cannot be newly booked")
    void testDeactivatedServiceCannotBeNewlyBooked() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Test Service Inactive " + suffix,
                "Inactive service",
                ServiceCategory.DETAILING,
                new BigDecimal("3500.00"),
                120,
                false
        );
        service = serviceCatalogRepository.save(service);

        LocalDate futureDate = LocalDate.now().plusDays(7);
        String bookingPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "14:00"
                }
                """, customerVehicle.getId(), service.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("17. Existing booking containing deactivated service remains readable")
    void testExistingBookingWithDeactivatedServiceRemainsReadable() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Test Service " + suffix,
                "Soon to be deactivated",
                ServiceCategory.DIAGNOSTICS,
                new BigDecimal("999.00"),
                30,
                true
        );
        service = serviceCatalogRepository.save(service);

        LocalDate futureDate = LocalDate.now().plusDays(8);
        String bookingPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "15:00"
                }
                """, customerVehicle.getId(), service.getId(), futureDate);

        String createRes = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(createRes)
                .path("data").path("id").asLong();

        // Admin deactivates the service
        mockMvc.perform(patch("/api/v1/admin/services/" + service.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Booking remains readable
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bookingId))
                .andExpect(jsonPath("$.data.serviceNameSnapshot").value(service.getName()))
                .andExpect(jsonPath("$.data.totalAmount").value(999.00));
    }

    @Test
    @DisplayName("18. Service cannot be hard-deleted if historical references exist")
    void testServiceCannotBeHardDeletedWithHistoricalReferences() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Test Service " + suffix,
                "Referenced service",
                ServiceCategory.TYRE_SERVICE,
                new BigDecimal("600.00"),
                30,
                true
        );
        service = serviceCatalogRepository.save(service);

        LocalDate futureDate = LocalDate.now().plusDays(9);
        String bookingPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "16:00"
                }
                """, customerVehicle.getId(), service.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isCreated());

        // Admin attempts hard delete -> should be rejected with 409 Conflict
        mockMvc.perform(delete("/api/v1/admin/services/" + service.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("referenced by historical bookings")));

        // Verify service still exists in database
        assertTrue(serviceCatalogRepository.existsById(service.getId()));

        // Deactivation should succeed
        mockMvc.perform(patch("/api/v1/admin/services/" + service.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));

        // Unreferenced service can be safely deleted
        String suffix2 = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog unreferenced = new ServiceCatalog(
                "Unreferenced Service " + suffix2,
                "No bookings",
                ServiceCategory.OTHER,
                new BigDecimal("500.00"),
                20,
                true
        );
        unreferenced = serviceCatalogRepository.save(unreferenced);

        mockMvc.perform(delete("/api/v1/admin/services/" + unreferenced.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertFalse(serviceCatalogRepository.existsById(unreferenced.getId()));
    }
}

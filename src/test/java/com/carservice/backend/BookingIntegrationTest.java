package com.carservice.backend;

import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
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
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class BookingIntegrationTest {

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

    private User customerA;
    private User customerB;
    private String tokenA;
    private String tokenB;
    private Vehicle vehicleA;
    private Vehicle vehicleB;
    private ServiceCatalog activeService;
    private ServiceCatalog inactiveService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        customerA = new User();
        customerA.setName("Customer A");
        customerA.setEmail("book.cust.a." + suffixA + "@test.com");
        customerA.setPhone("9" + (System.currentTimeMillis() % 1000000000L));
        customerA.setPassword(passwordEncoder.encode("Password@123"));
        customerA.setRole(UserRole.CUSTOMER);
        customerA.setIsActive(true);
        customerA = userRepository.save(customerA);
        tokenA = jwtService.generateAccessToken(customerA);

        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        customerB = new User();
        customerB.setName("Customer B");
        customerB.setEmail("book.cust.b." + suffixB + "@test.com");
        customerB.setPhone("8" + ((System.currentTimeMillis() + 1) % 1000000000L));
        customerB.setPassword(passwordEncoder.encode("Password@123"));
        customerB.setRole(UserRole.CUSTOMER);
        customerB.setIsActive(true);
        customerB = userRepository.save(customerB);
        tokenB = jwtService.generateAccessToken(customerB);

        vehicleA = new Vehicle(
                customerA,
                "Maruti",
                "Swift",
                2022,
                "DL01" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                FuelType.PETROL,
                Transmission.MANUAL
        );
        vehicleA = vehicleRepository.save(vehicleA);

        vehicleB = new Vehicle(
                customerB,
                "Hyundai",
                "Creta",
                2023,
                "DL02" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                FuelType.DIESEL,
                Transmission.AUTOMATIC
        );
        vehicleB = vehicleRepository.save(vehicleB);

        activeService = new ServiceCatalog(
                "Active Booking Service " + UUID.randomUUID().toString().substring(0, 6),
                "Standard active car service",
                ServiceCategory.GENERAL_SERVICE,
                new BigDecimal("1999.00"),
                120,
                true
        );
        activeService = serviceCatalogRepository.save(activeService);

        inactiveService = new ServiceCatalog(
                "Inactive Booking Service " + UUID.randomUUID().toString().substring(0, 6),
                "Discontinued service",
                ServiceCategory.AC_SERVICE,
                new BigDecimal("799.00"),
                45,
                false
        );
        inactiveService = serviceCatalogRepository.save(inactiveService);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        serviceCatalogRepository.deactivateTestArtifacts();
    }

    @Test
    @DisplayName("1. Customer can create booking successfully (201 Created) with PENDING status and catalog price")
    void testCreateBooking_Success() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "10:30",
                    "customerNotes": "Please check brake noise"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking created successfully"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.estimatedPrice").value(1999.00))
                .andExpect(jsonPath("$.data.bookingDate").value(futureDate.toString()))
                .andExpect(jsonPath("$.data.bookingTime").value("10:30:00"))
                .andExpect(jsonPath("$.data.customerNotes").value("Please check brake noise"))
                .andExpect(jsonPath("$.data.vehicle.id").value(vehicleA.getId()))
                .andExpect(jsonPath("$.data.vehicle.make").value("Maruti"))
                .andExpect(jsonPath("$.data.service.id").value(activeService.getId()))
                .andExpect(jsonPath("$.data.service.name").value(activeService.getName()));
    }

    @Test
    @DisplayName("2. Client cannot manipulate estimated price (catalog price is always enforced)")
    void testCreateBooking_PriceIntegrity() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(3);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "11:00",
                    "estimatedPrice": 1.00,
                    "status": "CONFIRMED"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.estimatedPrice").value(1999.00))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("3. Unauthenticated request is rejected (401 Unauthorized)")
    void testCreateBooking_Unauthenticated() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "14:00"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("4. Past booking date/time is rejected (400 Bad Request)")
    void testCreateBooking_PastDateTimeRejected() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "10:00"
                }
                """, vehicleA.getId(), activeService.getId(), pastDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("cannot be in the past")));
    }

    @Test
    @DisplayName("5. Customer cannot book another customer's vehicle (404 Not Found)")
    void testCreateBooking_OtherCustomerVehicleForbidden() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        // Customer A trying to book with vehicle B (owned by Customer B)
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "15:00"
                }
                """, vehicleB.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Vehicle not found")));
    }

    @Test
    @DisplayName("6. Customer cannot book an inactive service (404 Not Found)")
    void testCreateBooking_InactiveServiceForbidden() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "16:00"
                }
                """, vehicleA.getId(), inactiveService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Service not found")));
    }

    @Test
    @DisplayName("7. Customer cannot book nonexistent vehicle or service (404 Not Found)")
    void testCreateBooking_NonexistentResources() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(2);

        // Nonexistent vehicle
        String jsonPayload1 = String.format("""
                {
                    "vehicleId": 999999,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "10:00"
                }
                """, activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload1))
                .andExpect(status().isNotFound());

        // Nonexistent service
        String jsonPayload2 = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": 999999,
                    "bookingDate": "%s",
                    "bookingTime": "10:00"
                }
                """, vehicleA.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("8. Duplicate active scheduling for same vehicle and service at same time is rejected (409 Conflict)")
    void testCreateBooking_DuplicateConflict() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(4);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "12:00"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        // First booking succeeds
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());

        // Duplicate booking attempt fails with 409 Conflict
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already have an active booking")));
    }

    @Test
    @DisplayName("9. List own bookings returns only the authenticated customer's bookings")
    void testGetMyBookings_Isolation() throws Exception {
        LocalDate dateA = LocalDate.now().plusDays(2);
        LocalDate dateB = LocalDate.now().plusDays(3);

        Booking bookingA = new Booking(
                customerA, vehicleA, activeService, dateA, LocalTime.of(9, 30),
                BookingStatus.PENDING, "Notes A", activeService.getBasePrice()
        );
        Booking bookingB = new Booking(
                customerB, vehicleB, activeService, dateB, LocalTime.of(14, 0),
                BookingStatus.PENDING, "Notes B", activeService.getBasePrice()
        );
        bookingRepository.save(bookingA);
        bookingRepository.save(bookingB);

        // Customer A listing
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].id", hasItem(bookingA.getId().intValue())))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(bookingB.getId().intValue()))));

        // Customer B listing
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].id", hasItem(bookingB.getId().intValue())))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(bookingA.getId().intValue()))));
    }

    @Test
    @DisplayName("10. Retrieve single booking succeeds for owner (200 OK)")
    void testGetMyBooking_Success() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(5);
        Booking booking = new Booking(
                customerA, vehicleA, activeService, futureDate, LocalTime.of(10, 0),
                BookingStatus.PENDING, "Direct check", activeService.getBasePrice()
        );
        booking = bookingRepository.save(booking);

        mockMvc.perform(get("/api/v1/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(booking.getId()))
                .andExpect(jsonPath("$.data.vehicle.id").value(vehicleA.getId()))
                .andExpect(jsonPath("$.data.service.id").value(activeService.getId()));
    }

    @Test
    @DisplayName("11. Cross-user get booking returns 404 Not Found (no enumeration)")
    void testGetMyBooking_CrossUserForbidden() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(5);
        Booking bookingA = new Booking(
                customerA, vehicleA, activeService, futureDate, LocalTime.of(11, 0),
                BookingStatus.PENDING, "Secret notes", activeService.getBasePrice()
        );
        bookingA = bookingRepository.save(bookingA);

        mockMvc.perform(get("/api/v1/bookings/" + bookingA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    @DisplayName("12. Customer can cancel own PENDING booking (200 OK, status CANCELLED)")
    void testCancelBooking_PendingSuccess() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(6);
        Booking booking = new Booking(
                customerA, vehicleA, activeService, futureDate, LocalTime.of(13, 0),
                BookingStatus.PENDING, "Cancel me", activeService.getBasePrice()
        );
        booking = bookingRepository.save(booking);

        mockMvc.perform(put("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking cancelled successfully"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, updated.getStatus());
    }

    @Test
    @DisplayName("13. Customer can cancel own CONFIRMED booking (200 OK, status CANCELLED)")
    void testCancelBooking_ConfirmedSuccess() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(7);
        Booking booking = new Booking(
                customerA, vehicleA, activeService, futureDate, LocalTime.of(14, 0),
                BookingStatus.CONFIRMED, "Cancel confirmed", activeService.getBasePrice()
        );
        booking = bookingRepository.save(booking);

        mockMvc.perform(put("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("14. Customer cannot cancel another customer's booking (404 Not Found)")
    void testCancelBooking_CrossUserForbidden() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(8);
        Booking bookingA = new Booking(
                customerA, vehicleA, activeService, futureDate, LocalTime.of(15, 0),
                BookingStatus.PENDING, "User A booking", activeService.getBasePrice()
        );
        bookingA = bookingRepository.save(bookingA);

        mockMvc.perform(put("/api/v1/bookings/" + bookingA.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        // Verify status unchanged
        Booking unchanged = bookingRepository.findById(bookingA.getId()).orElseThrow();
        assertEquals(BookingStatus.PENDING, unchanged.getStatus());
    }

    @Test
    @DisplayName("15. Cannot cancel already CANCELLED booking (400 Bad Request)")
    void testCancelBooking_AlreadyCancelled() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(9);
        Booking booking = new Booking(
                customerA, vehicleA, activeService, futureDate, LocalTime.of(16, 0),
                BookingStatus.CANCELLED, "Already cancelled", activeService.getBasePrice()
        );
        booking = bookingRepository.save(booking);

        mockMvc.perform(put("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("cannot be cancelled")));
    }

    @Test
    @DisplayName("16. Cannot cancel IN_PROGRESS or COMPLETED booking (400 Bad Request)")
    void testCancelBooking_InProgressOrCompleted() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(10);
        Booking inProgress = new Booking(
                customerA, vehicleA, activeService, futureDate, LocalTime.of(17, 0),
                BookingStatus.IN_PROGRESS, "In progress", activeService.getBasePrice()
        );
        inProgress = bookingRepository.save(inProgress);

        mockMvc.perform(put("/api/v1/bookings/" + inProgress.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be cancelled")));

        Booking completed = new Booking(
                customerA, vehicleA, activeService, futureDate, LocalTime.of(18, 0),
                BookingStatus.COMPLETED, "Completed", activeService.getBasePrice()
        );
        completed = bookingRepository.save(completed);

        mockMvc.perform(put("/api/v1/bookings/" + completed.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be cancelled")));
    }

    @Test
    @DisplayName("17. Re-booking after cancellation is allowed (no false conflict)")
    void testRebookingAfterCancellation_Allowed() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(11);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "bookingTime": "09:00"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        // 1. Create booking
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());

        // Find and cancel it
        Booking booking = bookingRepository.findAllByUserIdOrderByCreatedAtDesc(customerA.getId()).get(0);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // 2. Re-create booking for same vehicle/service/date/time should succeed now
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("18. Booking generates unique human-friendly bookingReference (CSB-YYYYMMDD-XXXXXX)")
    void testCreateBooking_GeneratesBookingReference() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(12);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "timeSlot": "11:00-12:00",
                    "customerNotes": "Ref test"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookingReference").value(matchesPattern("^CSB-\\d{8}-\\d{6}$")))
                .andExpect(jsonPath("$.data.timeSlot").value("11:00-12:00"))
                .andExpect(jsonPath("$.data.serviceNameSnapshot").value(activeService.getName()))
                .andExpect(jsonPath("$.data.servicePriceSnapshot").value(activeService.getBasePrice().doubleValue()))
                .andExpect(jsonPath("$.data.price").value(activeService.getBasePrice().doubleValue()));
    }

    @Test
    @DisplayName("19. Arbitrary uncontrolled time slots are rejected (400 Bad Request)")
    void testCreateBooking_InvalidTimeSlotRejected() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(13);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "timeSlot": "whenever"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Invalid time slot")));
    }

    @Test
    @DisplayName("20. Price snapshot immutability: changing service catalog price does not alter historical booking price")
    void testPriceSnapshotImmutability() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(14);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "timeSlot": "14:00-15:00"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        // 1. Create booking at basePrice 1999.00
        String responseContent = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.price").value(1999.00))
                .andReturn().getResponse().getContentAsString();

        Long bookingId = ((Number) com.jayway.jsonpath.JsonPath.read(responseContent, "$.data.id")).longValue();

        // 2. Change price in service catalog to 3500.00
        ServiceCatalog catalogService = serviceCatalogRepository.findById(activeService.getId()).orElseThrow();
        catalogService.setBasePrice(new BigDecimal("3500.00"));
        serviceCatalogRepository.save(catalogService);

        // 3. Fetch booking - must still show snapshot price 1999.00
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.servicePriceSnapshot").value(1999.00))
                .andExpect(jsonPath("$.data.estimatedPrice").value(1999.00))
                .andExpect(jsonPath("$.data.price").value(1999.00));

        // Restore original price
        catalogService.setBasePrice(new BigDecimal("1999.00"));
        serviceCatalogRepository.save(catalogService);
    }

    @Test
    @DisplayName("21. Security: User B cannot view User A's booking (404 Not Found)")
    void testSecurity_UserBCannotViewUserABooking() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(15);
        Booking bookingA = new Booking(
                customerA, vehicleA, activeService, futureDate, LocalTime.of(10, 0),
                BookingStatus.PENDING, "A private notes", activeService.getBasePrice()
        );
        bookingA = bookingRepository.save(bookingA);

        mockMvc.perform(get("/api/v1/bookings/" + bookingA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("22. Security: User B cannot book service using User A's vehicle (404 Not Found)")
    void testSecurity_UserBCannotBookWithUserAVehicle() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(16);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "timeSlot": "15:00-16:00"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Vehicle not found")));
    }

    @Test
    @DisplayName("23. Multi-service booking: Customer can book 2 services and server calculates total amount")
    void testCreateBooking_MultipleServices_Success() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service1 = new ServiceCatalog(
                "Multi AC " + suffix,
                "AC check",
                ServiceCategory.AC_SERVICE,
                new BigDecimal("1500.00"),
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                60,
                true
        );
        service1 = serviceCatalogRepository.save(service1);

        ServiceCatalog service2 = new ServiceCatalog(
                "Multi Alignment " + suffix,
                "Wheel alignment",
                ServiceCategory.WHEEL_ALIGNMENT,
                new BigDecimal("800.00"),
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("100.00"),
                45,
                true
        );
        service2 = serviceCatalogRepository.save(service2);

        LocalDate futureDate = LocalDate.now().plusDays(17);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d, %d],
                    "bookingDate": "%s",
                    "timeSlot": "09:00-10:00",
                    "customerNotes": "Two services"
                }
                """, vehicleA.getId(), service1.getId(), service2.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(2050.00))
                .andExpect(jsonPath("$.data.services", hasSize(2)))
                .andExpect(jsonPath("$.data.services[0].serviceId").value(service1.getId()))
                .andExpect(jsonPath("$.data.services[0].serviceName").value(service1.getName()))
                .andExpect(jsonPath("$.data.services[0].basePrice").value(1500.00))
                .andExpect(jsonPath("$.data.services[0].discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$.data.services[0].discountValue").value(10.00))
                .andExpect(jsonPath("$.data.services[0].finalPrice").value(1350.00))
                .andExpect(jsonPath("$.data.services[1].serviceId").value(service2.getId()))
                .andExpect(jsonPath("$.data.services[1].serviceName").value(service2.getName()))
                .andExpect(jsonPath("$.data.services[1].basePrice").value(800.00))
                .andExpect(jsonPath("$.data.services[1].discountType").value("FIXED_AMOUNT"))
                .andExpect(jsonPath("$.data.services[1].discountValue").value(100.00))
                .andExpect(jsonPath("$.data.services[1].finalPrice").value(700.00));
    }

    @Test
    @DisplayName("24. Multi-service booking: Three or more services can be booked and line items created")
    void testCreateBooking_ThreeOrMoreServices_Success() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog s1 = serviceCatalogRepository.save(new ServiceCatalog(
                "Service One " + suffix, "Desc 1", ServiceCategory.GENERAL_SERVICE,
                new BigDecimal("1000.00"), DiscountType.NO_DISCOUNT, BigDecimal.ZERO, 60, true
        ));
        ServiceCatalog s2 = serviceCatalogRepository.save(new ServiceCatalog(
                "Service Two " + suffix, "Desc 2", ServiceCategory.BRAKE_SERVICE,
                new BigDecimal("2000.00"), DiscountType.PERCENTAGE, new BigDecimal("15.00"), 60, true
        ));
        ServiceCatalog s3 = serviceCatalogRepository.save(new ServiceCatalog(
                "Service Three " + suffix, "Desc 3", ServiceCategory.TYRE_SERVICE,
                new BigDecimal("500.00"), DiscountType.FIXED_AMOUNT, new BigDecimal("50.00"), 30, true
        ));

        LocalDate futureDate = LocalDate.now().plusDays(18);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d, %d, %d],
                    "bookingDate": "%s",
                    "timeSlot": "10:00-11:00"
                }
                """, vehicleA.getId(), s1.getId(), s2.getId(), s3.getId(), futureDate);

        // 1000 + 1700 + 450 = 3150.00
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(3150.00))
                .andExpect(jsonPath("$.data.services", hasSize(3)))
                .andExpect(jsonPath("$.data.services[0].finalPrice").value(1000.00))
                .andExpect(jsonPath("$.data.services[1].finalPrice").value(1700.00))
                .andExpect(jsonPath("$.data.services[2].finalPrice").value(450.00));
    }

    @Test
    @DisplayName("25. Multi-service booking: Empty serviceIds list is rejected (400 Bad Request)")
    void testCreateBooking_EmptyServiceIds_Rejected() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(19);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [],
                    "bookingDate": "%s",
                    "timeSlot": "12:00-13:00"
                }
                """, vehicleA.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("At least one service must be selected")));
    }

    @Test
    @DisplayName("26. Multi-service booking: Unknown service ID in serviceIds list is rejected (404 Not Found)")
    void testCreateBooking_UnknownServiceIdInList_Rejected() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(20);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d, 9999999],
                    "bookingDate": "%s",
                    "timeSlot": "14:00-15:00"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Service not found")));
    }

    @Test
    @DisplayName("27. Multi-service booking: Inactive service in serviceIds list is rejected (404 Not Found)")
    void testCreateBooking_InactiveServiceInList_Rejected() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(21);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d, %d],
                    "bookingDate": "%s",
                    "timeSlot": "14:00-15:00"
                }
                """, vehicleA.getId(), activeService.getId(), inactiveService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Service not found")));
    }

    @Test
    @DisplayName("28. Transaction atomicity: Failure during multi-service creation persists no partial booking")
    void testCreateBooking_TransactionRollbackOnInvalidService() throws Exception {
        long initialBookingCount = bookingRepository.count();
        long initialItemCount = bookingServiceRepository.count();

        LocalDate futureDate = LocalDate.now().plusDays(22);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d, 8888888],
                    "bookingDate": "%s",
                    "timeSlot": "15:00-16:00"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound());

        // Verify that no booking or line items were saved
        assertEquals(initialBookingCount, bookingRepository.count());
        assertEquals(initialItemCount, bookingServiceRepository.count());
    }

    @Test
    @DisplayName("29. Multi-service booking: Duplicate service IDs are safely deduplicated")
    void testCreateBooking_DuplicateServiceIdsDeduplicated() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog s1 = serviceCatalogRepository.save(new ServiceCatalog(
                "Dedup S1 " + suffix, "Desc", ServiceCategory.AC_SERVICE,
                new BigDecimal("1000.00"), 60, true
        ));
        ServiceCatalog s2 = serviceCatalogRepository.save(new ServiceCatalog(
                "Dedup S2 " + suffix, "Desc", ServiceCategory.BRAKE_SERVICE,
                new BigDecimal("500.00"), 45, true
        ));

        LocalDate futureDate = LocalDate.now().plusDays(23);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d, %d, %d],
                    "bookingDate": "%s",
                    "timeSlot": "16:00-17:00"
                }
                """, vehicleA.getId(), s1.getId(), s2.getId(), s1.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.services", hasSize(2)))
                .andExpect(jsonPath("$.data.totalAmount").value(1500.00));
    }

    @Test
    @DisplayName("30. Security: Client-supplied fake total, prices, discount and status are strictly ignored")
    void testCreateBooking_ClientSuppliedTamperedFieldsIgnored() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = serviceCatalogRepository.save(new ServiceCatalog(
                "Tamper Test " + suffix, "Desc", ServiceCategory.PERIODIC_SERVICE,
                new BigDecimal("2999.00"), 120, true
        ));

        LocalDate futureDate = LocalDate.now().plusDays(24);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d],
                    "bookingDate": "%s",
                    "timeSlot": "09:00-10:00",
                    "totalAmount": 1.00,
                    "price": 1.00,
                    "finalPrice": 1.00,
                    "discountValue": 99.99,
                    "status": "COMPLETED",
                    "bookingReference": "CSB-HACKED-123456"
                }
                """, vehicleA.getId(), service.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(2999.00))
                .andExpect(jsonPath("$.data.services[0].finalPrice").value(2999.00))
                .andExpect(jsonPath("$.data.bookingReference").value(not("CSB-HACKED-123456")))
                .andExpect(jsonPath("$.data.bookingReference").value(matchesPattern("^CSB-\\d{8}-\\d{6}$")));
    }

    @Test
    @DisplayName("31. Immutability: Modifying service catalog name, price and discount does not alter booking snapshots")
    void testImmutability_MultiServiceSnapshotsPreserved() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog s1 = serviceCatalogRepository.save(new ServiceCatalog(
                "Immut S1 " + suffix, "Desc", ServiceCategory.GENERAL_SERVICE,
                new BigDecimal("1500.00"), DiscountType.PERCENTAGE, new BigDecimal("10.00"), 60, true
        ));
        ServiceCatalog s2 = serviceCatalogRepository.save(new ServiceCatalog(
                "Immut S2 " + suffix, "Desc", ServiceCategory.AC_SERVICE,
                new BigDecimal("800.00"), DiscountType.FIXED_AMOUNT, new BigDecimal("100.00"), 45, true
        ));

        LocalDate futureDate = LocalDate.now().plusDays(25);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d, %d],
                    "bookingDate": "%s",
                    "timeSlot": "10:00-11:00"
                }
                """, vehicleA.getId(), s1.getId(), s2.getId(), futureDate);

        String responseContent = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(2050.00))
                .andReturn().getResponse().getContentAsString();

        Long bookingId = ((Number) com.jayway.jsonpath.JsonPath.read(responseContent, "$.data.id")).longValue();

        // Admin changes s1 and s2 in catalog
        s1.setName("Changed S1 Name " + UUID.randomUUID().toString().substring(0, 6));
        s1.setBasePrice(new BigDecimal("5000.00"));
        s1.setDiscountType(DiscountType.NO_DISCOUNT);
        s1.setDiscountValue(BigDecimal.ZERO);
        serviceCatalogRepository.save(s1);

        s2.setName("Changed S2 Name " + UUID.randomUUID().toString().substring(0, 6));
        s2.setBasePrice(new BigDecimal("3000.00"));
        serviceCatalogRepository.save(s2);

        // Fetch booking - must still have original snapshot prices, names and total
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(2050.00))
                .andExpect(jsonPath("$.data.services", hasSize(2)))
                .andExpect(jsonPath("$.data.services[0].serviceName").value("Immut S1 " + suffix))
                .andExpect(jsonPath("$.data.services[0].basePrice").value(1500.00))
                .andExpect(jsonPath("$.data.services[0].discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$.data.services[0].discountValue").value(10.00))
                .andExpect(jsonPath("$.data.services[0].finalPrice").value(1350.00))
                .andExpect(jsonPath("$.data.services[1].serviceName").value("Immut S2 " + suffix))
                .andExpect(jsonPath("$.data.services[1].basePrice").value(800.00))
                .andExpect(jsonPath("$.data.services[1].finalPrice").value(700.00));
    }

    @Test
    @DisplayName("32. Double booking protection: Conflict rule blocks multi-service booking at same vehicle/date/time")
    void testDoubleBooking_MultiServiceBlocked() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(26);
        String jsonPayload1 = String.format("""
                {
                    "vehicleId": %d,
                    "serviceId": %d,
                    "bookingDate": "%s",
                    "timeSlot": "11:00-12:00"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        // First booking
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload1))
                .andExpect(status().isCreated());

        // Second booking attempt with multiple services for same vehicle and time slot
        String jsonPayload2 = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d],
                    "bookingDate": "%s",
                    "timeSlot": "11:00-12:00"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already have an active booking")));
    }

    @Test
    @DisplayName("33. List bookings includes services line items and server totalAmount")
    void testGetMyBookings_IncludesServicesAndTotalAmount() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(27);
        String jsonPayload = String.format("""
                {
                    "vehicleId": %d,
                    "serviceIds": [%d],
                    "bookingDate": "%s",
                    "timeSlot": "14:00-15:00"
                }
                """, vehicleA.getId(), activeService.getId(), futureDate);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].services").isArray())
                .andExpect(jsonPath("$.data[0].totalAmount").isNumber());
    }
}

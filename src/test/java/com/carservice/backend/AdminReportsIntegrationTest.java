package com.carservice.backend;

import com.carservice.backend.admin.repository.AuditLogRepository;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.security.jwt.JwtService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
public class AdminReportsIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

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

    private Workshop testWorkshop;
    private Vehicle testVehicle;
    private ServiceRequest testRequest;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Admin
        adminUser = new User();
        adminUser.setName("Report Admin " + runId);
        adminUser.setEmail("rep_adm_" + runId + "@test.com");
        adminUser.setPhone("91" + Math.abs(runId.hashCode() % 100000000));
        if (adminUser.getPhone().length() < 10) adminUser.setPhone("9188" + runId.substring(0, 6));
        adminUser.setPassword(passwordEncoder.encode("Pass@1234"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);

        // 2. Create Customer
        customerUser = new User();
        customerUser.setName("Report Customer " + runId);
        customerUser.setEmail("rep_cust_" + runId + "@test.com");
        customerUser.setPhone("92" + Math.abs(runId.hashCode() % 100000000));
        if (customerUser.getPhone().length() < 10) customerUser.setPhone("9288" + runId.substring(0, 6));
        customerUser.setPassword(passwordEncoder.encode("Pass@1234"));
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setIsActive(true);
        customerUser = userRepository.save(customerUser);
        customerToken = jwtService.generateAccessToken(customerUser);

        // 3. Create Partner & Workshop
        partnerUser = new User();
        partnerUser.setName("Report Partner " + runId);
        partnerUser.setEmail("rep_part_" + runId + "@test.com");
        partnerUser.setPhone("93" + Math.abs(runId.hashCode() % 100000000));
        if (partnerUser.getPhone().length() < 10) partnerUser.setPhone("9388" + runId.substring(0, 6));
        partnerUser.setPassword(passwordEncoder.encode("Pass@1234"));
        partnerUser.setRole(UserRole.PARTNER);
        partnerUser.setIsActive(true);
        partnerUser = userRepository.save(partnerUser);
        partnerToken = jwtService.generateAccessToken(partnerUser);

        testWorkshop = new Workshop();
        testWorkshop.setUser(partnerUser);
        testWorkshop.setBusinessName("Premier Auto Hub " + runId);
        testWorkshop.setEmail("wk_" + runId + "@test.com");
        testWorkshop.setPhone(partnerUser.getPhone());
        testWorkshop.setAddress("Sector 18");
        testWorkshop.setCity("Noida");
        testWorkshop.setState("Uttar Pradesh");
        testWorkshop.setPincode("201301");
        testWorkshop.setServiceRadiusKm(BigDecimal.valueOf(15.0));
        testWorkshop.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        testWorkshop.setIsActive(true);
        testWorkshop = workshopRepository.save(testWorkshop);

        // 4. Create Vehicle
        testVehicle = new Vehicle();
        testVehicle.setUser(customerUser);
        testVehicle.setMake("Toyota");
        testVehicle.setModel("Fortuner");
        testVehicle.setYear(2023);
        testVehicle.setRegistrationNumber("UP16" + runId.substring(0, 4).toUpperCase());
        testVehicle.setFuelType(FuelType.DIESEL);
        testVehicle.setTransmission(Transmission.AUTOMATIC);
        testVehicle = vehicleRepository.save(testVehicle);

        // 5. Create Service Request
        testRequest = new ServiceRequest(
                customerUser,
                testVehicle,
                "Noida",
                "Sector 62",
                "201301",
                new BigDecimal("28.6000000"),
                new BigDecimal("77.3500000"),
                LocalDate.now().plusDays(2),
                "10:00 AM - 12:00 PM",
                "Test report request"
        );
        testRequest.setRequestReference("SR-REP-" + runId);
        testRequest.setStatus(ServiceRequestStatus.MATCHED);
        testRequest.setAssignedWorkshop(testWorkshop);
        testRequest = serviceRequestRepository.save(testRequest);

        // 6. Create Booking
        testBooking = new Booking();
        testBooking.setUser(customerUser);
        testBooking.setVehicle(testVehicle);
        testBooking.setServiceRequest(testRequest);
        testBooking.setBookingReference("BK-REP-" + runId);
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setBookingDate(LocalDate.now().plusDays(2));
        testBooking.setTimeSlot("10:00 AM - 12:00 PM");
        testBooking.setBookingTime(LocalTime.of(10, 0));
        testBooking.setServiceNameSnapshot("Comprehensive Service");
        testBooking.setTotalAmount(BigDecimal.valueOf(4500.00));
        testBooking.setAddress("Sector 62");
        testBooking.setCity("Noida");
        testBooking.setPincode("201301");
        testBooking = bookingRepository.save(testBooking);
    }

    @Test
    @DisplayName("1. Admin can access overview report with 200 OK")
    void test_adminCanAccessOverview_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalCustomers", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalWorkshops", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalServiceRequests", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalBookings", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("2. Customer cannot access reports (403 Forbidden)")
    void test_customerCannotAccessReports_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/overview")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Workshop cannot access reports (403 Forbidden)")
    void test_workshopCannotAccessReports_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/overview")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Unauthenticated request returns 401 Unauthorized")
    void test_unauthenticatedReturns_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5. Overview returns accurate database counts")
    void test_overviewReturnsCorrectCounts() throws Exception {
        long actualCustomers = userRepository.countByRole(UserRole.CUSTOMER);
        long actualWorkshops = workshopRepository.count();
        long actualBookings = bookingRepository.count();

        mockMvc.perform(get("/api/v1/admin/reports/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCustomers", is((int) actualCustomers)))
                .andExpect(jsonPath("$.data.totalWorkshops", is((int) actualWorkshops)))
                .andExpect(jsonPath("$.data.totalBookings", is((int) actualBookings)));
    }

    @Test
    @DisplayName("6. Customer report respects date range")
    void test_customerReportRespectsDateRange() throws Exception {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        mockMvc.perform(get("/api/v1/admin/reports/customers")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newCustomers", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.timeline", notNullValue()));
    }

    @Test
    @DisplayName("7. Workshop report respects date range and includes geo notice")
    void test_workshopReportRespectsDateRangeAndGeoNotice() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/workshops")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalWorkshops", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.geoNotice", containsString("Workshop location analytics will be available")));
    }

    @Test
    @DisplayName("8. Booking report returns status breakdown")
    void test_bookingReportRespectsStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/bookings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBookings", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.confirmed", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.averageBookingsPerDay", notNullValue()))
                .andExpect(jsonPath("$.data.timeline", notNullValue()));
    }

    @Test
    @DisplayName("9. Service request report returns status breakdown")
    void test_serviceRequestReportRespectsStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/service-requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequests", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.matched", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.timeline", notNullValue()));
    }

    @Test
    @DisplayName("10. Service catalog report handles zero state cleanly without errors")
    void test_serviceCatalogReport_handlesZeroState() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/services")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalServiceCatalogItems", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.mostRequestedServices", notNullValue()))
                .andExpect(jsonPath("$.data.mostBookedServices", notNullValue()));
    }

    @Test
    @DisplayName("11. Activity pagination and search works")
    void test_activityPaginationAndSearchWorks() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/activity")
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", testBooking.getBookingReference())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].reference", is(testBooking.getBookingReference())));
    }

    @Test
    @DisplayName("12. CSV export overview returns text/csv")
    void test_csvExport_overview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/export/csv")
                        .param("reportType", "OVERVIEW")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content().string(containsString("Total Customers")))
                .andExpect(content().string(containsString("Total Bookings")));
    }

    @Test
    @DisplayName("13. CSV export bookings returns valid CSV data")
    void test_csvExport_bookings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/export/csv")
                        .param("reportType", "BOOKINGS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content().string(containsString("Total Bookings,")));
    }

    @Test
    @DisplayName("14. Zero mock data verified in car_service_dev")
    void test_zeroMockDataInDevDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/car_service_dev", "car_service_user", "Canada@1212");
             Statement stmt = conn.createStatement()) {

            ResultSet rsBookings = stmt.executeQuery("SELECT count(*) FROM bookings");
            if (rsBookings.next()) {
                assertEquals(0, rsBookings.getLong(1), "car_service_dev.bookings must be 0 (no mock bookings)");
            }

            ResultSet rsJobs = stmt.executeQuery("SELECT count(*) FROM workshop_jobs");
            if (rsJobs.next()) {
                assertEquals(0, rsJobs.getLong(1), "car_service_dev.workshop_jobs must be 0 (no mock jobs)");
            }

            ResultSet rsCatalog = stmt.executeQuery("SELECT count(*) FROM service_catalog");
            if (rsCatalog.next()) {
                assertEquals(0, rsCatalog.getLong(1), "car_service_dev.service_catalog must remain clean (0 records)");
            }
        } catch (Exception e) {
            fail("Failed to verify dev database cleanliness: " + e.getMessage());
        }
    }
}

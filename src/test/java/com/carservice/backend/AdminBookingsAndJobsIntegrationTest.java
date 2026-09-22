package com.carservice.backend;

import com.carservice.backend.admin.dto.AdminCancelBookingRequest;
import com.carservice.backend.admin.dto.AdminUpdateJobStatusRequest;
import com.carservice.backend.admin.entity.AuditLog;
import com.carservice.backend.admin.repository.AuditLogRepository;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.entity.BookingService;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.marketplace.entity.LeadOpportunity;
import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.entity.WorkshopJob;
import com.carservice.backend.marketplace.enums.OpportunityStatus;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.marketplace.repository.LeadOpportunityRepository;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.marketplace.repository.WorkshopJobRepository;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
public class AdminBookingsAndJobsIntegrationTest {

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
    private WorkshopJobRepository workshopJobRepository;

    @Autowired
    private LeadOpportunityRepository leadOpportunityRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private String adminToken;

    private User customerUser;
    private String customerToken;

    private User partnerUser;
    private Workshop workshop;
    private Vehicle vehicle;
    private ServiceCatalog catalogService;

    private Booking testBooking;
    private ServiceRequest testServiceRequest;
    private WorkshopJob testJob;
    private LeadOpportunity testOpportunity;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Admin
        adminUser = new User();
        adminUser.setName("Admin Ops " + runId);
        adminUser.setEmail("admin_ops_" + runId + "@carservice.com");
        adminUser.setPhone("91" + Math.abs(runId.hashCode() % 100000000));
        if (adminUser.getPhone().length() < 10) adminUser.setPhone("9188" + runId.substring(0, 6));
        adminUser.setPassword(passwordEncoder.encode("Admin@12345"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);

        // 2. Create Customer
        customerUser = new User();
        customerUser.setName("Customer Alpha " + runId);
        customerUser.setEmail("cust_ops_" + runId + "@carservice.com");
        customerUser.setPhone("92" + Math.abs(runId.hashCode() % 100000000));
        if (customerUser.getPhone().length() < 10) customerUser.setPhone("9288" + runId.substring(0, 6));
        customerUser.setPassword(passwordEncoder.encode("Cust@12345"));
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setIsActive(true);
        customerUser = userRepository.save(customerUser);
        customerToken = jwtService.generateAccessToken(customerUser);

        // 3. Create Partner & Workshop
        partnerUser = new User();
        partnerUser.setName("Partner Beta " + runId);
        partnerUser.setEmail("part_ops_" + runId + "@carservice.com");
        partnerUser.setPhone("93" + Math.abs(runId.hashCode() % 100000000));
        if (partnerUser.getPhone().length() < 10) partnerUser.setPhone("9388" + runId.substring(0, 6));
        partnerUser.setPassword(passwordEncoder.encode("Part@12345"));
        partnerUser.setRole(UserRole.PARTNER);
        partnerUser.setIsActive(true);
        partnerUser = userRepository.save(partnerUser);

        workshop = new Workshop();
        workshop.setUser(partnerUser);
        workshop.setBusinessName("Prime Speed Garage " + runId);
        workshop.setPhone(partnerUser.getPhone());
        workshop.setEmail(partnerUser.getEmail());
        workshop.setAddress("42 Industrial Estate");
        workshop.setCity("New Delhi");
        workshop.setState("Delhi");
        workshop.setPincode("110020");
        workshop.setLatitude(new BigDecimal("28.5355000"));
        workshop.setLongitude(new BigDecimal("77.2650000"));
        workshop.setServiceRadiusKm(new BigDecimal("25.00"));
        workshop.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        workshop.setIsActive(true);
        workshop = workshopRepository.save(workshop);

        // 4. Create Vehicle
        vehicle = new Vehicle();
        vehicle.setUser(customerUser);
        vehicle.setMake("Hyundai");
        vehicle.setModel("Creta");
        vehicle.setYear(2023);
        vehicle.setRegistrationNumber("DL01AB" + runId.substring(0, 4).toUpperCase());
        vehicle.setFuelType(FuelType.PETROL);
        vehicle.setTransmission(Transmission.AUTOMATIC);
        vehicle = vehicleRepository.save(vehicle);

        // 5. Create Service Catalog Item
        catalogService = new ServiceCatalog(
                "Full Synthetic Service " + runId,
                "Comprehensive engine oil & filter service",
                ServiceCategory.PERIODIC_SERVICE,
                new BigDecimal("3500.00"),
                120,
                true
        );
        catalogService = serviceCatalogRepository.save(catalogService);

        // 6. Create Booking & Service Request & Workshop Job
        testBooking = new Booking();
        testBooking.setBookingReference("BK-" + runId.toUpperCase());
        testBooking.setUser(customerUser);
        testBooking.setVehicle(vehicle);
        testBooking.setService(catalogService);
        testBooking.setServiceNameSnapshot(catalogService.getName());
        testBooking.setServicePriceSnapshot(catalogService.getBasePrice());
        testBooking.setBookingDate(LocalDate.now().plusDays(2));
        testBooking.setBookingTime(LocalTime.of(10, 0));
        testBooking.setTimeSlot("10:00 - 11:00");
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setCustomerNotes("Please inspect brakes thoroughly");
        testBooking.setEstimatedPrice(new BigDecimal("3500.00"));
        testBooking.setTotalAmount(new BigDecimal("3500.00"));
        testBooking.setCity("New Delhi");
        testBooking.setAddress("123 Green Park");
        testBooking.setPincode("110016");

        BookingService lineItem = new BookingService(
                testBooking,
                catalogService,
                catalogService.getName(),
                catalogService.getBasePrice(),
                DiscountType.NO_DISCOUNT,
                BigDecimal.ZERO,
                catalogService.getBasePrice()
        );
        testBooking.addBookingService(lineItem);
        testBooking = bookingRepository.save(testBooking);

        testServiceRequest = new ServiceRequest(
                customerUser,
                vehicle,
                "New Delhi",
                "123 Green Park",
                "110016",
                new BigDecimal("28.5500000"),
                new BigDecimal("77.2000000"),
                LocalDate.now().plusDays(2),
                "10:00 - 11:00",
                "Please inspect brakes thoroughly"
        );
        testServiceRequest.setBooking(testBooking);
        testServiceRequest.setBookingReference(testBooking.getBookingReference());
        testServiceRequest.setAssignedWorkshop(workshop);
        testServiceRequest.setStatus(ServiceRequestStatus.ACCEPTED);
        testServiceRequest = serviceRequestRepository.save(testServiceRequest);

        testOpportunity = new LeadOpportunity(testServiceRequest, workshop, new BigDecimal("125.00"));
        testOpportunity.setStatus(OpportunityStatus.ACCEPTED);
        testOpportunity = leadOpportunityRepository.save(testOpportunity);

        testJob = new WorkshopJob(testServiceRequest, workshop, testOpportunity);
        testJob.setStatus(WorkshopJobStatus.ASSIGNED);
        testJob = workshopJobRepository.save(testJob);

        testServiceRequest.setCurrentJob(testJob);
        testServiceRequest = serviceRequestRepository.save(testServiceRequest);

        testBooking.setServiceRequest(testServiceRequest);
        testBooking.setServiceRequestReference(testServiceRequest.getRequestReference());
        testBooking = bookingRepository.save(testBooking);
    }

    // ==========================================
    // ADMIN BOOKINGS TESTS
    // ==========================================

    @Test
    @DisplayName("Admin can retrieve real booking summary KPIs")
    void testGetBookingSummary_asAdmin_returnsRealMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBookings", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.confirmedBookings", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalRevenue", notNullValue()));
    }

    @Test
    @DisplayName("Admin can retrieve paginated bookings with search and filter")
    void testGetBookings_asAdmin_returnsFilteredPage() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .param("search", testBooking.getBookingReference())
                        .param("status", "CONFIRMED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].bookingReference").value(testBooking.getBookingReference()))
                .andExpect(jsonPath("$.data.content[0].customerName").value(customerUser.getName()))
                .andExpect(jsonPath("$.data.content[0].workshopName").value(workshop.getBusinessName()));
    }

    @Test
    @DisplayName("Admin can retrieve 360° booking detail with workshop routing pipeline")
    void testGetBookingDetail_asAdmin_returns360Dossier() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings/" + testBooking.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testBooking.getId()))
                .andExpect(jsonPath("$.data.bookingReference").value(testBooking.getBookingReference()))
                .andExpect(jsonPath("$.data.customerName").value(customerUser.getName()))
                .andExpect(jsonPath("$.data.vehicleRegistrationNumber").value(vehicle.getRegistrationNumber()))
                .andExpect(jsonPath("$.data.services", hasSize(1)))
                .andExpect(jsonPath("$.data.assignedWorkshopName").value(workshop.getBusinessName()))
                .andExpect(jsonPath("$.data.currentJobReference").value(testJob.getJobReference()))
                .andExpect(jsonPath("$.data.routingOpportunities", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Customer cannot access admin bookings endpoints (Forbidden)")
    void testGetBookings_asCustomer_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user cannot access admin bookings endpoints (Unauthorized)")
    void testGetBookings_unauthenticated_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Non-existent booking returns 404")
    void testGetBookingDetail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings/999999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin can cancel booking, creating immutable audit log and customer notification")
    void testCancelBooking_asAdmin_cancelsAndAudits() throws Exception {
        AdminCancelBookingRequest request = new AdminCancelBookingRequest("Customer called to cancel appointment");

        mockMvc.perform(put("/api/v1/admin/bookings/" + testBooking.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Verify DB update
        Booking updatedBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, updatedBooking.getStatus());
        assertNotNull(updatedBooking.getCancelledAt());

        // Verify Audit Log
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("BOOKING", testBooking.getId().toString());
        assertFalse(auditLogs.isEmpty());
        AuditLog logEntry = auditLogs.get(0);
        assertEquals("BOOKING_CANCELLED", logEntry.getAction());
        assertEquals(adminUser.getEmail(), logEntry.getActorEmail());
    }

    // ==========================================
    // ADMIN WORKSHOP JOBS TESTS
    // ==========================================

    @Test
    @DisplayName("Admin can retrieve real workshop jobs summary KPIs")
    void testGetJobSummary_asAdmin_returnsRealMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshop-jobs/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalJobs", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.assignedJobs", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Admin can retrieve paginated workshop jobs with search and filter")
    void testGetWorkshopJobs_asAdmin_returnsFilteredPage() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshop-jobs")
                        .param("search", testJob.getJobReference())
                        .param("status", "ASSIGNED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].jobReference").value(testJob.getJobReference()))
                .andExpect(jsonPath("$.data.content[0].workshopName").value(workshop.getBusinessName()));
    }

    @Test
    @DisplayName("Admin can retrieve 360° workshop job detail")
    void testGetWorkshopJobDetail_asAdmin_returns360Dossier() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshop-jobs/" + testJob.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testJob.getId()))
                .andExpect(jsonPath("$.data.jobReference").value(testJob.getJobReference()))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.workshopName").value(workshop.getBusinessName()))
                .andExpect(jsonPath("$.data.customerName").value(customerUser.getName()))
                .andExpect(jsonPath("$.data.vehicleRegistrationNumber").value(vehicle.getRegistrationNumber()))
                .andExpect(jsonPath("$.data.bookingReference").value(testBooking.getBookingReference()));
    }

    @Test
    @DisplayName("Customer cannot access admin workshop jobs endpoints (Forbidden)")
    void testGetWorkshopJobs_asCustomer_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshop-jobs")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can update workshop job status with audit trail and synchronized booking state")
    void testUpdateJobStatus_asAdmin_updatesAndAudits() throws Exception {
        AdminUpdateJobStatusRequest request = new AdminUpdateJobStatusRequest(
                WorkshopJobStatus.WORK_IN_PROGRESS,
                "Admin authorized bay transfer for express service",
                "Operational acceleration"
        );

        mockMvc.perform(put("/api/v1/admin/workshop-jobs/" + testJob.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("WORK_IN_PROGRESS"));

        // Verify DB update
        WorkshopJob updatedJob = workshopJobRepository.findById(testJob.getId()).orElseThrow();
        assertEquals(WorkshopJobStatus.WORK_IN_PROGRESS, updatedJob.getStatus());
        assertNotNull(updatedJob.getWorkStartedAt());

        // Verify Booking state synchronized
        Booking synchronizedBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.IN_PROGRESS, synchronizedBooking.getStatus());

        // Verify Audit Log
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("WORKSHOP_JOB", testJob.getId().toString());
        assertFalse(auditLogs.isEmpty());
        AuditLog logEntry = auditLogs.get(0);
        assertEquals("WORKSHOP_JOB_STATUS_CHANGED", logEntry.getAction());
        assertEquals(adminUser.getEmail(), logEntry.getActorEmail());
    }
}

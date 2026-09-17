package com.carservice.backend;

import com.carservice.backend.admin.dto.UpdateWorkshopStatusRequest;
import com.carservice.backend.admin.dto.UpdateWorkshopVerificationRequest;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.enums.BookingTimeSlot;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.repository.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
public class AdminWorkshopManagementIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    @Autowired
    private WorkshopServiceRepository workshopServiceRepository;

    @Autowired
    private WorkshopWalletRepository workshopWalletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WorkshopPaymentRepository workshopPaymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private LeadOpportunityRepository leadOpportunityRepository;

    @Autowired
    private WorkshopJobRepository workshopJobRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User customerUser;
    private User partnerUserA;
    private User partnerUserB;

    private String adminToken;
    private String customerToken;
    private String partnerTokenA;

    private Workshop workshopA;
    private Workshop workshopB;
    private ServiceCatalog serviceCatalogA;
    private WorkshopService workshopServiceA;
    private WorkshopWallet walletA;
    private ServiceRequest serviceRequestA;
    private LeadOpportunity opportunityA;
    private WorkshopJob jobA;
    private Booking bookingA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Admin
        adminUser = new User();
        adminUser.setName("Admin Tester " + runId);
        adminUser.setEmail("adm_ws_" + runId + "@carservice.com");
        adminUser.setPhone("91" + Math.abs(runId.hashCode() % 100000000));
        if (adminUser.getPhone().length() < 10) adminUser.setPhone("9188" + runId.substring(0, 6));
        adminUser.setPassword(passwordEncoder.encode("Admin@12345"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);

        // 2. Create Customer
        customerUser = new User();
        customerUser.setName("Customer Tester " + runId);
        customerUser.setEmail("cust_ws_" + runId + "@carservice.com");
        customerUser.setPhone("92" + Math.abs(runId.hashCode() % 100000000));
        if (customerUser.getPhone().length() < 10) customerUser.setPhone("9288" + runId.substring(0, 6));
        customerUser.setPassword(passwordEncoder.encode("Cust@12345"));
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setIsActive(true);
        customerUser = userRepository.save(customerUser);
        customerToken = jwtService.generateAccessToken(customerUser);

        // 3. Create Partner A
        partnerUserA = new User();
        partnerUserA.setName("Partner Alpha " + runId);
        partnerUserA.setEmail("part_a_" + runId + "@carservice.com");
        partnerUserA.setPhone("93" + Math.abs(runId.hashCode() % 100000000));
        if (partnerUserA.getPhone().length() < 10) partnerUserA.setPhone("9388" + runId.substring(0, 6));
        partnerUserA.setPassword(passwordEncoder.encode("Part@12345"));
        partnerUserA.setRole(UserRole.PARTNER);
        partnerUserA.setIsActive(true);
        partnerUserA = userRepository.save(partnerUserA);
        partnerTokenA = jwtService.generateAccessToken(partnerUserA);

        // 4. Create Partner B
        partnerUserB = new User();
        partnerUserB.setName("Partner Beta " + runId);
        partnerUserB.setEmail("part_b_" + runId + "@carservice.com");
        partnerUserB.setPhone("94" + Math.abs(runId.hashCode() % 100000000));
        if (partnerUserB.getPhone().length() < 10) partnerUserB.setPhone("9488" + runId.substring(0, 6));
        partnerUserB.setPassword(passwordEncoder.encode("Part@12345"));
        partnerUserB.setRole(UserRole.PARTNER);
        partnerUserB.setIsActive(true);
        partnerUserB = userRepository.save(partnerUserB);

        // 5. Create Workshop A (Active, Verified, in New Delhi)
        workshopA = new Workshop();
        workshopA.setUser(partnerUserA);
        workshopA.setBusinessName("Alpha Garage Delhi " + runId);
        workshopA.setPhone(partnerUserA.getPhone());
        workshopA.setEmail(partnerUserA.getEmail());
        workshopA.setAddress("123 Barakhamba Road, Connaught Place");
        workshopA.setCity("New Delhi");
        workshopA.setState("Delhi");
        workshopA.setPincode("110001");
        workshopA.setLatitude(new BigDecimal("28.6300"));
        workshopA.setLongitude(new BigDecimal("77.2100"));
        workshopA.setServiceRadiusKm(new BigDecimal("20.00"));
        workshopA.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        workshopA.setIsActive(true);
        workshopA = workshopRepository.save(workshopA);

        // Create Wallet A
        walletA = new WorkshopWallet(workshopA, new BigDecimal("1500.00"));
        walletA = workshopWalletRepository.save(walletA);
        workshopA.setWallet(walletA);

        // Create a Wallet Transaction
        WalletTransaction wt = new WalletTransaction(
                walletA,
                workshopA,
                WalletTransactionType.CREDIT,
                WalletReferenceType.WALLET_TOPUP,
                new BigDecimal("1500.00"),
                BigDecimal.ZERO,
                new BigDecimal("1500.00"),
                "TOPUP-" + runId,
                "IDEMP-" + runId,
                "Initial topup credit"
        );
        walletTransactionRepository.save(wt);

        // 6. Create Workshop B (Pending, Inactive, in Bangalore)
        workshopB = new Workshop();
        workshopB.setUser(partnerUserB);
        workshopB.setBusinessName("Beta Auto Care BLR " + runId);
        workshopB.setPhone(partnerUserB.getPhone());
        workshopB.setEmail(partnerUserB.getEmail());
        workshopB.setAddress("456 Koramangala 5th Block");
        workshopB.setCity("Bangalore");
        workshopB.setState("Karnataka");
        workshopB.setPincode("560095");
        workshopB.setLatitude(new BigDecimal("12.9352"));
        workshopB.setLongitude(new BigDecimal("77.6245"));
        workshopB.setServiceRadiusKm(new BigDecimal("15.00"));
        workshopB.setVerificationStatus(WorkshopVerificationStatus.PENDING);
        workshopB.setIsActive(false);
        workshopB = workshopRepository.save(workshopB);

        // 7. Create Service Catalog & attach capability to Workshop A
        serviceCatalogA = new ServiceCatalog(
                "Periodic Full Service " + runId,
                "Comprehensive periodic service",
                ServiceCategory.PERIODIC_SERVICE,
                new BigDecimal("2999.00"),
                DiscountType.NO_DISCOUNT,
                BigDecimal.ZERO,
                180,
                true
        );
        serviceCatalogA = serviceCatalogRepository.save(serviceCatalogA);

        workshopServiceA = new WorkshopService(workshopA, serviceCatalogA, true);
        workshopServiceA = workshopServiceRepository.save(workshopServiceA);

        // 8. Create Vehicle for customer
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(customerUser);
        vehicle.setMake("Honda");
        vehicle.setModel("City");
        vehicle.setYear(2022);
        vehicle.setRegistrationNumber("DL" + runId.substring(0, 2).toUpperCase() + "77" + (int)(Math.random() * 8999 + 1000));
        vehicle.setFuelType(FuelType.PETROL);
        vehicle.setTransmission(Transmission.MANUAL);
        vehicle = vehicleRepository.save(vehicle);

        // 9. Create Service Request & Lead Opportunity for Workshop A
        serviceRequestA = new ServiceRequest();
        serviceRequestA.setRequestReference("SR-" + runId.toUpperCase());
        serviceRequestA.setUser(customerUser);
        serviceRequestA.setVehicle(vehicle);
        serviceRequestA.setCity("New Delhi");
        serviceRequestA.setAddress("12 Connaught Place");
        serviceRequestA.setPincode("110001");
        serviceRequestA.setPreferredDate(LocalDate.now().plusDays(2));
        serviceRequestA.setPreferredTimeSlot("10:00 AM - 12:00 PM");
        serviceRequestA.setTotalAmount(new BigDecimal("2999.00"));
        serviceRequestA.setStatus(ServiceRequestStatus.ACCEPTED);
        serviceRequestA.setAssignedWorkshop(workshopA);
        serviceRequestA = serviceRequestRepository.save(serviceRequestA);

        opportunityA = new LeadOpportunity(serviceRequestA, workshopA, new BigDecimal("150.00"));
        opportunityA.setStatus(OpportunityStatus.ACCEPTED);
        opportunityA = leadOpportunityRepository.save(opportunityA);

        // 10. Create Workshop Job for Workshop A
        jobA = new WorkshopJob(serviceRequestA, workshopA, opportunityA);
        jobA.setStatus(WorkshopJobStatus.ASSIGNED);
        jobA = workshopJobRepository.save(jobA);

        // 11. Create Booking assigned to Workshop A
        bookingA = new Booking();
        bookingA.setBookingReference("BK-" + runId.toUpperCase());
        bookingA.setUser(customerUser);
        bookingA.setVehicle(vehicle);
        bookingA.setService(serviceCatalogA);
        bookingA.setServiceNameSnapshot(serviceCatalogA.getName());
        bookingA.setServicePriceSnapshot(serviceCatalogA.getBasePrice());
        bookingA.setBookingDate(LocalDate.now().plusDays(3));
        bookingA.setBookingTime(LocalTime.of(10, 0));
        bookingA.setTimeSlot(BookingTimeSlot.SLOT_10_11.getDisplayLabel());
        bookingA.setStatus(BookingStatus.CONFIRMED);
        bookingA.setTotalAmount(new BigDecimal("2999.00"));
        bookingA.setEstimatedPrice(new BigDecimal("2999.00"));
        bookingA.setServiceRequest(serviceRequestA);
        bookingA = bookingRepository.save(bookingA);
    }

    // =========================================================================
    // 1. REGISTRY & PAGINATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Admin can list workshops with pagination")
    void testGetWorkshopsPaginated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Admin can retrieve workshop summary counters")
    void testGetWorkshopSummary() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalWorkshops", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.activeWorkshops", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.pendingVerificationWorkshops", greaterThanOrEqualTo(1)));
    }

    // =========================================================================
    // 2. SEARCH & FILTER TESTS
    // =========================================================================

    @Test
    @DisplayName("Admin can search workshops by business name")
    void testSearchWorkshopsByName() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", workshopA.getBusinessName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(workshopA.getId()))
                .andExpect(jsonPath("$.data.content[0].businessName").value(workshopA.getBusinessName()));
    }

    @Test
    @DisplayName("Admin can search workshops by city")
    void testSearchWorkshopsByCity() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "Bangalore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[0].city").value("Bangalore"));
    }

    @Test
    @DisplayName("Admin can search workshops by phone")
    void testSearchWorkshopsByPhone() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", workshopA.getPhone()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(workshopA.getId()));
    }

    @Test
    @DisplayName("Admin can filter workshops by operational status ACTIVE")
    void testFilterWorkshopsByOperationalStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[*].isActive", everyItem(is(true))));
    }

    @Test
    @DisplayName("Admin can filter workshops by verification status PENDING")
    void testFilterWorkshopsByVerificationStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("verificationStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[*].verificationStatus", everyItem(is("PENDING"))));
    }

    @Test
    @DisplayName("Admin can filter workshops by marketplace activity")
    void testFilterWorkshopsByActivity() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("activity", "HAS_ACCEPTED_OPPORTUNITIES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[*].acceptedOpportunitiesCount", everyItem(greaterThan(0))));
    }

    // =========================================================================
    // 3. WORKSHOP 360° & SUB-RESOURCE DOSSIER TESTS
    // =========================================================================

    @Test
    @DisplayName("Admin can view workshop 360° detail dossier")
    void testGetWorkshopDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops/" + workshopA.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(workshopA.getId()))
                .andExpect(jsonPath("$.data.businessName").value(workshopA.getBusinessName()))
                .andExpect(jsonPath("$.data.ownerName").value(partnerUserA.getName()))
                .andExpect(jsonPath("$.data.city").value("New Delhi"))
                .andExpect(jsonPath("$.data.state").value("Delhi"))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.walletBalance").value(1500.00))
                .andExpect(jsonPath("$.data.totalOpportunities").value(1))
                .andExpect(jsonPath("$.data.acceptedOpportunities").value(1))
                .andExpect(jsonPath("$.data.capabilities", not(empty())));
    }

    @Test
    @DisplayName("Admin can view workshop capabilities")
    void testGetWorkshopCapabilities() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops/" + workshopA.getId() + "/capabilities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].serviceCatalogId").value(serviceCatalogA.getId()))
                .andExpect(jsonPath("$.data[0].isCapabilityActive").value(true));
    }

    @Test
    @DisplayName("Admin can view workshop opportunities")
    void testGetWorkshopOpportunities() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops/" + workshopA.getId() + "/opportunities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(opportunityA.getId()))
                .andExpect(jsonPath("$.data.content[0].status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("Admin can view workshop bookings")
    void testGetWorkshopBookings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops/" + workshopA.getId() + "/bookings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].bookingReference").value(bookingA.getBookingReference()));
    }

    @Test
    @DisplayName("Admin can view workshop wallet")
    void testGetWorkshopWallet() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops/" + workshopA.getId() + "/wallet")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(1500.00))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Admin can view workshop audit events")
    void testGetWorkshopAuditEvents() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops/" + workshopA.getId() + "/audit-events")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 4. STATUS & VERIFICATION MANAGEMENT TESTS
    // =========================================================================

    @Test
    @DisplayName("Admin can update workshop operational status to INACTIVE with reason")
    void testUpdateOperationalStatusToInactive() throws Exception {
        UpdateWorkshopStatusRequest req = new UpdateWorkshopStatusRequest(false, "Routine compliance review underway");

        mockMvc.perform(patch("/api/v1/admin/workshops/" + workshopA.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.statusReason").value("Routine compliance review underway"));

        // Historical wallet & bookings remain intact
        Workshop updated = workshopRepository.findById(workshopA.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
        assertEquals("Routine compliance review underway", updated.getStatusReason());
        assertNotNull(updated.getWallet());
        assertEquals(new BigDecimal("1500.00"), updated.getWallet().getBalance());
    }

    @Test
    @DisplayName("Admin can reactivate an inactive workshop")
    void testReactivateWorkshop() throws Exception {
        UpdateWorkshopStatusRequest req = new UpdateWorkshopStatusRequest(true, "Compliance review completed");

        mockMvc.perform(patch("/api/v1/admin/workshops/" + workshopB.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("Updating operational status to same value is rejected with 400")
    void testUpdateStatusSameValueRejected() throws Exception {
        UpdateWorkshopStatusRequest req = new UpdateWorkshopStatusRequest(true, "Redundant activation");

        mockMvc.perform(patch("/api/v1/admin/workshops/" + workshopA.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Deactivating workshop without reason is rejected with 400")
    void testDeactivateWithoutReasonRejected() throws Exception {
        UpdateWorkshopStatusRequest req = new UpdateWorkshopStatusRequest(false, "  ");

        mockMvc.perform(patch("/api/v1/admin/workshops/" + workshopA.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Admin can approve workshop verification status to VERIFIED")
    void testApproveWorkshopVerification() throws Exception {
        UpdateWorkshopVerificationRequest req = new UpdateWorkshopVerificationRequest(WorkshopVerificationStatus.VERIFIED, "Documents approved");

        mockMvc.perform(patch("/api/v1/admin/workshops/" + workshopB.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));
    }

    @Test
    @DisplayName("Admin can suspend workshop verification with reason")
    void testSuspendWorkshopVerification() throws Exception {
        UpdateWorkshopVerificationRequest req = new UpdateWorkshopVerificationRequest(WorkshopVerificationStatus.SUSPENDED, "Multiple customer complaints");

        mockMvc.perform(patch("/api/v1/admin/workshops/" + workshopA.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    @DisplayName("Suspending workshop verification without reason is rejected with 400")
    void testSuspendVerificationWithoutReasonRejected() throws Exception {
        UpdateWorkshopVerificationRequest req = new UpdateWorkshopVerificationRequest(WorkshopVerificationStatus.SUSPENDED, "");

        mockMvc.perform(patch("/api/v1/admin/workshops/" + workshopA.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 5. SECURITY & AUTHORIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Unauthenticated request to Admin workshop endpoints returns 401")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Customer role cannot access Admin workshop endpoints (403)")
    void testCustomerAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Partner role cannot access Admin workshop endpoints (403)")
    void testPartnerAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-existent workshop returns 404 Not Found")
    void testNonExistentWorkshopReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/workshops/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}

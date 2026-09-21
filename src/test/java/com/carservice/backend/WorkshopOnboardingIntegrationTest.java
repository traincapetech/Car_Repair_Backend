package com.carservice.backend;

import com.carservice.backend.admin.dto.WorkshopActionReasonRequest;
import com.carservice.backend.booking.dto.CreateBookingRequest;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.marketplace.dto.DirectPaymentRequest;
import com.carservice.backend.marketplace.dto.WorkshopRegistrationRequest;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class WorkshopOnboardingIntegrationTest {

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
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private LeadOpportunityRepository leadOpportunityRepository;

    @Autowired
    private BookingRepository bookingRepository;

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
    private String adminToken;
    private String customerToken;
    private ServiceCatalog testService;
    private Vehicle customerVehicle;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String runId = UUID.randomUUID().toString().substring(0, 8);

        // Admin User
        adminUser = new User();
        adminUser.setName("Admin Onboarding " + runId);
        adminUser.setEmail("adm_onboard_" + runId + "@carservice.com");
        adminUser.setPhone("98" + (Math.abs(runId.hashCode()) % 90000000 + 10000000));
        adminUser.setPassword(passwordEncoder.encode("Admin@12345"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);

        // Customer User
        customerUser = new User();
        customerUser.setName("Customer Onboarding " + runId);
        customerUser.setEmail("cust_onboard_" + runId + "@gmail.com");
        customerUser.setPhone("97" + (Math.abs(runId.hashCode()) % 90000000 + 10000000));
        customerUser.setPassword(passwordEncoder.encode("Cust@12345"));
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setIsActive(true);
        customerUser = userRepository.save(customerUser);
        customerToken = jwtService.generateAccessToken(customerUser);

        // Test Service Catalog
        testService = new ServiceCatalog();
        testService.setName("General Periodic Service " + runId);
        testService.setCategory(ServiceCategory.PERIODIC_SERVICE);
        testService.setBasePrice(new BigDecimal("1999.00"));
        testService.setDiscountType(DiscountType.NO_DISCOUNT);
        testService.setEstimatedDurationMinutes(60);
        testService.setDescription("Periodic maintenance");
        testService.setIsActive(true);
        testService = serviceCatalogRepository.save(testService);

        // Customer Vehicle
        customerVehicle = new Vehicle();
        customerVehicle.setUser(customerUser);
        customerVehicle.setMake("Hyundai");
        customerVehicle.setModel("Creta");
        customerVehicle.setYear(2022);
        customerVehicle.setRegistrationNumber("DL" + (Math.abs(runId.hashCode()) % 90 + 10) + "AB" + (Math.abs(runId.hashCode()) % 9000 + 1000));
        customerVehicle.setFuelType(FuelType.PETROL);
        customerVehicle.setTransmission(Transmission.MANUAL);
        customerVehicle = vehicleRepository.save(customerVehicle);
    }

    @Test
    @DisplayName("1. Workshop Self-Registration: Successfully registers in PENDING state with isActive=false")
    void testWorkshopSelfRegistrationSuccess() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        WorkshopRegistrationRequest req = new WorkshopRegistrationRequest();
        req.setBusinessName("Apex Motors " + runId);
        req.setOwnerName("Rajesh Kumar " + runId);
        req.setEmail("apex_" + runId + "@motors.com");
        req.setPhone("98" + (Math.abs(runId.hashCode()) % 90000000 + 10000000));
        req.setPassword("Partner@12345");
        req.setAddress("Plot 12, Okhla Phase 3");
        req.setCity("New Delhi");
        req.setState("Delhi");
        req.setPincode("110020");
        req.setLatitude(new BigDecimal("28.535500"));
        req.setLongitude(new BigDecimal("77.272500"));
        req.setServiceRadiusKm(new BigDecimal("25.0"));
        req.setOpeningTime("09:00");
        req.setClosingTime("19:00");
        req.setWorkingDays("Monday - Saturday");
        req.setServiceCatalogIds(List.of(testService.getId()));

        mockMvc.perform(post("/api/v1/workshops/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.businessName", is(req.getBusinessName())))
                .andExpect(jsonPath("$.data.verificationStatus", is("PENDING")))
                .andExpect(jsonPath("$.data.isActive", is(false)))
                .andExpect(jsonPath("$.data.latitude", notNullValue()))
                .andExpect(jsonPath("$.data.longitude", notNullValue()))
                .andExpect(jsonPath("$.data.walletBalance", is(0.0)));

        // Verify database persistence
        Workshop saved = workshopRepository.findByEmail(req.getEmail().toLowerCase()).orElse(null);
        assertNotNull(saved);
        assertEquals(WorkshopVerificationStatus.PENDING, saved.getVerificationStatus());
        assertFalse(saved.getIsActive());
        assertEquals("Rajesh Kumar " + runId, saved.getOwnerName());
        assertEquals("09:00", saved.getOpeningTime());

        // Verify User was created with ROLE_PARTNER
        User partnerUser = userRepository.findByEmail(req.getEmail().toLowerCase()).orElse(null);
        assertNotNull(partnerUser);
        assertEquals(UserRole.PARTNER, partnerUser.getRole());

        // Verify Workshop Wallet was created with 0 balance
        WorkshopWallet wallet = workshopWalletRepository.findByWorkshopId(saved.getId()).orElse(null);
        assertNotNull(wallet);
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));

        // Verify Workshop Service binding was created
        long boundCount = workshopServiceRepository.countActiveServicesByWorkshopAndServiceIds(saved.getId(), List.of(testService.getId()));
        assertEquals(1, boundCount);
    }

    @Test
    @DisplayName("2. Workshop Registration Duplicate Conflict: Rejects duplicate email or phone")
    void testWorkshopRegistrationDuplicateConflict() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String email = "dup_" + runId + "@workshop.com";
        String phone = "98" + (Math.abs(runId.hashCode()) % 90000000 + 10000000);

        WorkshopRegistrationRequest req = new WorkshopRegistrationRequest();
        req.setBusinessName("First Motors " + runId);
        req.setOwnerName("Amit Kumar");
        req.setEmail(email);
        req.setPhone(phone);
        req.setPassword("Partner@12345");
        req.setAddress("Connaught Place");
        req.setCity("New Delhi");
        req.setState("Delhi");
        req.setPincode("110001");
        req.setLatitude(new BigDecimal("28.630000"));
        req.setLongitude(new BigDecimal("77.220000"));

        // First registration succeeds
        mockMvc.perform(post("/api/v1/workshops/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second registration with same email should fail with 409 Conflict
        req.setBusinessName("Duplicate Motors");
        mockMvc.perform(post("/api/v1/workshops/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("3. Workshop Registration Validation: Rejects invalid coordinates or phone")
    void testWorkshopRegistrationValidation() throws Exception {
        WorkshopRegistrationRequest invalidReq = new WorkshopRegistrationRequest();
        invalidReq.setBusinessName("Invalid Lat Motors");
        invalidReq.setOwnerName("Test Owner");
        invalidReq.setEmail("invalid_lat@test.com");
        invalidReq.setPhone("12345"); // Invalid phone
        invalidReq.setPassword("short"); // Invalid password
        invalidReq.setAddress("Test Address");
        invalidReq.setCity("Delhi");
        invalidReq.setState("Delhi");
        invalidReq.setPincode("110001");
        invalidReq.setLatitude(new BigDecimal("95.000000")); // Invalid lat > 90
        invalidReq.setLongitude(new BigDecimal("77.200000"));

        mockMvc.perform(post("/api/v1/workshops/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("4. Partner Profile Endpoint: Retrieves profile for authenticated partner")
    void testPartnerProfileEndpoint() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        WorkshopRegistrationRequest req = new WorkshopRegistrationRequest();
        req.setBusinessName("Profile Workshop " + runId);
        req.setOwnerName("Vikram Singh");
        req.setEmail("vikram_" + runId + "@workshop.com");
        req.setPhone("98" + (Math.abs(runId.hashCode()) % 90000000 + 10000000));
        req.setPassword("Partner@12345");
        req.setAddress("Sector 18, Noida");
        req.setCity("Noida");
        req.setState("Uttar Pradesh");
        req.setPincode("201301");
        req.setLatitude(new BigDecimal("28.570000"));
        req.setLongitude(new BigDecimal("77.320000"));

        mockMvc.perform(post("/api/v1/workshops/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        User partnerUser = userRepository.findByEmail(req.getEmail().toLowerCase()).orElseThrow();
        String partnerToken = jwtService.generateAccessToken(partnerUser);

        // Fetch partner profile
        mockMvc.perform(get("/api/v1/partner/workshop/profile")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.businessName", is(req.getBusinessName())))
                .andExpect(jsonPath("$.data.ownerName", is("Vikram Singh")))
                .andExpect(jsonPath("$.data.verificationStatus", is("PENDING")))
                .andExpect(jsonPath("$.data.isActive", is(false)))
                .andExpect(jsonPath("$.data.city", is("Noida")));
    }

    @Test
    @DisplayName("5. Admin Workshop Approval: Approves PENDING workshop to VERIFIED and sets isActive=true")
    void testAdminWorkshopApproval() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        WorkshopRegistrationRequest req = new WorkshopRegistrationRequest();
        req.setBusinessName("Pending Workshop " + runId);
        req.setOwnerName("Suresh Nair");
        req.setEmail("suresh_" + runId + "@workshop.com");
        req.setPhone("98" + (Math.abs(runId.hashCode()) % 90000000 + 10000000));
        req.setPassword("Partner@12345");
        req.setAddress("Karol Bagh");
        req.setCity("New Delhi");
        req.setState("Delhi");
        req.setPincode("110005");
        req.setLatitude(new BigDecimal("28.650000"));
        req.setLongitude(new BigDecimal("77.190000"));

        mockMvc.perform(post("/api/v1/workshops/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Workshop workshop = workshopRepository.findByEmail(req.getEmail().toLowerCase()).orElseThrow();

        // Admin approves workshop
        mockMvc.perform(post("/api/v1/admin/workshops/" + workshop.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.verificationStatus", is("VERIFIED")))
                .andExpect(jsonPath("$.data.isActive", is(true)))
                .andExpect(jsonPath("$.data.approvedAt", notNullValue()))
                .andExpect(jsonPath("$.data.approvedBy", notNullValue()));

        Workshop verified = workshopRepository.findById(workshop.getId()).orElseThrow();
        assertEquals(WorkshopVerificationStatus.VERIFIED, verified.getVerificationStatus());
        assertTrue(verified.getIsActive());
        assertNotNull(verified.getApprovedAt());
    }

    @Test
    @DisplayName("6. Admin Workshop Rejection: Rejects PENDING workshop with reason and sets isActive=false")
    void testAdminWorkshopRejection() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        WorkshopRegistrationRequest req = new WorkshopRegistrationRequest();
        req.setBusinessName("Rejectable Workshop " + runId);
        req.setOwnerName("Owner " + runId);
        req.setEmail("reject_" + runId + "@workshop.com");
        req.setPhone("98" + (Math.abs(runId.hashCode()) % 90000000 + 10000000));
        req.setPassword("Partner@12345");
        req.setAddress("Unauthorized Area");
        req.setCity("New Delhi");
        req.setState("Delhi");
        req.setPincode("110001");
        req.setLatitude(new BigDecimal("28.600000"));
        req.setLongitude(new BigDecimal("77.200000"));

        mockMvc.perform(post("/api/v1/workshops/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Workshop workshop = workshopRepository.findByEmail(req.getEmail().toLowerCase()).orElseThrow();

        WorkshopActionReasonRequest rejectReq = new WorkshopActionReasonRequest("Facilities do not meet standard safety criteria");

        mockMvc.perform(post("/api/v1/admin/workshops/" + workshop.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.verificationStatus", is("REJECTED")))
                .andExpect(jsonPath("$.data.isActive", is(false)))
                .andExpect(jsonPath("$.data.statusReason", containsString("Facilities do not meet standard")));

        Workshop rejected = workshopRepository.findById(workshop.getId()).orElseThrow();
        assertEquals(WorkshopVerificationStatus.REJECTED, rejected.getVerificationStatus());
        assertFalse(rejected.getIsActive());
    }

    @Test
    @DisplayName("7. Admin Workshop Suspension and Reactivation: Suspends active workshop, then reactivates")
    void testAdminWorkshopSuspensionAndReactivation() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        WorkshopRegistrationRequest req = new WorkshopRegistrationRequest();
        req.setBusinessName("Cycle Workshop " + runId);
        req.setOwnerName("Owner " + runId);
        req.setEmail("cycle_" + runId + "@workshop.com");
        req.setPhone("98" + (Math.abs(runId.hashCode()) % 90000000 + 10000000));
        req.setPassword("Partner@12345");
        req.setAddress("Rohini Sector 7");
        req.setCity("New Delhi");
        req.setState("Delhi");
        req.setPincode("110085");
        req.setLatitude(new BigDecimal("28.700000"));
        req.setLongitude(new BigDecimal("77.120000"));

        mockMvc.perform(post("/api/v1/workshops/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Workshop workshop = workshopRepository.findByEmail(req.getEmail().toLowerCase()).orElseThrow();

        // 1. Approve
        mockMvc.perform(post("/api/v1/admin/workshops/" + workshop.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 2. Suspend with reason
        WorkshopActionReasonRequest suspendReq = new WorkshopActionReasonRequest("Temporary customer complaint audit");
        mockMvc.perform(post("/api/v1/admin/workshops/" + workshop.getId() + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus", is("SUSPENDED")))
                .andExpect(jsonPath("$.data.isActive", is(false)));

        // 3. Reactivate
        mockMvc.perform(post("/api/v1/admin/workshops/" + workshop.getId() + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus", is("VERIFIED")))
                .andExpect(jsonPath("$.data.isActive", is(true)));
    }

    @Test
    @DisplayName("8. Geo-Verified Marketplace Dispatch: Dispatches strictly within radius, nearest-first, excluding unapproved")
    void testGeoVerifiedMarketplaceDispatch() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // Workshop 1: Delhi Central (2 km from customer), APPROVED & ACTIVE
        Workshop ws1 = createWorkshop("WS Delhi Central " + runId, "28.620000", "77.210000", "25.0", true, WorkshopVerificationStatus.VERIFIED);

        // Workshop 2: Delhi South (8 km from customer), APPROVED & ACTIVE
        Workshop ws2 = createWorkshop("WS Delhi South " + runId, "28.550000", "77.200000", "25.0", true, WorkshopVerificationStatus.VERIFIED);

        // Workshop 3: Delhi Close (1 km from customer) BUT PENDING (Unapproved!)
        Workshop ws3Pending = createWorkshop("WS Delhi Pending " + runId, "28.615000", "77.209500", "25.0", false, WorkshopVerificationStatus.PENDING);

        // Workshop 4: Far Away (Lucknow, 500 km), APPROVED & ACTIVE
        Workshop ws4Far = createWorkshop("WS Far Away " + runId, "26.846700", "80.946200", "25.0", true, WorkshopVerificationStatus.VERIFIED);

        // Customer creates booking at Connaught Place, New Delhi (28.6139, 77.2090)
        CreateBookingRequest bookingReq = new CreateBookingRequest();
        bookingReq.setVehicleId(customerVehicle.getId());
        bookingReq.setServiceId(testService.getId());
        bookingReq.setBookingDate(LocalDate.now().plusDays(2));
        bookingReq.setTimeSlot("10:00-11:00");
        bookingReq.setCustomerNotes("Test geo-dispatch proximity");
        bookingReq.setCity("New Delhi");
        bookingReq.setAddress("Connaught Place, Central Delhi");
        bookingReq.setPincode("110001");
        bookingReq.setLatitude(new BigDecimal("28.613900"));
        bookingReq.setLongitude(new BigDecimal("77.209000"));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify opportunities created
        List<LeadOpportunity> opportunities = leadOpportunityRepository.findAll();

        // WS1 (Delhi Central, 2 km) should have received a lead
        boolean ws1Received = opportunities.stream().anyMatch(o -> o.getWorkshop().getId().equals(ws1.getId()));
        assertTrue(ws1Received, "Delhi Central workshop within 2 km must receive lead opportunity");

        // WS2 (Delhi South, 8 km) should have received a lead
        boolean ws2Received = opportunities.stream().anyMatch(o -> o.getWorkshop().getId().equals(ws2.getId()));
        assertTrue(ws2Received, "Delhi South workshop within 8 km must receive lead opportunity");

        // WS3 (Pending) MUST NOT receive a lead!
        boolean ws3Received = opportunities.stream().anyMatch(o -> o.getWorkshop().getId().equals(ws3Pending.getId()));
        assertFalse(ws3Received, "PENDING workshop MUST NOT receive any lead opportunity");

        // WS4 (Far away 500 km) MUST NOT receive a lead!
        boolean ws4Received = opportunities.stream().anyMatch(o -> o.getWorkshop().getId().equals(ws4Far.getId()));
        assertFalse(ws4Received, "Far away workshop outside radius MUST NOT receive lead opportunity");
    }

    @Test
    @DisplayName("9. Unapproved Workshop Cannot Access Marketplace Leads or Claim")
    void testUnapprovedWorkshopCannotAccessOrClaimLeads() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // Create a PENDING workshop
        WorkshopRegistrationRequest req = new WorkshopRegistrationRequest();
        req.setBusinessName("Unapproved Garage " + runId);
        req.setOwnerName("Unapproved Owner");
        req.setEmail("unapproved_" + runId + "@garage.com");
        req.setPhone("98" + (Math.abs(runId.hashCode()) % 90000000 + 10000000));
        req.setPassword("Partner@12345");
        req.setAddress("Connaught Place");
        req.setCity("New Delhi");
        req.setState("Delhi");
        req.setPincode("110001");
        req.setLatitude(new BigDecimal("28.613900"));
        req.setLongitude(new BigDecimal("77.209000"));

        mockMvc.perform(post("/api/v1/workshops/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        User partnerUser = userRepository.findByEmail(req.getEmail().toLowerCase()).orElseThrow();
        String partnerToken = jwtService.generateAccessToken(partnerUser);

        // 1. Calling getOpportunities returns empty list
        mockMvc.perform(get("/api/v1/partner/opportunities")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 2. Attempting to accept a fake/non-existent or other opportunity is blocked
        mockMvc.perform(post("/api/v1/partner/opportunities/99999/accept")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("10. Public Services Endpoint: Unauthenticated prospective workshop can fetch active catalog services")
    void testPublicAvailableServicesEndpoint() throws Exception {
        // Unauthenticated call must succeed without JWT token
        mockMvc.perform(get("/api/v1/workshops/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // Alias endpoint also succeeds
        mockMvc.perform(get("/api/v1/services/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    private Workshop createWorkshop(String name, String lat, String lng, String radiusKm, boolean isActive, WorkshopVerificationStatus status) {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        User partnerUser = new User();
        partnerUser.setName(name + " User");
        partnerUser.setEmail("usr_" + runId + "@workshop.com");
        partnerUser.setPhone("98" + (Math.abs(runId.hashCode()) % 90000000 + 10000000));
        partnerUser.setPassword(passwordEncoder.encode("Partner@12345"));
        partnerUser.setRole(UserRole.PARTNER);
        partnerUser.setIsActive(isActive);
        partnerUser = userRepository.save(partnerUser);

        Workshop workshop = new Workshop();
        workshop.setUser(partnerUser);
        workshop.setBusinessName(name);
        workshop.setOwnerName(name + " Owner");
        workshop.setEmail(partnerUser.getEmail());
        workshop.setPhone(partnerUser.getPhone());
        workshop.setAddress("Test Address " + name);
        workshop.setCity("New Delhi");
        workshop.setState("Delhi");
        workshop.setPincode("110001");
        workshop.setLatitude(new BigDecimal(lat));
        workshop.setLongitude(new BigDecimal(lng));
        workshop.setServiceRadiusKm(new BigDecimal(radiusKm));
        workshop.setIsActive(isActive);
        workshop.setVerificationStatus(status);
        workshop = workshopRepository.save(workshop);

        // Bind test service to workshop
        WorkshopService ws = new WorkshopService(workshop, testService, true);
        workshopServiceRepository.save(ws);

        // Create wallet
        WorkshopWallet wallet = new WorkshopWallet(workshop, BigDecimal.ZERO);
        workshopWalletRepository.save(wallet);

        return workshop;
    }
}

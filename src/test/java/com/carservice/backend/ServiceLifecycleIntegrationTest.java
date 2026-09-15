package com.carservice.backend;

import com.carservice.backend.booking.dto.CreateBookingRequest;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.marketplace.dto.UpdateJobStatusRequest;
import com.carservice.backend.marketplace.dto.WalletClaimPaymentRequest;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.payment.FakePaymentGateway;
import com.carservice.backend.marketplace.payment.PaymentGateway;
import com.carservice.backend.marketplace.repository.*;
import com.carservice.backend.marketplace.service.PlatformConfigService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "razorpay.enabled=true",
        "razorpay.key-id=rzp_test_testkey123",
        "razorpay.key-secret=secret_test_key_xyz_456",
        "razorpay.webhook-secret=webhook_secret_test_789",
        "razorpay.mock-gateway=true"
})
public class ServiceLifecycleIntegrationTest {

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
    private WorkshopRepository workshopRepository;

    @Autowired
    private WorkshopServiceRepository workshopServiceRepository;

    @Autowired
    private WorkshopWalletRepository workshopWalletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ServiceRequestItemRepository serviceRequestItemRepository;

    @Autowired
    private LeadOpportunityRepository leadOpportunityRepository;

    @Autowired
    private WorkshopPaymentRepository workshopPaymentRepository;

    @Autowired
    private WorkshopJobRepository workshopJobRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private MarketplaceAuditEventRepository marketplaceAuditEventRepository;

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private PaymentGateway paymentGateway;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        platformConfigService.updateLeadAcceptanceFee(new BigDecimal("99.00"));

        if (paymentGateway instanceof FakePaymentGateway fake) {
            fake.setAvailable(true);
            fake.setSimulateOrderFailure(false);
            fake.setSimulateRefundFailure(false);
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private User createCustomer(String prefix) {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setName(prefix + " Customer " + uid);
        user.setEmail(prefix.toLowerCase() + "." + uid + "@customer.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(UserRole.CUSTOMER);
        user.setPhone("98" + uid.replaceAll("[^0-9]", "1").substring(0, 8));
        return userRepository.save(user);
    }

    private Vehicle createVehicle(User owner) {
        String reg = "DL01" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(owner);
        vehicle.setMake("Honda");
        vehicle.setModel("City");
        vehicle.setYear(2022);
        vehicle.setRegistrationNumber(reg);
        vehicle.setFuelType(FuelType.PETROL);
        vehicle.setTransmission(Transmission.AUTOMATIC);
        return vehicleRepository.save(vehicle);
    }

    private ServiceCatalog createService(String name, BigDecimal basePrice, DiscountType discountType, BigDecimal discountValue) {
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setName(name + " " + UUID.randomUUID().toString().substring(0, 5));
        catalog.setDescription("High quality " + name);
        catalog.setCategory(ServiceCategory.PERIODIC_SERVICE);
        catalog.setBasePrice(basePrice);
        catalog.setDiscountType(discountType);
        catalog.setDiscountValue(discountValue);
        catalog.setEstimatedDurationMinutes(60);
        catalog.setIsActive(true);
        return serviceCatalogRepository.save(catalog);
    }

    private Workshop createWorkshopWithWallet(String name, String city, BigDecimal lat, BigDecimal lon, BigDecimal walletBalance) {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setName(name + " Owner");
        user.setEmail("owner." + uid + "@workshop.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(UserRole.PARTNER);
        user.setPhone("97" + uid.replaceAll("[^0-9]", "2").substring(0, 8));
        User savedUser = userRepository.save(user);

        Workshop workshop = new Workshop();
        workshop.setUser(savedUser);
        workshop.setBusinessName(name + " " + uid);
        workshop.setCity(city);
        workshop.setAddress("Industrial Area, " + city);
        workshop.setPincode("110001");
        workshop.setLatitude(lat);
        workshop.setLongitude(lon);
        workshop.setPhone(savedUser.getPhone());
        workshop.setEmail(savedUser.getEmail());
        workshop.setState(city);
        workshop.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        workshop.setIsActive(true);
        Workshop savedWorkshop = workshopRepository.save(workshop);

        WorkshopWallet wallet = new WorkshopWallet(savedWorkshop, walletBalance);
        workshopWalletRepository.save(wallet);

        return savedWorkshop;
    }

    private void linkWorkshopService(Workshop workshop, ServiceCatalog service) {
        WorkshopService ws = new WorkshopService(workshop, service, true);
        workshopServiceRepository.save(ws);
    }

    // ==========================================
    // TEST 1: Multi-service booking creates linked ServiceRequest & snapshots
    // ==========================================
    @Test
    @DisplayName("Test 1: Multi-service booking creates linked ServiceRequest with authoritative snapshots")
    void test01_multiServiceBooking_createsMatchingServiceRequestAndAuthoritativeSnapshots() throws Exception {
        User customer = createCustomer("Multi");
        Vehicle vehicle = createVehicle(customer);

        ServiceCatalog s1 = createService("Full Service", new BigDecimal("3000.00"), DiscountType.PERCENTAGE, new BigDecimal("10.00")); // 2700
        ServiceCatalog s2 = createService("Wheel Alignment", new BigDecimal("800.00"), DiscountType.FIXED_AMOUNT, new BigDecimal("100.00")); // 700

        Workshop delhiWorkshop = createWorkshopWithWallet("Apex Delhi", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("1000.00"));
        linkWorkshopService(delhiWorkshop, s1);
        linkWorkshopService(delhiWorkshop, s2);

        String token = jwtService.generateAccessToken(customer);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicle.getId());
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("10:00-11:00");
        req.setServiceIds(List.of(s1.getId(), s2.getId()));
        req.setCity("New Delhi");
        req.setAddress("Connaught Place, New Delhi");
        req.setPincode("110001");
        req.setLatitude(new BigDecimal("28.6139"));
        req.setLongitude(new BigDecimal("77.2090"));
        req.setCustomerNotes("Customer notes for multi service");

        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingReference").isNotEmpty())
                .andExpect(jsonPath("$.data.serviceRequestReference").isNotEmpty())
                .andExpect(jsonPath("$.data.totalAmount").value(3400.00))
                .andExpect(jsonPath("$.data.services", hasSize(2)))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Long bookingId = objectMapper.readTree(json).get("data").get("id").asLong();

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertNotNull(booking.getServiceRequestReference());
        ServiceRequest sr = serviceRequestRepository.findByRequestReference(booking.getServiceRequestReference()).orElseThrow();
        assertEquals(new BigDecimal("3400.00"), sr.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        List<ServiceRequestItem> items = serviceRequestItemRepository.findByServiceRequestId(sr.getId());
        assertEquals(2, items.size());
        assertEquals("New Delhi", sr.getCity());

        // Opportunity should have been created for delhiWorkshop
        List<LeadOpportunity> opps = leadOpportunityRepository.findByServiceRequestId(sr.getId());
        assertFalse(opps.isEmpty());
        assertEquals(delhiWorkshop.getId(), opps.get(0).getWorkshop().getId());
    }

    // ==========================================
    // TEST 2: Strict Location Matching (Delhi vs Mumbai/Bangalore)
    // ==========================================
    @Test
    @DisplayName("Test 2: Strict Location Matching - Delhi customer strictly does NOT match Mumbai or Bangalore")
    void test02_locationMatching_strictCoordinateRadius_noMatchForDistantCities() throws Exception {
        User customer = createCustomer("Location");
        Vehicle vehicle = createVehicle(customer);

        ServiceCatalog service = createService("General Service", new BigDecimal("2000.00"), DiscountType.NO_DISCOUNT, BigDecimal.ZERO);

        // Delhi workshop (~1 km from customer)
        Workshop delhiWorkshop = createWorkshopWithWallet("Delhi Hub", "New Delhi", new BigDecimal("28.6200"), new BigDecimal("77.2100"), new BigDecimal("500.00"));
        linkWorkshopService(delhiWorkshop, service);

        // Mumbai workshop (~1150 km away)
        Workshop mumbaiWorkshop = createWorkshopWithWallet("Mumbai Marine", "Mumbai", new BigDecimal("19.0760"), new BigDecimal("72.8777"), new BigDecimal("500.00"));
        linkWorkshopService(mumbaiWorkshop, service);

        // Bangalore workshop (~1740 km away)
        Workshop bangaloreWorkshop = createWorkshopWithWallet("Bangalore Tech", "Bangalore", new BigDecimal("12.9716"), new BigDecimal("77.5946"), new BigDecimal("500.00"));
        linkWorkshopService(bangaloreWorkshop, service);

        String token = jwtService.generateAccessToken(customer);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicle.getId());
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("10:00-11:00");
        req.setServiceIds(List.of(service.getId()));
        req.setCity("New Delhi");
        req.setAddress("Barakhamba Road, New Delhi");
        req.setPincode("110001");
        req.setLatitude(new BigDecimal("28.6139"));
        req.setLongitude(new BigDecimal("77.2090"));

        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Long bookingId = objectMapper.readTree(json).get("data").get("id").asLong();

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        ServiceRequest sr = booking.getServiceRequest();
        assertNotNull(sr);

        List<LeadOpportunity> opportunities = leadOpportunityRepository.findByServiceRequestId(sr.getId());

        // Delhi workshop received an opportunity
        boolean delhiMatched = opportunities.stream().anyMatch(o -> o.getWorkshop().getId().equals(delhiWorkshop.getId()));
        assertTrue(delhiMatched, "Delhi workshop must be matched");

        // Mumbai and Bangalore workshops must NEVER match
        boolean mumbaiMatched = opportunities.stream().anyMatch(o -> o.getWorkshop().getId().equals(mumbaiWorkshop.getId()));
        boolean bangaloreMatched = opportunities.stream().anyMatch(o -> o.getWorkshop().getId().equals(bangaloreWorkshop.getId()));

        assertFalse(mumbaiMatched, "Mumbai workshop must NEVER match a Delhi customer");
        assertFalse(bangaloreMatched, "Bangalore workshop must NEVER match a Delhi customer");
    }

    // ==========================================
    // TEST 3 & 4: Capability and Status Filtering
    // ==========================================
    @Test
    @DisplayName("Test 3 & 4: Capability & Active status filtering")
    void test03_test04_capabilityAndStatusFiltering() throws Exception {
        User customer = createCustomer("Filter");
        Vehicle vehicle = createVehicle(customer);

        ServiceCatalog serviceA = createService("Brake Overhaul", new BigDecimal("2500.00"), DiscountType.NO_DISCOUNT, BigDecimal.ZERO);
        ServiceCatalog serviceB = createService("AC Overhaul", new BigDecimal("3500.00"), DiscountType.NO_DISCOUNT, BigDecimal.ZERO);

        // Workshop 1: Active, offers serviceA
        Workshop wActiveA = createWorkshopWithWallet("Active A", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));
        linkWorkshopService(wActiveA, serviceA);

        // Workshop 2: Active, offers ONLY serviceB (lacks serviceA)
        Workshop wActiveB = createWorkshopWithWallet("Active B", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));
        linkWorkshopService(wActiveB, serviceB);

        // Workshop 3: Inactive, offers serviceA
        Workshop wInactive = createWorkshopWithWallet("Inactive Hub", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));
        wInactive.setIsActive(false);
        workshopRepository.save(wInactive);
        linkWorkshopService(wInactive, serviceA);

        // Workshop 4: Suspended, offers serviceA
        Workshop wSuspended = createWorkshopWithWallet("Suspended Hub", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));
        wSuspended.setVerificationStatus(WorkshopVerificationStatus.SUSPENDED);
        workshopRepository.save(wSuspended);
        linkWorkshopService(wSuspended, serviceA);

        String token = jwtService.generateAccessToken(customer);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicle.getId());
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("10:00-11:00");
        req.setServiceIds(List.of(serviceA.getId()));
        req.setCity("New Delhi");

        MvcResult result = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Long bookingId = objectMapper.readTree(json).get("data").get("id").asLong();

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        ServiceRequest sr = booking.getServiceRequest();

        List<LeadOpportunity> opps = leadOpportunityRepository.findByServiceRequestId(sr.getId());
        assertEquals(1, opps.size());
        assertEquals(wActiveA.getId(), opps.get(0).getWorkshop().getId());
    }

    // ==========================================
    // TEST 5: Privacy Masking before Claim
    // ==========================================
    @Test
    @DisplayName("Test 5: Lead opportunity contains masked customer info before payment")
    void test05_leadOpportunityMasking_customerPersonalDetailsHiddenBeforeClaim() throws Exception {
        User customer = createCustomer("Privacy");
        Vehicle vehicle = createVehicle(customer);

        ServiceCatalog service = createService("Inspection", new BigDecimal("1000.00"), DiscountType.NO_DISCOUNT, BigDecimal.ZERO);
        Workshop workshop = createWorkshopWithWallet("Privacy Workshop", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));
        linkWorkshopService(workshop, service);

        String customerToken = jwtService.generateAccessToken(customer);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicle.getId());
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("10:00-11:00");
        req.setServiceIds(List.of(service.getId()));
        req.setCity("New Delhi");
        req.setAddress("123 Private Street, Block C");
        req.setPincode("110001");

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Booking booking = bookingRepository.findAllByUserIdOrderByCreatedAtDesc(customer.getId()).get(0);
        ServiceRequest sr = booking.getServiceRequest();
        LeadOpportunity opp = leadOpportunityRepository.findByServiceRequestId(sr.getId()).get(0);

        String workshopToken = jwtService.generateAccessToken(workshop.getUser());

        mockMvc.perform(get("/api/v1/partner/opportunities/" + opp.getId())
                        .header("Authorization", "Bearer " + workshopToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.name").value(containsString("***")))
                .andExpect(jsonPath("$.data.customerProfile.phone").value(containsString("***")))
                .andExpect(jsonPath("$.data.customerProfile.address").value(containsString("***")));
    }

    // ==========================================
    // TEST 6 & 8: Atomic Claim with Wallet, Job ASSIGNED, and Complete Floor Lifecycle
    // ==========================================
    @Test
    @DisplayName("Test 6 & 8: Atomic Claim with Wallet -> ASSIGNED -> Floor Lifecycle to COMPLETED")
    void test06_test08_atomicClaimWithWallet_andSequentialJobFloorLifecycle() throws Exception {
        User customer = createCustomer("Lifecycle");
        Vehicle vehicle = createVehicle(customer);

        ServiceCatalog service = createService("Full Engine Check", new BigDecimal("4000.00"), DiscountType.NO_DISCOUNT, BigDecimal.ZERO);
        Workshop workshop = createWorkshopWithWallet("Master Workshop", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));
        linkWorkshopService(workshop, service);

        String customerToken = jwtService.generateAccessToken(customer);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicle.getId());
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("10:00-11:00");
        req.setServiceIds(List.of(service.getId()));
        req.setCity("New Delhi");

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Booking booking = bookingRepository.findAllByUserIdOrderByCreatedAtDesc(customer.getId()).get(0);
        ServiceRequest sr = booking.getServiceRequest();
        LeadOpportunity opp = leadOpportunityRepository.findByServiceRequestId(sr.getId()).get(0);

        String workshopToken = jwtService.generateAccessToken(workshop.getUser());

        // Claim with Wallet
        WalletClaimPaymentRequest claimReq = new WalletClaimPaymentRequest(opp.getId(), "IDEMP-" + UUID.randomUUID());
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify WorkshopJob is created in status ASSIGNED
        WorkshopJob job = workshopJobRepository.findByServiceRequestId(sr.getId()).orElseThrow();
        assertEquals(WorkshopJobStatus.ASSIGNED, job.getStatus());
        assertEquals(workshop.getId(), job.getWorkshop().getId());

        // Verify linked booking is CONFIRMED
        Booking refreshedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, refreshedBooking.getStatus());

        // Floor Stage 1: ASSIGNED -> CONFIRMED
        UpdateJobStatusRequest statusReq = new UpdateJobStatusRequest(WorkshopJobStatus.CONFIRMED, "Booking slot confirmed with technician");
        mockMvc.perform(put("/api/v1/workshops/jobs/" + job.getId() + "/status")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // Floor Stage 2: CONFIRMED -> VEHICLE_RECEIVED
        statusReq = new UpdateJobStatusRequest(WorkshopJobStatus.VEHICLE_RECEIVED, "Car arrived at bay 4");
        mockMvc.perform(put("/api/v1/workshops/jobs/" + job.getId() + "/status")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VEHICLE_RECEIVED"));

        // Floor Stage 3: VEHICLE_RECEIVED -> INSPECTION
        statusReq = new UpdateJobStatusRequest(WorkshopJobStatus.INSPECTION, "Performing 40-point diagnostics");
        mockMvc.perform(put("/api/v1/workshops/jobs/" + job.getId() + "/status")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INSPECTION"));

        // Floor Stage 4: INSPECTION -> WORK_IN_PROGRESS
        statusReq = new UpdateJobStatusRequest(WorkshopJobStatus.WORK_IN_PROGRESS, "Parts replaced and filters serviced");
        mockMvc.perform(put("/api/v1/workshops/jobs/" + job.getId() + "/status")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WORK_IN_PROGRESS"));

        // Floor Stage 5: WORK_IN_PROGRESS -> READY_FOR_DELIVERY
        statusReq = new UpdateJobStatusRequest(WorkshopJobStatus.READY_FOR_DELIVERY, "Wash and detailing completed, ready for customer handover");
        mockMvc.perform(put("/api/v1/workshops/jobs/" + job.getId() + "/status")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY_FOR_DELIVERY"));

        // Floor Stage 6: READY_FOR_DELIVERY -> COMPLETED
        statusReq = new UpdateJobStatusRequest(WorkshopJobStatus.COMPLETED, "Handed over to customer");
        mockMvc.perform(put("/api/v1/workshops/jobs/" + job.getId() + "/status")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Check overall ServiceRequest status became COMPLETED
        ServiceRequest completedSr = serviceRequestRepository.findById(sr.getId()).orElseThrow();
        assertEquals(ServiceRequestStatus.COMPLETED, completedSr.getStatus());
    }

    // ==========================================
    // TEST 9 & 10: Invalid transitions blocked and multi-tenant security
    // ==========================================
    @Test
    @DisplayName("Test 9 & 10: Invalid transitions blocked & workshop multi-tenant security")
    void test09_test10_invalidTransitionsBlockedAndMultiTenantSecurity() throws Exception {
        User customer = createCustomer("Security");
        Vehicle vehicle = createVehicle(customer);

        ServiceCatalog service = createService("Oil Change", new BigDecimal("1200.00"), DiscountType.NO_DISCOUNT, BigDecimal.ZERO);
        Workshop workshopA = createWorkshopWithWallet("Workshop Alpha", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));
        Workshop workshopB = createWorkshopWithWallet("Workshop Beta", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));

        linkWorkshopService(workshopA, service);
        linkWorkshopService(workshopB, service);

        String customerToken = jwtService.generateAccessToken(customer);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicle.getId());
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("10:00-11:00");
        req.setServiceIds(List.of(service.getId()));
        req.setCity("New Delhi");

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Booking booking = bookingRepository.findAllByUserIdOrderByCreatedAtDesc(customer.getId()).get(0);
        ServiceRequest sr = booking.getServiceRequest();
        LeadOpportunity opp = leadOpportunityRepository.findByServiceRequestId(sr.getId()).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        String tokenA = jwtService.generateAccessToken(workshopA.getUser());
        String tokenB = jwtService.generateAccessToken(workshopB.getUser());

        // Workshop A claims opportunity
        WalletClaimPaymentRequest claimReq = new WalletClaimPaymentRequest(opp.getId(), "IDEMP-" + UUID.randomUUID());
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimReq)))
                .andExpect(status().isOk());

        WorkshopJob job = workshopJobRepository.findByServiceRequestId(sr.getId()).orElseThrow();

        // 1. Invalid jump: ASSIGNED -> COMPLETED directly must be blocked
        UpdateJobStatusRequest invalidJump = new UpdateJobStatusRequest(WorkshopJobStatus.COMPLETED, "Trying to skip stages");
        mockMvc.perform(put("/api/v1/workshops/jobs/" + job.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidJump)))
                .andExpect(status().isBadRequest());

        // 2. Multi-tenant security: Workshop B cannot modify Workshop A's job
        UpdateJobStatusRequest unauthorizedReq = new UpdateJobStatusRequest(WorkshopJobStatus.CONFIRMED, "Hacking stage");
        mockMvc.perform(put("/api/v1/workshops/jobs/" + job.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedReq)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // TEST 11: Customer Tracking Endpoint
    // ==========================================
    @Test
    @DisplayName("Test 11: Customer Tracking endpoint returns friendly titles and stage progression")
    void test11_customerTrackingEndpoint() throws Exception {
        User customer = createCustomer("Tracker");
        Vehicle vehicle = createVehicle(customer);

        ServiceCatalog service = createService("Brake Pad Replacement", new BigDecimal("1800.00"), DiscountType.NO_DISCOUNT, BigDecimal.ZERO);
        Workshop workshop = createWorkshopWithWallet("Elite Care", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));
        linkWorkshopService(workshop, service);

        String customerToken = jwtService.generateAccessToken(customer);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicle.getId());
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("10:00-11:00");
        req.setServiceIds(List.of(service.getId()));
        req.setCity("New Delhi");

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Booking booking = bookingRepository.findAllByUserIdOrderByCreatedAtDesc(customer.getId()).get(0);
        ServiceRequest sr = booking.getServiceRequest();
        LeadOpportunity opp = leadOpportunityRepository.findByServiceRequestId(sr.getId()).get(0);

        String workshopToken = jwtService.generateAccessToken(workshop.getUser());

        // Workshop claims
        WalletClaimPaymentRequest claimReq = new WalletClaimPaymentRequest(opp.getId(), "IDEMP-" + UUID.randomUUID());
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimReq)))
                .andExpect(status().isOk());

        WorkshopJob job = workshopJobRepository.findByServiceRequestId(sr.getId()).orElseThrow();

        // Advance to WORK_IN_PROGRESS
        job.setStatus(WorkshopJobStatus.WORK_IN_PROGRESS);
        workshopJobRepository.save(job);

        // Customer calls tracking endpoint
        mockMvc.perform(get("/api/v1/customer/tracking/" + booking.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobStatus").value("WORK_IN_PROGRESS"))
                .andExpect(jsonPath("$.data.stageNumber").value(5))
                .andExpect(jsonPath("$.data.stageTitle").value("Service in progress"))
                .andExpect(jsonPath("$.data.assignedWorkshopName").value(workshop.getBusinessName()))
                .andExpect(jsonPath("$.data.isCancellable").value(false));
    }

    // ==========================================
    // TEST 12: Cancellation allowed before vehicle intake with automatic refund
    // ==========================================
    @Test
    @DisplayName("Test 12: Customer cancellation allowed when CONFIRMED and triggers automated workshop refund")
    void test12_cancellationRules_allowedBeforeVehicleReceived_withAutomatedRefund() throws Exception {
        User customer = createCustomer("CancelPre");
        Vehicle vehicle = createVehicle(customer);

        ServiceCatalog service = createService("AC Service", new BigDecimal("2200.00"), DiscountType.NO_DISCOUNT, BigDecimal.ZERO);
        Workshop workshop = createWorkshopWithWallet("FastFix Center", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));
        linkWorkshopService(workshop, service);

        String customerToken = jwtService.generateAccessToken(customer);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicle.getId());
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("10:00-11:00");
        req.setServiceIds(List.of(service.getId()));
        req.setCity("New Delhi");

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Booking booking = bookingRepository.findAllByUserIdOrderByCreatedAtDesc(customer.getId()).get(0);
        ServiceRequest sr = booking.getServiceRequest();
        LeadOpportunity opp = leadOpportunityRepository.findByServiceRequestId(sr.getId()).get(0);

        String workshopToken = jwtService.generateAccessToken(workshop.getUser());

        // Workshop claims with Wallet
        WalletClaimPaymentRequest claimReq = new WalletClaimPaymentRequest(opp.getId(), "IDEMP-" + UUID.randomUUID());
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimReq)))
                .andExpect(status().isOk());

        WorkshopJob job = workshopJobRepository.findByServiceRequestId(sr.getId()).orElseThrow();

        // Workshop confirms job
        UpdateJobStatusRequest confirmReq = new UpdateJobStatusRequest(WorkshopJobStatus.CONFIRMED, "Slot confirmed");
        mockMvc.perform(put("/api/v1/workshops/jobs/" + job.getId() + "/status")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk());

        WorkshopWallet walletBeforeCancel = workshopWalletRepository.findByWorkshopId(workshop.getId()).orElseThrow();
        assertEquals(new BigDecimal("401.00"), walletBeforeCancel.getBalance().setScale(2, RoundingMode.HALF_UP));

        // Customer cancels booking BEFORE vehicle intake
        mockMvc.perform(put("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Workshop should receive automatic refund
        WorkshopWallet walletAfterCancel = workshopWalletRepository.findByWorkshopId(workshop.getId()).orElseThrow();
        assertEquals(new BigDecimal("500.00"), walletAfterCancel.getBalance().setScale(2, RoundingMode.HALF_UP));

        // Refund record created
        List<Refund> refunds = refundRepository.findAll();
        boolean hasCustomerCancelRefund = refunds.stream()
                .anyMatch(r -> r.getOpportunity().getId().equals(opp.getId()) && r.getRefundReason() == RefundReason.CUSTOMER_CANCELLED);
        assertTrue(hasCustomerCancelRefund, "Refund with CUSTOMER_CANCELLED reason must exist");

        // Job should be marked CANCELLED
        WorkshopJob cancelledJob = workshopJobRepository.findById(job.getId()).orElseThrow();
        assertEquals(WorkshopJobStatus.CANCELLED, cancelledJob.getStatus());
    }

    // ==========================================
    // TEST 13, 14, 15: Cancellation BLOCKED once physical work started
    // ==========================================
    @Test
    @DisplayName("Test 13-15: Customer cancellation BLOCKED once vehicle intake occurs")
    void test13_to_15_cancellationBlockedOnceVehicleReceived() throws Exception {
        User customer = createCustomer("BlockCancel");
        Vehicle vehicle = createVehicle(customer);

        ServiceCatalog service = createService("Suspension Tuning", new BigDecimal("5000.00"), DiscountType.NO_DISCOUNT, BigDecimal.ZERO);
        Workshop workshop = createWorkshopWithWallet("Precision Garage", "New Delhi", new BigDecimal("28.6139"), new BigDecimal("77.2090"), new BigDecimal("500.00"));
        linkWorkshopService(workshop, service);

        String customerToken = jwtService.generateAccessToken(customer);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehicleId(vehicle.getId());
        req.setBookingDate(LocalDate.now().plusDays(2));
        req.setTimeSlot("10:00-11:00");
        req.setServiceIds(List.of(service.getId()));
        req.setCity("New Delhi");

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        Booking booking = bookingRepository.findAllByUserIdOrderByCreatedAtDesc(customer.getId()).get(0);
        ServiceRequest sr = booking.getServiceRequest();
        LeadOpportunity opp = leadOpportunityRepository.findByServiceRequestId(sr.getId()).get(0);

        String workshopToken = jwtService.generateAccessToken(workshop.getUser());

        // Claim
        WalletClaimPaymentRequest claimReq = new WalletClaimPaymentRequest(opp.getId(), "IDEMP-" + UUID.randomUUID());
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + workshopToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimReq)))
                .andExpect(status().isOk());

        WorkshopJob job = workshopJobRepository.findByServiceRequestId(sr.getId()).orElseThrow();

        // Advance to VEHICLE_RECEIVED
        job.setStatus(WorkshopJobStatus.VEHICLE_RECEIVED);
        workshopJobRepository.save(job);

        // Cancellation must now fail with 400 Bad Request
        mockMvc.perform(put("/api/v1/bookings/" + booking.getId() + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot cancel")));

        // Also test direct service request cancel endpoint
        mockMvc.perform(put("/api/v1/service-requests/" + sr.getId() + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot cancel")));
    }
}

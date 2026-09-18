package com.carservice.backend;

import com.carservice.backend.marketplace.dto.*;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
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
import tools.jackson.databind.ObjectMapper;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class MarketplaceIntegrationTest {

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
    private LeadPaymentRepository leadPaymentRepository;

    @Autowired
    private PlatformConfigRepository platformConfigRepository;

    @Autowired
    private MarketplaceAuditEventRepository auditEventRepository;

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User customer;
    private String customerToken;
    private Vehicle customerVehicle;

    private User customer2;
    private String customer2Token;
    private Vehicle customer2Vehicle;

    private User admin;
    private String adminToken;

    private User partnerUserA;
    private String partnerTokenA;
    private Workshop workshopA;

    private User partnerUserB;
    private String partnerTokenB;
    private Workshop workshopB;

    private User partnerUserMumbai;
    private String partnerTokenMumbai;
    private Workshop workshopMumbai;

    private ServiceCatalog service1;
    private ServiceCatalog service2;
    private ServiceCatalog inactiveService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 1. Reset lead fee to 99.00
        platformConfigService.updateLeadAcceptanceFee(new BigDecimal("99.00"));

        // 2. Setup Customer 1
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        customer = new User();
        customer.setName("Rahul Sharma");
        customer.setEmail("rahul." + suffix + "@test.com");
        customer.setPhone("98" + (System.currentTimeMillis() % 100000000L));
        customer.setPassword(passwordEncoder.encode("Password@123"));
        customer.setRole(UserRole.CUSTOMER);
        customer.setIsActive(true);
        customer = userRepository.save(customer);
        customerToken = jwtService.generateAccessToken(customer);

        customerVehicle = new Vehicle(
                customer,
                "Hyundai",
                "Creta",
                2023,
                "DL01" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                FuelType.PETROL,
                Transmission.AUTOMATIC
        );
        customerVehicle = vehicleRepository.save(customerVehicle);

        // 3. Setup Customer 2
        customer2 = new User();
        customer2.setName("Priya Verma");
        customer2.setEmail("priya." + suffix + "@test.com");
        customer2.setPhone("97" + ((System.currentTimeMillis() + 1) % 100000000L));
        customer2.setPassword(passwordEncoder.encode("Password@123"));
        customer2.setRole(UserRole.CUSTOMER);
        customer2.setIsActive(true);
        customer2 = userRepository.save(customer2);
        customer2Token = jwtService.generateAccessToken(customer2);

        customer2Vehicle = new Vehicle(
                customer2,
                "Tata",
                "Nexon",
                2022,
                "UP16" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                FuelType.DIESEL,
                Transmission.MANUAL
        );
        customer2Vehicle = vehicleRepository.save(customer2Vehicle);

        // 4. Setup Admin
        admin = new User();
        admin.setName("Platform Admin");
        admin.setEmail("admin." + suffix + "@carservice.com");
        admin.setPhone("96" + ((System.currentTimeMillis() + 2) % 100000000L));
        admin.setPassword(passwordEncoder.encode("Admin@1234"));
        admin.setRole(UserRole.ADMIN);
        admin.setIsActive(true);
        admin = userRepository.save(admin);
        adminToken = jwtService.generateAccessToken(admin);

        // 5. Setup Services
        service1 = new ServiceCatalog(
                "Marketplace AC Cooling Test " + suffix,
                "Comprehensive AC inspection and refrigerant recharge",
                ServiceCategory.AC_SERVICE,
                new BigDecimal("2000.00"),
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), // Final = 1800.00
                90,
                true
        );
        service1 = serviceCatalogRepository.save(service1);

        service2 = new ServiceCatalog(
                "Marketplace Wheel Alignment Test " + suffix,
                "Precision computerized laser wheel alignment",
                ServiceCategory.WHEEL_ALIGNMENT,
                new BigDecimal("800.00"),
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("100.00"), // Final = 700.00
                45,
                true
        );
        service2 = serviceCatalogRepository.save(service2);

        inactiveService = new ServiceCatalog(
                "Inactive Test Service " + suffix,
                "Deprecated service",
                ServiceCategory.PERIODIC_SERVICE,
                new BigDecimal("1500.00"),
                DiscountType.NO_DISCOUNT,
                BigDecimal.ZERO,
                60,
                false
        );
        inactiveService = serviceCatalogRepository.save(inactiveService);

        // 6. Setup Workshop A (Delhi, Connaught Place: 28.6315, 77.2167)
        partnerUserA = new User();
        partnerUserA.setName("Vikram Delhi Partner");
        partnerUserA.setEmail("partnerA." + suffix + "@delhiworkshop.com");
        partnerUserA.setPhone("95" + ((System.currentTimeMillis() + 3) % 100000000L));
        partnerUserA.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserA.setRole(UserRole.PARTNER);
        partnerUserA.setIsActive(true);
        partnerUserA = userRepository.save(partnerUserA);
        partnerTokenA = jwtService.generateAccessToken(partnerUserA);

        workshopA = new Workshop(
                partnerUserA,
                "Delhi Auto Care Hub " + suffix,
                partnerUserA.getPhone(),
                partnerUserA.getEmail(),
                "12 Connaught Circus",
                "Delhi",
                "Delhi",
                "110001",
                new BigDecimal("28.6315"),
                new BigDecimal("77.2167"),
                new BigDecimal("30.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopA = workshopRepository.save(workshopA);
        workshopServiceRepository.save(new WorkshopService(workshopA, service1, true));
        workshopServiceRepository.save(new WorkshopService(workshopA, service2, true));

        WorkshopWallet walletA = new WorkshopWallet(workshopA, new BigDecimal("500.00"));
        workshopWalletRepository.save(walletA);

        // 7. Setup Workshop B (Delhi / Noida border: 28.5800, 77.3100)
        partnerUserB = new User();
        partnerUserB.setName("Suresh Noida Partner");
        partnerUserB.setEmail("partnerB." + suffix + "@noidaworkshop.com");
        partnerUserB.setPhone("94" + ((System.currentTimeMillis() + 4) % 100000000L));
        partnerUserB.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserB.setRole(UserRole.PARTNER);
        partnerUserB.setIsActive(true);
        partnerUserB = userRepository.save(partnerUserB);
        partnerTokenB = jwtService.generateAccessToken(partnerUserB);

        workshopB = new Workshop(
                partnerUserB,
                "Speedy Wheels Sector 18 " + suffix,
                partnerUserB.getPhone(),
                partnerUserB.getEmail(),
                "Sector 18 Market",
                "Delhi",
                "Delhi",
                "110092",
                new BigDecimal("28.5700"),
                new BigDecimal("77.3100"),
                new BigDecimal("25.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopB = workshopRepository.save(workshopB);
        workshopServiceRepository.save(new WorkshopService(workshopB, service1, true));
        workshopServiceRepository.save(new WorkshopService(workshopB, service2, true));

        WorkshopWallet walletB = new WorkshopWallet(workshopB, new BigDecimal("300.00"));
        workshopWalletRepository.save(walletB);

        // 8. Setup Workshop Mumbai (Far away: 19.0178, 72.8478)
        partnerUserMumbai = new User();
        partnerUserMumbai.setName("Anil Mumbai Partner");
        partnerUserMumbai.setEmail("partnerMum." + suffix + "@mumbaiworkshop.com");
        partnerUserMumbai.setPhone("93" + ((System.currentTimeMillis() + 5) % 100000000L));
        partnerUserMumbai.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserMumbai.setRole(UserRole.PARTNER);
        partnerUserMumbai.setIsActive(true);
        partnerUserMumbai = userRepository.save(partnerUserMumbai);
        partnerTokenMumbai = jwtService.generateAccessToken(partnerUserMumbai);

        workshopMumbai = new Workshop(
                partnerUserMumbai,
                "Worli Precision Auto " + suffix,
                partnerUserMumbai.getPhone(),
                partnerUserMumbai.getEmail(),
                "Worli Naka",
                "Mumbai",
                "Maharashtra",
                "400018",
                new BigDecimal("19.0178"),
                new BigDecimal("72.8478"),
                new BigDecimal("20.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopMumbai = workshopRepository.save(workshopMumbai);
        workshopServiceRepository.save(new WorkshopService(workshopMumbai, service1, true));
        workshopServiceRepository.save(new WorkshopService(workshopMumbai, service2, true));

        WorkshopWallet walletM = new WorkshopWallet(workshopMumbai, new BigDecimal("500.00"));
        workshopWalletRepository.save(walletM);
    }

    private CreateServiceRequestRequest buildDelhiRequest() {
        CreateServiceRequestRequest req = new CreateServiceRequestRequest();
        req.setVehicleId(customerVehicle.getId());
        req.setServiceIds(List.of(service1.getId(), service2.getId()));
        req.setCity("Delhi");
        req.setAddress("Flat 402, Sunshine Apartments, Mayur Vihar");
        req.setPincode("110091");
        req.setLatitude(new BigDecimal("28.6139"));
        req.setLongitude(new BigDecimal("77.2090"));
        req.setPreferredDate(LocalDate.now().plusDays(2));
        req.setPreferredTimeSlot("10:00 AM - 01:00 PM");
        req.setCustomerNotes("Please check AC filter");
        return req;
    }

    // =========================================================================
    // 1. Customer Multi-Service Request & Server-Side Pricing
    // =========================================================================
    @Test
    @DisplayName("1. Customer can create service request with multiple services and server-calculated price")
    void customerCanCreateServiceRequestWithMultipleServices() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestReference", startsWith("SR-")))
                .andExpect(jsonPath("$.data.userId").value(customer.getId()))
                .andExpect(jsonPath("$.data.vehicleId").value(customerVehicle.getId()))
                // Service 1: 2000 - 10% = 1800, Service 2: 800 - 100 = 700. Total = 2500.00
                .andExpect(jsonPath("$.data.totalAmount").value(2500.00))
                .andExpect(jsonPath("$.data.status").value("MATCHED"))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].finalPriceSnapshot").value(1800.00))
                .andExpect(jsonPath("$.data.items[1].finalPriceSnapshot").value(700.00));
    }

    // =========================================================================
    // 2. Vehicle Ownership Validation
    // =========================================================================
    @Test
    @DisplayName("2. Service request rejected if vehicle not owned by requesting customer")
    void serviceRequestRejectedIfVehicleNotOwnedByCustomer() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();
        req.setVehicleId(customer2Vehicle.getId()); // Belongs to customer2

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 3. Inactive or Missing Service Validation
    // =========================================================================
    @Test
    @DisplayName("3. Service request rejected if service is inactive or not found")
    void serviceRequestRejectedIfServiceNotFoundOrInactive() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();
        req.setServiceIds(List.of(inactiveService.getId()));

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 4 & 5. Haversine Matching within Radius & Excludes Far Away
    // =========================================================================
    @Test
    @DisplayName("4 & 5. Matching engine matches nearby workshops and strictly excludes out-of-radius workshops")
    void matchingEngineRadiusCheck() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("MATCHED"));

        // Workshop A (Delhi ~3km away) MUST have opportunity
        List<LeadOpportunity> oppsA = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopA.getId());
        assertFalse(oppsA.isEmpty(), "Workshop A in Delhi should have matched");

        // Workshop Mumbai (>1000km away) MUST NOT have opportunity
        List<LeadOpportunity> oppsMumbai = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopMumbai.getId());
        assertTrue(oppsMumbai.isEmpty(), "Workshop Mumbai should NOT match a Delhi request");
    }

    // =========================================================================
    // 6. City Fallback when Lat/Lng Null
    // =========================================================================
    @Test
    @DisplayName("6. Matching engine falls back to case-insensitive city match when lat/lng are null")
    void matchingEngineCityFallback() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();
        req.setCity("mUMbaI"); // Case insensitive
        req.setLatitude(null);
        req.setLongitude(null);

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("MATCHED"));

        // Mumbai workshop should have matched
        List<LeadOpportunity> oppsMumbai = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopMumbai.getId());
        assertFalse(oppsMumbai.isEmpty(), "Mumbai workshop should match via city fallback");
    }

    // =========================================================================
    // 7. Strict 100% Service Coverage Rule
    // =========================================================================
    @Test
    @DisplayName("7. Workshop offering only partial services is strictly excluded")
    void matchingEngineStrictServiceCoverageRule() throws Exception {
        // Create specialized Workshop C that ONLY supports service1 (not service2)
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User partnerC = new User();
        partnerC.setName("Partner C Partial");
        partnerC.setEmail("partnerC." + suffix + "@test.com");
        partnerC.setPhone("92" + (System.currentTimeMillis() % 100000000L));
        partnerC.setPassword(passwordEncoder.encode("Partner@123"));
        partnerC.setRole(UserRole.PARTNER);
        partnerC.setIsActive(true);
        partnerC = userRepository.save(partnerC);

        Workshop workshopC = new Workshop(
                partnerC,
                "Partial Service Center " + suffix,
                partnerC.getPhone(),
                partnerC.getEmail(),
                "Connaught Place",
                "Delhi",
                "Delhi",
                "110001",
                new BigDecimal("28.6315"),
                new BigDecimal("77.2167"),
                new BigDecimal("30.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopC = workshopRepository.save(workshopC);
        workshopServiceRepository.save(new WorkshopService(workshopC, service1, true)); // ONLY service 1!

        // Customer requests service1 AND service2
        CreateServiceRequestRequest req = buildDelhiRequest();

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        List<LeadOpportunity> oppsC = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopC.getId());
        assertTrue(oppsC.isEmpty(), "Workshop C offering only 1 of 2 services must be strictly excluded");
    }

    // =========================================================================
    // 8, 9, 10, 11. Opportunity Lifecycle, Auto-View, Accept, and Strict Masking
    // =========================================================================
    @Test
    @DisplayName("8, 9, 10, 11. Opportunity lifecycle: AVAILABLE -> auto-VIEWED -> ACCEPTED with strict masking")
    void opportunityLifecycleAndMasking() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();

        // Customer creates request
        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Find opportunity for Workshop A
        LeadOpportunity opp = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopA.getId()).get(0);
        assertEquals(OpportunityStatus.AVAILABLE, opp.getStatus());
        assertEquals(0, opp.getFeeSnapshot().compareTo(new BigDecimal("99.00")));

        // 9. Partner views opportunity -> auto-transitions to VIEWED
        mockMvc.perform(get("/api/v1/partner/opportunities/" + opp.getId())
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VIEWED"))
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(false))
                // 11. Strict masking verification!
                .andExpect(jsonPath("$.data.customerProfile.masked").value(true))
                .andExpect(jsonPath("$.data.customerProfile.name").value("********"))
                .andExpect(jsonPath("$.data.customerProfile.phone").value("**********"))
                .andExpect(jsonPath("$.data.customerProfile.email").value("********"))
                .andExpect(jsonPath("$.data.customerProfile.address", not(containsString("Sunshine Apartments"))));

        // 10. Partner accepts opportunity -> transitions to ACCEPTED
        mockMvc.perform(post("/api/v1/partner/opportunities/" + opp.getId() + "/accept")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.masked").value(true));
    }

    // =========================================================================
    // 12. Wallet Topup & Ledger Check
    // =========================================================================
    @Test
    @DisplayName("12. Workshop can topup wallet and view ledger entries")
    void workshopWalletTopupAndLedger() throws Exception {
        WalletTopupRequest topup = new WalletTopupRequest(new BigDecimal("500.00"), "Test topup", "REF-TOPUP-123");

        mockMvc.perform(post("/api/v1/partner/wallet/topup")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topup)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("CREDIT"))
                .andExpect(jsonPath("$.data.amount").value(500.00))
                .andExpect(jsonPath("$.data.balanceAfter").value(1000.00)); // 500 initial + 500

        mockMvc.perform(get("/api/v1/partner/wallet/transactions")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    // =========================================================================
    // 13 & 17. Wallet Payment Unlocks Details & Cancels Competing Opportunities
    // =========================================================================
    @Test
    @DisplayName("13 & 17. Wallet payment deducts exact fee, unlocks customer details, assigns request, and cancels competing leads")
    void walletPaymentUnlocksDetails() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();

        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        LeadOpportunity oppA = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopA.getId()).get(0);
        LeadOpportunity oppB = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopB.getId()).get(0);

        // Workshop A pays lead fee via wallet
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/pay/wallet")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CUSTOMER_DETAILS_UNLOCKED"))
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(true))
                // Real details revealed!
                .andExpect(jsonPath("$.data.customerProfile.masked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.name").value("Rahul Sharma"))
                .andExpect(jsonPath("$.data.customerProfile.phone").value(customer.getPhone()))
                .andExpect(jsonPath("$.data.customerProfile.email").value(customer.getEmail()))
                .andExpect(jsonPath("$.data.customerProfile.address", containsString("Sunshine Apartments")))
                .andExpect(jsonPath("$.data.payment.paymentMethod").value("WALLET"))
                .andExpect(jsonPath("$.data.payment.amount").value(99.00));

        // Wallet balance checked: 500 - 99 = 401.00
        WorkshopWallet walletA = workshopWalletRepository.findByWorkshopId(workshopA.getId()).get();
        assertEquals(0, walletA.getBalance().compareTo(new BigDecimal("401.00")));

        // ServiceRequest assigned to Workshop A
        ServiceRequest updatedReq = serviceRequestRepository.findById(oppA.getServiceRequest().getId()).get();
        assertEquals(ServiceRequestStatus.ACCEPTED, updatedReq.getStatus());
        assertEquals(workshopA.getId(), updatedReq.getAssignedWorkshop().getId());

        // 17. Competing opportunity for Workshop B is cancelled
        LeadOpportunity refreshedOppB = leadOpportunityRepository.findById(oppB.getId()).get();
        assertEquals(OpportunityStatus.CANCELLED, refreshedOppB.getStatus());
    }

    // =========================================================================
    // 14. Insufficient Wallet Balance
    // =========================================================================
    @Test
    @DisplayName("14. Wallet payment rejected when workshop has insufficient balance")
    void walletPaymentFailsOnInsufficientBalance() throws Exception {
        // Zero out Workshop A's wallet
        WorkshopWallet walletA = workshopWalletRepository.findByWorkshopId(workshopA.getId()).get();
        walletA.setBalance(BigDecimal.ZERO);
        workshopWalletRepository.save(walletA);

        CreateServiceRequestRequest req = buildDelhiRequest();
        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        LeadOpportunity oppA = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopA.getId()).get(0);

        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/pay/wallet")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isBadRequest()); // IllegalStateException mapped to 400 Bad Request
    }

    // =========================================================================
    // 15. Direct Payment (UPI / CARD) Unlocks Details
    // =========================================================================
    @Test
    @DisplayName("15. Direct payment via UPI or Card unlocks customer details successfully")
    void directPaymentUnlocksCustomerDetails() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();
        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        LeadOpportunity oppA = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopA.getId()).get(0);

        String txRef = "UPI-REF-" + UUID.randomUUID().toString().substring(0, 8);
        DirectPaymentRequest directPay = new DirectPaymentRequest(LeadPaymentMethod.UPI, txRef);

        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/pay/direct")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(directPay)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CUSTOMER_DETAILS_UNLOCKED"))
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(true))
                .andExpect(jsonPath("$.data.customerProfile.name").value("Rahul Sharma"))
                .andExpect(jsonPath("$.data.payment.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.data.payment.transactionReference").value(txRef));
    }

    // =========================================================================
    // 16. Cross-Workshop Access Forbidden
    // =========================================================================
    @Test
    @DisplayName("16. Workshop B cannot view or pay for Workshop A's opportunity")
    void crossWorkshopAccessForbidden() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();
        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        LeadOpportunity oppA = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopA.getId()).get(0);

        // Partner B attempts to access oppA
        mockMvc.perform(get("/api/v1/partner/opportunities/" + oppA.getId())
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 18, 19, 20, 21. Multi-Hop Transfer, Re-Matching, No Refund, Independent Fee
    // =========================================================================
    @Test
    @DisplayName("18, 19, 20, 21. Transfer triggers re-matching excluding Workshop A, keeps A's payment, requires B to pay independently")
    void multiHopTransferAndReMatching() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();
        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        LeadOpportunity oppA = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopA.getId()).get(0);

        // 1. Workshop A pays and unlocks details
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/pay/wallet")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk());

        BigDecimal walletABeforeTransfer = workshopWalletRepository.findByWorkshopId(workshopA.getId()).get().getBalance();

        // 2. Workshop A transfers opportunity
        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("Bay capacity full due to urgent engine overhaul");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TRANSFERRED"))
                .andExpect(jsonPath("$.data.transferReason").value("Bay capacity full due to urgent engine overhaul"));

        // 20. Transferred workshop does NOT receive refund
        BigDecimal walletAAfterTransfer = workshopWalletRepository.findByWorkshopId(workshopA.getId()).get().getBalance();
        assertEquals(0, walletABeforeTransfer.compareTo(walletAAfterTransfer), "Transferred workshop balance must not change (no refund)");

        // 19. Re-matching: Workshop B receives new opportunity
        List<LeadOpportunity> oppsB = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopB.getId());
        assertFalse(oppsB.isEmpty());
        LeadOpportunity newOppB = oppsB.get(0);

        // 21. Second workshop MUST pay independently to unlock details
        mockMvc.perform(get("/api/v1/partner/opportunities/" + newOppB.getId())
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.masked").value(true));

        // Workshop B pays its own ₹99 fee
        mockMvc.perform(post("/api/v1/partner/opportunities/" + newOppB.getId() + "/pay/wallet")
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(true))
                .andExpect(jsonPath("$.data.customerProfile.masked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.name").value("Rahul Sharma"));

        // Workshop B's wallet debited: 300 - 99 = 201.00
        WorkshopWallet walletB = workshopWalletRepository.findByWorkshopId(workshopB.getId()).get();
        assertEquals(0, walletB.getBalance().compareTo(new BigDecimal("201.00")));
    }

    // =========================================================================
    // 22 & 23. Admin Dynamic Fee Update & Snapshot Immutability
    // =========================================================================
    @Test
    @DisplayName("22 & 23. Admin updates dynamic lead fee to 149; new leads get 149 while existing snapshots stay 99")
    void adminDynamicLeadFeeUpdateAndSnapshots() throws Exception {
        // Create request 1 under ₹99 fee
        CreateServiceRequestRequest req1 = buildDelhiRequest();
        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        LeadOpportunity oppOld = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopA.getId()).get(0);
        assertEquals(0, oppOld.getFeeSnapshot().compareTo(new BigDecimal("99.00")));

        // 22. Admin updates fee to 149.00
        UpdateLeadFeeRequest updateFee = new UpdateLeadFeeRequest(new BigDecimal("149.00"));
        mockMvc.perform(put("/api/v1/admin/marketplace/config/lead-fee")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateFee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configValue").value("149.00"));

        // 23. Create request 2 under new fee
        CreateServiceRequestRequest req2 = buildDelhiRequest();
        mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        LeadOpportunity oppNew = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopA.getId()).get(0);
        assertEquals(0, oppNew.getFeeSnapshot().compareTo(new BigDecimal("149.00")));

        // Verify old snapshot is still 99.00!
        LeadOpportunity refreshedOld = leadOpportunityRepository.findById(oppOld.getId()).get();
        assertEquals(0, refreshedOld.getFeeSnapshot().compareTo(new BigDecimal("99.00")));
    }

    // =========================================================================
    // 24. Auditable Event Trail
    // =========================================================================
    @Test
    @DisplayName("24. Marketplace audit event journal records full request lifecycle")
    void auditTrailCompleteJournal() throws Exception {
        CreateServiceRequestRequest req = buildDelhiRequest();
        String responseContent = mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ServiceRequestResponse created = objectMapper.readValue(
                objectMapper.readTree(responseContent).get("data").toString(),
                ServiceRequestResponse.class
        );

        mockMvc.perform(get("/api/v1/admin/marketplace/audit-events/request/" + created.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data[*].eventType", hasItems("REQUEST_CREATED", "WORKSHOPS_MATCHED", "OPPORTUNITY_CREATED")));
    }
}

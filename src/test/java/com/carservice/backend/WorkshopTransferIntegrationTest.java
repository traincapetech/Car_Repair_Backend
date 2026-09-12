package com.carservice.backend;

import com.carservice.backend.marketplace.dto.*;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.repository.*;
import com.carservice.backend.marketplace.service.LeadOpportunityService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
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
@ActiveProfiles("test")
@Transactional
public class WorkshopTransferIntegrationTest {

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
    private WorkshopPaymentRepository workshopPaymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private MarketplaceAuditEventRepository auditEventRepository;

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private LeadOpportunityService leadOpportunityService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User customer;
    private String customerToken;
    private Vehicle customerVehicle;

    private User partnerUserA;
    private String partnerTokenA;
    private Workshop workshopA;

    private User partnerUserB;
    private String partnerTokenB;
    private Workshop workshopB;

    private User partnerUserC;
    private String partnerTokenC;
    private Workshop workshopC;

    private User partnerUserMumbai;
    private String partnerTokenMumbai;
    private Workshop workshopMumbai;

    private ServiceCatalog service1;
    private ServiceCatalog service2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        platformConfigService.updateLeadAcceptanceFee(new BigDecimal("99.00"));

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Setup Customer in Delhi
        customer = new User();
        customer.setName("Aarav Mehta");
        customer.setEmail("aarav." + suffix + "@test.com");
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

        // 2. Setup Services
        service1 = new ServiceCatalog(
                "Transfer Periodic Service " + suffix,
                "Standard maintenance service",
                ServiceCategory.PERIODIC_SERVICE,
                new BigDecimal("2999.00"),
                DiscountType.FIXED_AMOUNT,
                BigDecimal.ZERO,
                120,
                true
        );
        service1 = serviceCatalogRepository.save(service1);

        service2 = new ServiceCatalog(
                "Transfer AC Service " + suffix,
                "AC check and cooling coil cleaning",
                ServiceCategory.AC_SERVICE,
                new BigDecimal("1500.00"),
                DiscountType.FIXED_AMOUNT,
                BigDecimal.ZERO,
                60,
                true
        );
        service2 = serviceCatalogRepository.save(service2);

        // 3. Workshop A (Delhi CP: 28.6315, 77.2167) - Balance ₹1,000
        partnerUserA = new User();
        partnerUserA.setName("Partner A Delhi");
        partnerUserA.setEmail("partnerA." + suffix + "@delhi.com");
        partnerUserA.setPhone("95" + ((System.currentTimeMillis() + 1) % 100000000L));
        partnerUserA.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserA.setRole(UserRole.PARTNER);
        partnerUserA.setIsActive(true);
        partnerUserA = userRepository.save(partnerUserA);
        partnerTokenA = jwtService.generateAccessToken(partnerUserA);

        workshopA = new Workshop(
                partnerUserA,
                "Apex Auto Care CP " + suffix,
                partnerUserA.getPhone(),
                partnerUserA.getEmail(),
                "14 Barakhamba Road, Connaught Place",
                "Delhi",
                "Delhi",
                "110001",
                new BigDecimal("28.6315"),
                new BigDecimal("77.2167"),
                new BigDecimal("25.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopA = workshopRepository.save(workshopA);
        workshopServiceRepository.save(new WorkshopService(workshopA, service1, true));
        workshopServiceRepository.save(new WorkshopService(workshopA, service2, true));
        workshopWalletRepository.save(new WorkshopWallet(workshopA, new BigDecimal("1000.00")));

        // 4. Workshop B (Delhi Sector 18: 28.5700, 77.3100) - Balance ₹500
        partnerUserB = new User();
        partnerUserB.setName("Partner B Delhi");
        partnerUserB.setEmail("partnerB." + suffix + "@delhi.com");
        partnerUserB.setPhone("94" + ((System.currentTimeMillis() + 2) % 100000000L));
        partnerUserB.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserB.setRole(UserRole.PARTNER);
        partnerUserB.setIsActive(true);
        partnerUserB = userRepository.save(partnerUserB);
        partnerTokenB = jwtService.generateAccessToken(partnerUserB);

        workshopB = new Workshop(
                partnerUserB,
                "Speedy Motors Mayur Vihar " + suffix,
                partnerUserB.getPhone(),
                partnerUserB.getEmail(),
                "Pocket 1, Mayur Vihar Phase 1",
                "Delhi",
                "Delhi",
                "110091",
                new BigDecimal("28.5700"),
                new BigDecimal("77.3100"),
                new BigDecimal("25.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopB = workshopRepository.save(workshopB);
        workshopServiceRepository.save(new WorkshopService(workshopB, service1, true));
        workshopServiceRepository.save(new WorkshopService(workshopB, service2, true));
        workshopWalletRepository.save(new WorkshopWallet(workshopB, new BigDecimal("500.00")));

        // 5. Workshop C (Delhi Karol Bagh: 28.6515, 77.1900) - Balance ₹600
        partnerUserC = new User();
        partnerUserC.setName("Partner C Delhi");
        partnerUserC.setEmail("partnerC." + suffix + "@delhi.com");
        partnerUserC.setPhone("93" + ((System.currentTimeMillis() + 3) % 100000000L));
        partnerUserC.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserC.setRole(UserRole.PARTNER);
        partnerUserC.setIsActive(true);
        partnerUserC = userRepository.save(partnerUserC);
        partnerTokenC = jwtService.generateAccessToken(partnerUserC);

        workshopC = new Workshop(
                partnerUserC,
                "Elite Motors Karol Bagh " + suffix,
                partnerUserC.getPhone(),
                partnerUserC.getEmail(),
                "Pusa Road, Karol Bagh",
                "Delhi",
                "Delhi",
                "110005",
                new BigDecimal("28.6515"),
                new BigDecimal("77.1900"),
                new BigDecimal("20.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopC = workshopRepository.save(workshopC);
        workshopServiceRepository.save(new WorkshopService(workshopC, service1, true));
        workshopServiceRepository.save(new WorkshopService(workshopC, service2, true));
        workshopWalletRepository.save(new WorkshopWallet(workshopC, new BigDecimal("600.00")));

        // 6. Workshop Mumbai (Distant: 19.0178, 72.8478) - Ineligible for Delhi requests
        partnerUserMumbai = new User();
        partnerUserMumbai.setName("Partner Mumbai");
        partnerUserMumbai.setEmail("partnerMum." + suffix + "@mumbai.com");
        partnerUserMumbai.setPhone("92" + ((System.currentTimeMillis() + 4) % 100000000L));
        partnerUserMumbai.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserMumbai.setRole(UserRole.PARTNER);
        partnerUserMumbai.setIsActive(true);
        partnerUserMumbai = userRepository.save(partnerUserMumbai);
        partnerTokenMumbai = jwtService.generateAccessToken(partnerUserMumbai);

        workshopMumbai = new Workshop(
                partnerUserMumbai,
                "Mumbai Express Worli " + suffix,
                partnerUserMumbai.getPhone(),
                partnerUserMumbai.getEmail(),
                "Worli Sea Face",
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
        workshopWalletRepository.save(new WorkshopWallet(workshopMumbai, new BigDecimal("500.00")));
    }

    private Long createCustomerServiceRequest() throws Exception {
        CreateServiceRequestRequest req = new CreateServiceRequestRequest();
        req.setVehicleId(customerVehicle.getId());
        req.setServiceIds(List.of(service1.getId(), service2.getId()));
        req.setCity("Delhi");
        req.setAddress("Flat 402, Barakhamba Apartments, Connaught Place");
        req.setPincode("110001");
        req.setLatitude(new BigDecimal("28.6315"));
        req.setLongitude(new BigDecimal("77.2167"));
        req.setPreferredDate(LocalDate.now().plusDays(2));
        req.setPreferredTimeSlot("10:00 AM - 01:00 PM");
        req.setCustomerNotes("Please check AC filter and engine oil");

        String resJson = mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(resJson).path("data").path("id").asLong();
    }

    // =========================================================================
    // Scenarios 1 & 4: Workshop can transfer its own eligible opportunity
    // =========================================================================
    @Test
    @DisplayName("1 & 4. Workshop can transfer its own eligible opportunity, status becomes TRANSFERRED")
    void test01_WorkshopCanTransferOwnOpportunity() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("WORKSHOP_AT_CAPACITY", "Bay capacity full this week");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("TRANSFERRED"))
                .andExpect(jsonPath("$.data.transferReason", containsString("WORKSHOP_AT_CAPACITY")));

        LeadOpportunity updated = leadOpportunityRepository.findById(oppA.getId()).orElseThrow();
        assertEquals(OpportunityStatus.TRANSFERRED, updated.getStatus());
        assertNotNull(updated.getTransferredAt());
        assertTrue(updated.getTransferReason().contains("WORKSHOP_AT_CAPACITY"));
    }

    // =========================================================================
    // Scenario 2: Workshop cannot transfer another workshop's opportunity
    // =========================================================================
    @Test
    @DisplayName("2. Workshop cannot transfer another workshop's opportunity")
    void test02_WorkshopCannotTransferAnotherWorkshopOpportunity() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Workshop B attempts to transfer Workshop A's opportunity
        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("PARTS_UNAVAILABLE");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not authorized")));
    }

    // =========================================================================
    // Scenario 3: Transfer reason is required and validated
    // =========================================================================
    @Test
    @DisplayName("3. Transfer reason is required and validated against valid reasons")
    void test03_TransferReasonRequiredAndValidated() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // A) Empty reason rejected
        TransferOpportunityRequest emptyReq = new TransferOpportunityRequest("");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyReq)))
                .andExpect(status().isBadRequest());

        // B) Completely invalid reason rejected
        TransferOpportunityRequest invalidReq = new TransferOpportunityRequest("INVALID_UNKNOWN_RANDOM_REASON_XYZ");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid transfer reason")));

        // C) Valid standard reason accepted
        TransferOpportunityRequest validReq = new TransferOpportunityRequest("PARTS_UNAVAILABLE", "Filter out of stock");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TRANSFERRED"));
    }

    // =========================================================================
    // Scenario 5: Transfer history is permanently persisted in audit events
    // =========================================================================
    @Test
    @DisplayName("5. Transfer history is permanently persisted in audit events")
    void test05_TransferHistoryPersistedInAuditEvents() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("OUTSIDE_SERVICE_RADIUS");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk());

        List<MarketplaceAuditEvent> events = auditEventRepository.findByServiceRequestIdOrderByCreatedAtDesc(reqId);
        boolean hasTransferRequested = events.stream()
                .anyMatch(e -> e.getEventType() == MarketplaceEventType.TRANSFER_REQUESTED && e.getWorkshopId().equals(workshopA.getId()));
        boolean hasOpportunityTransferred = events.stream()
                .anyMatch(e -> e.getEventType() == MarketplaceEventType.OPPORTUNITY_TRANSFERRED && e.getWorkshopId().equals(workshopA.getId()));
        boolean hasReMatched = events.stream()
                .anyMatch(e -> e.getEventType() == MarketplaceEventType.RE_MATCHED);

        assertTrue(hasTransferRequested, "Must record TRANSFER_REQUESTED audit event");
        assertTrue(hasOpportunityTransferred, "Must record OPPORTUNITY_TRANSFERRED audit event");
        assertTrue(hasReMatched, "Must record RE_MATCHED audit event");
    }

    // =========================================================================
    // Scenarios 6, 7 & 8: Re-matching creates NEW opportunity for different workshop, original excluded
    // =========================================================================
    @Test
    @DisplayName("6, 7 & 8. Re-matching creates brand new opportunity for different workshop; transferring workshop excluded")
    void test06_ReMatchingCreatesNewOpportunityAndExcludesOriginalWorkshop() throws Exception {
        Long reqId = createCustomerServiceRequest();

        // Workshop A claims & pays
        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        WalletClaimPaymentRequest payReq = new WalletClaimPaymentRequest(oppA.getId(), "IDEM-A-01");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk());

        // Competing opportunities were cancelled when A won
        // Now Workshop A transfers
        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("WORKSHOP_AT_CAPACITY");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk());

        // Re-matching should create a NEW opportunity for Workshop B or C, but NOT Workshop A
        List<LeadOpportunity> afterTransferOpps = leadOpportunityRepository.findByServiceRequestId(reqId);

        // Find active/available opportunities after transfer
        List<LeadOpportunity> newAvailableOpps = afterTransferOpps.stream()
                .filter(o -> o.getStatus() == OpportunityStatus.AVAILABLE)
                .toList();

        assertFalse(newAvailableOpps.isEmpty(), "New rematched opportunities must be created");

        // 8. Original workshop A must NOT be in the newly created opportunities
        boolean workshopAReceivesNewOpp = newAvailableOpps.stream()
                .anyMatch(o -> o.getWorkshop().getId().equals(workshopA.getId()));
        assertFalse(workshopAReceivesNewOpp, "Workshop A must be permanently excluded from receiving rematched opportunity");

        // 7. Belongs to different eligible workshops (B or C)
        for (LeadOpportunity newOpp : newAvailableOpps) {
            assertNotEquals(workshopA.getId(), newOpp.getWorkshop().getId());
            assertNotEquals(oppA.getId(), newOpp.getId(), "New opportunity must have a different ID");
        }
    }

    // =========================================================================
    // Scenarios 9, 10, 11, 12, 13 & 14: Payment isolation & Customer privacy on new opportunity
    // =========================================================================
    @Test
    @DisplayName("9-14. New opportunity does NOT inherit payment/order/wallet ref, starts unpaid with masked customer details")
    void test09_NewOpportunityDoesNotInheritPaymentAndKeepsCustomerMasked() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Workshop A pays and unlocks
        WalletClaimPaymentRequest payReq = new WalletClaimPaymentRequest(oppA.getId(), "IDEM-A-ISOLATION");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk());

        LeadOpportunity oppAUnlocked = leadOpportunityRepository.findById(oppA.getId()).orElseThrow();
        assertEquals(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED, oppAUnlocked.getStatus());
        assertNotNull(oppAUnlocked.getPayment());

        // Workshop A transfers
        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("SPECIALIZED_EQUIPMENT_REQUIRED");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk());

        // Workshop B receives new rematched opportunity
        LeadOpportunity oppB = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopB.getId()).get(0);

        // 9. New opportunity has no payment
        assertNull(oppB.getPayment(), "New opportunity must not inherit previous payment");

        // 13. Starts unpaid
        assertEquals(OpportunityStatus.AVAILABLE, oppB.getStatus());
        assertNull(oppB.getPaidAt());
        assertNull(oppB.getUnlockedAt());

        // 14. Customer details remain masked for Workshop B
        mockMvc.perform(get("/api/v1/partner/opportunities/" + oppB.getId())
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.masked").value(true))
                .andExpect(jsonPath("$.data.customerProfile.phone").value("**********"))
                .andExpect(jsonPath("$.data.payment").doesNotExist());
    }

    // =========================================================================
    // Scenario 15: New workshop must independently pay before customer details unlock
    // =========================================================================
    @Test
    @DisplayName("15. New workshop B must independently pay platform fee before customer details unlock")
    void test15_NewWorkshopMustIndependentlyPayBeforeUnlock() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Workshop A pays and unlocks
        WalletClaimPaymentRequest payReqA = new WalletClaimPaymentRequest(oppA.getId(), "IDEM-A-WIN");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReqA)))
                .andExpect(status().isOk());

        // Workshop A transfers
        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("WORKSHOP_AT_CAPACITY");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk());

        // Workshop B pays its own independent fee via wallet
        LeadOpportunity oppB = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopB.getId()).get(0);
        BigDecimal bBalanceBefore = workshopWalletRepository.findByWorkshopId(workshopB.getId()).get().getBalance();

        WalletClaimPaymentRequest payReqB = new WalletClaimPaymentRequest(oppB.getId(), "IDEM-B-WIN");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReqB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        // Workshop B's wallet is debited by ₹99: 500.00 - 99.00 = 401.00
        BigDecimal bBalanceAfter = workshopWalletRepository.findByWorkshopId(workshopB.getId()).get().getBalance();
        assertEquals(new BigDecimal("401.00"), bBalanceAfter);

        // Workshop B now has unlocked details
        mockMvc.perform(get("/api/v1/partner/opportunities/" + oppB.getId())
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(true))
                .andExpect(jsonPath("$.data.customerProfile.masked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.name").value("Aarav Mehta"))
                .andExpect(jsonPath("$.data.customerProfile.phone").value(customer.getPhone()));
    }

    // =========================================================================
    // Scenarios 16 & 17: Previous workshop's payment and refund records remain intact
    // =========================================================================
    @Test
    @DisplayName("16 & 17. Previous workshop's payment record remains intact; voluntary transfer does not refund")
    void test16_17_PreviousPaymentRecordRemainsIntactAndNoRefundForVoluntaryTransfer() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Workshop A pays ₹99
        WalletClaimPaymentRequest payReqA = new WalletClaimPaymentRequest(oppA.getId(), "IDEM-A-PERM");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReqA)))
                .andExpect(status().isOk());

        BigDecimal aBalanceAfterPayment = workshopWalletRepository.findByWorkshopId(workshopA.getId()).get().getBalance();
        assertEquals(new BigDecimal("901.00"), aBalanceAfterPayment);

        // Workshop A transfers
        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("PARTS_UNAVAILABLE");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk());

        // 16. Workshop A's payment record in database remains SUCCESS
        List<WorkshopPayment> paymentsA = workshopPaymentRepository.findByOpportunityId(oppA.getId());
        assertFalse(paymentsA.isEmpty());
        assertEquals(PaymentStatus.SUCCESS, paymentsA.get(0).getPaymentStatus());

        // 17. No refund given for voluntary transfer (balance stays 901.00)
        BigDecimal aBalanceAfterTransfer = workshopWalletRepository.findByWorkshopId(workshopA.getId()).get().getBalance();
        assertEquals(0, aBalanceAfterPayment.compareTo(aBalanceAfterTransfer));
    }

    // =========================================================================
    // Scenarios 18 & 19: Same workshop cannot transfer twice; duplicate transfer safely rejected/idempotent
    // =========================================================================
    @Test
    @DisplayName("18 & 19. Same workshop cannot transfer twice; duplicate transfer is safely rejected")
    void test18_19_DuplicateTransferSafelyRejected() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("WORKSHOP_AT_CAPACITY");

        // First transfer succeeds
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk());

        // Duplicate transfer attempt is safely rejected
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already been transferred")));
    }

    // =========================================================================
    // Scenario 20: Cross-workshop access remains strictly isolated
    // =========================================================================
    @Test
    @DisplayName("20. Cross-workshop access remains isolated; Workshop B cannot view Workshop A's transferred opportunity")
    void test20_CrossWorkshopAccessRemainsIsolated() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Workshop A transfers
        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("WORKSHOP_AT_CAPACITY");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk());

        // Workshop B tries to view Workshop A's transferred opportunity
        mockMvc.perform(get("/api/v1/partner/opportunities/" + oppA.getId())
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not authorized")));
    }

    // =========================================================================
    // Scenario 21: Geographic matching rule (Delhi customer does not get Mumbai workshop)
    // =========================================================================
    @Test
    @DisplayName("21. Delhi customer request is never dispatched to distant workshop (Mumbai)")
    void test21_GeographicMatchingExcludesDistantWorkshops() throws Exception {
        Long reqId = createCustomerServiceRequest();

        // Workshop A transfers
        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("OUTSIDE_SERVICE_RADIUS");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk());

        // Check if Mumbai workshop ever received an opportunity for this request
        List<LeadOpportunity> mumbaiOpps = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopMumbai.getId());
        boolean mumbaiMatchedToDelhi = mumbaiOpps.stream()
                .anyMatch(o -> o.getServiceRequest().getId().equals(reqId));

        assertFalse(mumbaiMatchedToDelhi, "Delhi service request must never be dispatched to Mumbai workshop");
    }

    // =========================================================================
    // Scenario 22: If no eligible workshop exists, service request enters RE_MATCHING without corruption
    // =========================================================================
    @Test
    @DisplayName("22. If all eligible workshops transfer, service request stays in RE_MATCHING without corruption")
    void test22_NoEligibleWorkshopsEntersReMatchingWithoutCorruption() throws Exception {
        Long reqId = createCustomerServiceRequest();

        // Workshop A transfers
        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferOpportunityRequest("WORKSHOP_AT_CAPACITY"))))
                .andExpect(status().isOk());

        // Workshop B transfers
        LeadOpportunity oppB = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopB.getId()).stream()
                .filter(o -> o.getServiceRequest().getId().equals(reqId))
                .findFirst().orElseThrow();
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppB.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferOpportunityRequest("PARTS_UNAVAILABLE"))))
                .andExpect(status().isOk());

        // Workshop C transfers
        LeadOpportunity oppC = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopC.getId()).stream()
                .filter(o -> o.getServiceRequest().getId().equals(reqId))
                .findFirst().orElseThrow();
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppC.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferOpportunityRequest("SPECIALIZED_EQUIPMENT_REQUIRED"))))
                .andExpect(status().isOk());

        // Now all eligible Delhi workshops have transferred!
        ServiceRequest reqAfterAllTransfers = serviceRequestRepository.findById(reqId).orElseThrow();
        assertEquals(ServiceRequestStatus.RE_MATCHING, reqAfterAllTransfers.getStatus(),
                "Service request must remain in RE_MATCHING status when no eligible replacement workshops exist");
        assertNull(reqAfterAllTransfers.getAssignedWorkshop(),
                "Service request must have no assigned workshop");
    }

    // =========================================================================
    // Scenario 23: Race condition safety on rematched opportunity
    // =========================================================================
    @Test
    @DisplayName("23. Race condition on rematched opportunity: first payer wins and claims request; second payer gets refund")
    void test23_RaceConditionSafetyOnRematchedOpportunity() throws Exception {
        Long reqId = createCustomerServiceRequest();

        // Workshop A transfers immediately
        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferOpportunityRequest("WORKSHOP_AT_CAPACITY"))))
                .andExpect(status().isOk());

        // Both Workshop B and Workshop C received new opportunities
        LeadOpportunity oppB = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopB.getId()).stream()
                .filter(o -> o.getServiceRequest().getId().equals(reqId) && o.getStatus() == OpportunityStatus.AVAILABLE)
                .findFirst().orElseThrow();

        LeadOpportunity oppC = leadOpportunityRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopC.getId()).stream()
                .filter(o -> o.getServiceRequest().getId().equals(reqId) && o.getStatus() == OpportunityStatus.AVAILABLE)
                .findFirst().orElseThrow();

        // Workshop B pays first and claims the request
        WalletClaimPaymentRequest payB = new WalletClaimPaymentRequest(oppB.getId(), "RACE-B-01");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        // Workshop B unlocks details
        LeadOpportunity oppBWon = leadOpportunityRepository.findById(oppB.getId()).orElseThrow();
        assertEquals(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED, oppBWon.getStatus());

        // Now Workshop C pays for its opportunity (competing opportunity was cancelled by B's win)
        // Trying to pay for cancelled opportunity is rejected or triggers refund
        WalletClaimPaymentRequest payC = new WalletClaimPaymentRequest(oppC.getId(), "RACE-C-01");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payC)))
                .andExpect(status().isBadRequest());

        // Workshop C's balance was not deducted (remains ₹600)
        BigDecimal cBalance = workshopWalletRepository.findByWorkshopId(workshopC.getId()).get().getBalance();
        assertEquals(new BigDecimal("600.00"), cBalance);

        // Service request remains assigned to Workshop B
        ServiceRequest finalReq = serviceRequestRepository.findById(reqId).orElseThrow();
        assertEquals(workshopB.getId(), finalReq.getAssignedWorkshop().getId());
        assertEquals(ServiceRequestStatus.ACCEPTED, finalReq.getStatus());
    }
}

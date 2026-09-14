package com.carservice.backend;

import com.carservice.backend.marketplace.dto.CreateServiceRequestRequest;
import com.carservice.backend.marketplace.dto.InitiatePaymentRequest;
import com.carservice.backend.marketplace.dto.TransferOpportunityRequest;
import com.carservice.backend.marketplace.dto.WalletClaimPaymentRequest;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.repository.*;
import com.carservice.backend.marketplace.service.LeadOpportunityService;
import com.carservice.backend.marketplace.service.PlatformConfigService;
import com.carservice.backend.marketplace.service.WorkshopPaymentService;
import com.carservice.backend.marketplace.service.WorkshopRefundService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "razorpay.enabled=true",
        "razorpay.key-id=rzp_test_default",
        "razorpay.key-secret=secret_test_default",
        "razorpay.mock-gateway=true"
})
public class WorkshopPaymentIntegrationTest {

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
    private PlatformConfigRepository platformConfigRepository;

    @Autowired
    private MarketplaceAuditEventRepository auditEventRepository;

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private WorkshopPaymentService workshopPaymentService;

    @Autowired
    private WorkshopRefundService workshopRefundService;

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

    private User admin;
    private String adminToken;

    private User partnerUserA;
    private String partnerTokenA;
    private Workshop workshopA;

    private User partnerUserB;
    private String partnerTokenB;
    private Workshop workshopB;

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

        // Setup Customer
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

        // Setup Admin
        admin = new User();
        admin.setName("Super Admin");
        admin.setEmail("admin." + suffix + "@carservice.com");
        admin.setPhone("96" + ((System.currentTimeMillis() + 1) % 100000000L));
        admin.setPassword(passwordEncoder.encode("Admin@1234"));
        admin.setRole(UserRole.ADMIN);
        admin.setIsActive(true);
        admin = userRepository.save(admin);
        adminToken = jwtService.generateAccessToken(admin);

        // Setup Services
        service1 = new ServiceCatalog(
                "Payment AC Service " + suffix,
                "AC check and cooling coil cleaning",
                ServiceCategory.AC_SERVICE,
                new BigDecimal("2000.00"),
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                90,
                true
        );
        service1 = serviceCatalogRepository.save(service1);

        service2 = new ServiceCatalog(
                "Payment Wheel Alignment " + suffix,
                "Computerized alignment and balancing",
                ServiceCategory.WHEEL_ALIGNMENT,
                new BigDecimal("800.00"),
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("100.00"),
                45,
                true
        );
        service2 = serviceCatalogRepository.save(service2);

        // Workshop A (Delhi CP: 28.6315, 77.2167)
        partnerUserA = new User();
        partnerUserA.setName("Partner A Delhi");
        partnerUserA.setEmail("partnerA." + suffix + "@delhihub.com");
        partnerUserA.setPhone("95" + ((System.currentTimeMillis() + 2) % 100000000L));
        partnerUserA.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserA.setRole(UserRole.PARTNER);
        partnerUserA.setIsActive(true);
        partnerUserA = userRepository.save(partnerUserA);
        partnerTokenA = jwtService.generateAccessToken(partnerUserA);

        workshopA = new Workshop(
                partnerUserA,
                "Connaught Care " + suffix,
                partnerUserA.getPhone(),
                partnerUserA.getEmail(),
                "15 CP Inner Circle",
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

        // Workshop B (Delhi Sector 18: 28.5700, 77.3100)
        partnerUserB = new User();
        partnerUserB.setName("Partner B Delhi");
        partnerUserB.setEmail("partnerB." + suffix + "@delhihub2.com");
        partnerUserB.setPhone("94" + ((System.currentTimeMillis() + 3) % 100000000L));
        partnerUserB.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserB.setRole(UserRole.PARTNER);
        partnerUserB.setIsActive(true);
        partnerUserB = userRepository.save(partnerUserB);
        partnerTokenB = jwtService.generateAccessToken(partnerUserB);

        workshopB = new Workshop(
                partnerUserB,
                "East Delhi Wheels " + suffix,
                partnerUserB.getPhone(),
                partnerUserB.getEmail(),
                "Sector 18 Road",
                "Delhi",
                "Delhi",
                "110092",
                new BigDecimal("28.5700"),
                new BigDecimal("77.3100"),
                new BigDecimal("30.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopB = workshopRepository.save(workshopB);
        workshopServiceRepository.save(new WorkshopService(workshopB, service1, true));
        workshopServiceRepository.save(new WorkshopService(workshopB, service2, true));

        WorkshopWallet walletB = new WorkshopWallet(workshopB, new BigDecimal("500.00"));
        workshopWalletRepository.save(walletB);
    }

    private Long createCustomerServiceRequest() throws Exception {
        CreateServiceRequestRequest req = new CreateServiceRequestRequest();
        req.setVehicleId(customerVehicle.getId());
        req.setServiceIds(List.of(service1.getId(), service2.getId()));
        req.setCity("Delhi");
        req.setAddress("Barakhamba Road, Connaught Place, New Delhi");
        req.setPincode("110001");
        req.setLatitude(new BigDecimal("28.6300"));
        req.setLongitude(new BigDecimal("77.2150"));
        req.setPreferredDate(LocalDate.now().plusDays(2));
        req.setPreferredTimeSlot("10:00 AM - 01:00 PM");
        req.setCustomerNotes("Please check AC gas");

        String res = mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = com.jayway.jsonpath.JsonPath.read(res, "$.data.id");
        return id.longValue();
    }

    @Test
    @DisplayName("Scenario 1: Payment record creation with authoritative snapshot fee")
    void test01_PaymentRecordCreationWithAuthoritativeSnapshotFee() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        InitiatePaymentRequest initReq = new InitiatePaymentRequest(
                oppA.getId(),
                PaymentMethod.RAZORPAY,
                "IDEM-INIT-001"
        );

        mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opportunityId").value(oppA.getId()))
                .andExpect(jsonPath("$.data.amount").value(99.00))
                .andExpect(jsonPath("$.data.paymentStatus").value("CREATED"))
                .andExpect(jsonPath("$.data.paymentMethod").value("RAZORPAY"))
                .andExpect(jsonPath("$.data.razorpayOrderId").isNotEmpty());

        // Verify in database
        List<WorkshopPayment> payments = workshopPaymentRepository.findByOpportunityId(oppA.getId());
        assertEquals(1, payments.size());
        WorkshopPayment p = payments.get(0);
        assertEquals(new BigDecimal("99.00").setScale(2, RoundingMode.HALF_UP), p.getAmount().setScale(2, RoundingMode.HALF_UP));
        assertEquals(PaymentStatus.CREATED, p.getPaymentStatus());
        assertEquals(PaymentMethod.RAZORPAY, p.getPaymentMethod());

        // Verify opportunity status is PAYMENT_PENDING
        LeadOpportunity updatedOpp = leadOpportunityRepository.findById(oppA.getId()).orElseThrow();
        assertEquals(OpportunityStatus.PAYMENT_PENDING, updatedOpp.getStatus());
    }

    @Test
    @DisplayName("Scenario 2: Frontend cannot control or alter payment fee")
    void test02_FrontendCannotControlPaymentFee() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Even if an attacker attempts to inject a custom amount in JSON payload
        String maliciousPayload = "{\"opportunityId\":" + oppA.getId() + ",\"paymentMethod\":\"RAZORPAY\",\"amount\":1.00,\"idempotencyKey\":\"IDEM-HACK-1\"}";

        mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(99.00)); // Authoritative fee strictly enforced

        WorkshopPayment p = workshopPaymentRepository.findByOpportunityId(oppA.getId()).get(0);
        assertEquals(new BigDecimal("99.00").setScale(2, RoundingMode.HALF_UP), p.getAmount().setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Scenario 3: Duplicate payment request / Idempotency check")
    void test03_DuplicatePaymentIdempotency() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        InitiatePaymentRequest initReq = new InitiatePaymentRequest(
                oppA.getId(),
                PaymentMethod.RAZORPAY,
                "IDEM-DUPLICATE-KEY-999"
        );

        // First call
        String res1 = mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number paymentId1 = com.jayway.jsonpath.JsonPath.read(res1, "$.data.paymentId");

        // Second call with same idempotency key
        String res2 = mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number paymentId2 = com.jayway.jsonpath.JsonPath.read(res2, "$.data.paymentId");

        assertEquals(paymentId1.longValue(), paymentId2.longValue(), "Idempotent calls must return the same payment record");

        // Verify only 1 payment was created in DB
        List<WorkshopPayment> list = workshopPaymentRepository.findByOpportunityId(oppA.getId());
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Scenario 4: Workshop ownership isolation - Workshop cannot pay for another's opportunity")
    void test04_WorkshopOwnershipIsolation() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Workshop B tries to initiate payment on Workshop A's opportunity
        InitiatePaymentRequest initReq = new InitiatePaymentRequest(
                oppA.getId(),
                PaymentMethod.RAZORPAY,
                "IDEM-CROSS-01"
        );

        mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isBadRequest()); // Rejected due to unauthorized opportunity ownership

        // Workshop B tries to wallet-pay Workshop A's opportunity
        WalletClaimPaymentRequest walletReq = new WalletClaimPaymentRequest(oppA.getId(), "IDEM-CROSS-02");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Scenario 5: Cross-workshop payment access rejected")
    void test05_CrossWorkshopPaymentAccessRejected() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        InitiatePaymentRequest initReq = new InitiatePaymentRequest(
                oppA.getId(),
                PaymentMethod.RAZORPAY,
                "IDEM-ACCESS-01"
        );

        String res = mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number paymentId = com.jayway.jsonpath.JsonPath.read(res, "$.data.paymentId");

        // Workshop B tries to view Workshop A's payment details
        mockMvc.perform(get("/api/v1/partner/payments/" + paymentId)
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isBadRequest());

        // Workshop B tries to cancel Workshop A's payment
        mockMvc.perform(post("/api/v1/partner/payments/" + paymentId + "/cancel")
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isBadRequest());

        // Workshop A can view own payment details
        mockMvc.perform(get("/api/v1/partner/payments/" + paymentId)
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(paymentId.longValue()));
    }

    @Test
    @DisplayName("Scenario 6: Transfer creates independent payment lifecycle")
    void test06_TransferCreatesIndependentPaymentLifecycle() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Workshop A transfers opportunity
        TransferOpportunityRequest transferReq = new TransferOpportunityRequest("Workshop capacity full this week");
        mockMvc.perform(post("/api/v1/partner/opportunities/" + oppA.getId() + "/transfer")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk());

        // Verify oppA is TRANSFERRED
        LeadOpportunity afterTransfer = leadOpportunityRepository.findById(oppA.getId()).orElseThrow();
        assertEquals(OpportunityStatus.TRANSFERRED, afterTransfer.getStatus());

        // Workshop A cannot pay for transferred opportunity
        WalletClaimPaymentRequest walletReq = new WalletClaimPaymentRequest(oppA.getId(), "IDEM-TRANS-01");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Scenario 7: Payment cannot unlock customer before winning")
    void test07_PaymentCannotUnlockCustomerBeforeWinning() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Initiate payment (in CREATED state, not won yet)
        InitiatePaymentRequest initReq = new InitiatePaymentRequest(
                oppA.getId(),
                PaymentMethod.RAZORPAY,
                "IDEM-UNLOCK-TEST"
        );
        mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk());

        // Check opportunity details: phone MUST still be masked
        mockMvc.perform(get("/api/v1/partner/opportunities/" + oppA.getId())
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.masked").value(true))
                .andExpect(jsonPath("$.data.customerProfile.phone").value("**********"));
    }

    @Test
    @DisplayName("Scenario 8: Payment cannot claim already-assigned request")
    void test08_PaymentCannotClaimAlreadyAssignedRequest() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        LeadOpportunity oppB = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopB.getId()))
                .findFirst().orElseThrow();

        // Workshop A claims first via wallet
        WalletClaimPaymentRequest walletReqA = new WalletClaimPaymentRequest(oppA.getId(), "IDEM-CLAIM-A");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletReqA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        // Service request is now assigned to Workshop A
        ServiceRequest sr = serviceRequestRepository.findById(reqId).orElseThrow();
        assertEquals(workshopA.getId(), sr.getAssignedWorkshop().getId());
        assertEquals(ServiceRequestStatus.ACCEPTED, sr.getStatus());
    }

    @Test
    @DisplayName("Scenario 9: Concurrency and Race Condition - Single winner unlocks, losing workshop refunded")
    void test09_ConcurrencyAndRaceCondition_LosingWorkshopRefunded() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        LeadOpportunity oppB = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopB.getId()))
                .findFirst().orElseThrow();

        BigDecimal initialWalletA = workshopWalletRepository.findByWorkshopId(workshopA.getId()).get().getBalance();
        BigDecimal initialWalletB = workshopWalletRepository.findByWorkshopId(workshopB.getId()).get().getBalance();

        // Step 1: Workshop A starts payment first
        InitiatePaymentRequest initReqA = new InitiatePaymentRequest(
                oppA.getId(),
                PaymentMethod.WALLET,
                "IDEM-RACE-A-INIT"
        );
        mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReqA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("CREATED"));

        // Step 2: Workshop B pays via wallet and wins the atomic claim
        WalletClaimPaymentRequest walletReqB = new WalletClaimPaymentRequest(oppB.getId(), "IDEM-RACE-B");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletReqB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        // Verify Workshop B won
        LeadOpportunity updatedOppB = leadOpportunityRepository.findById(oppB.getId()).orElseThrow();
        assertTrue(updatedOppB.isCustomerDetailsUnlocked());
        assertEquals(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED, updatedOppB.getStatus());

        // Check B's opportunity endpoint - unmasked phone visible
        mockMvc.perform(get("/api/v1/partner/opportunities/" + oppB.getId())
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(true))
                .andExpect(jsonPath("$.data.customerProfile.masked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.phone").value(customer.getPhone()));

        // Step 3: Workshop A's payment subsequently succeeds
        WalletClaimPaymentRequest walletReqA = new WalletClaimPaymentRequest(oppA.getId(), "IDEM-RACE-A");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletReqA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("REFUNDED")); // Wallet payment was immediately refunded

        // Verify Workshop A's opportunity status is LOST
        LeadOpportunity updatedOppA = leadOpportunityRepository.findById(oppA.getId()).orElseThrow();
        assertEquals(OpportunityStatus.LOST, updatedOppA.getStatus());
        assertFalse(updatedOppA.isCustomerDetailsUnlocked());

        // Verify Workshop A's customer details remain masked
        mockMvc.perform(get("/api/v1/partner/opportunities/" + oppA.getId())
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.masked").value(true))
                .andExpect(jsonPath("$.data.customerProfile.phone").value("**********"));

        // Verify Workshop A's wallet balance was restored by the automatic refund
        BigDecimal finalWalletA = workshopWalletRepository.findByWorkshopId(workshopA.getId()).get().getBalance();
        assertEquals(initialWalletA.setScale(2, RoundingMode.HALF_UP), finalWalletA.setScale(2, RoundingMode.HALF_UP));

        // Verify Workshop B's wallet balance was deducted (₹500 - ₹99 = ₹401)
        BigDecimal finalWalletB = workshopWalletRepository.findByWorkshopId(workshopB.getId()).get().getBalance();
        assertEquals(initialWalletB.subtract(new BigDecimal("99.00")).setScale(2, RoundingMode.HALF_UP),
                finalWalletB.setScale(2, RoundingMode.HALF_UP));

        // Step 4: Verify permanent auditable Refund record created for Workshop A
        List<Refund> refundsA = refundRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopA.getId());
        assertEquals(1, refundsA.size());
        Refund refund = refundsA.get(0);
        assertEquals(RefundReason.OPPORTUNITY_ALREADY_ASSIGNED, refund.getRefundReason());
        assertEquals(RefundStatus.SUCCESS, refund.getRefundStatus());
        assertEquals(new BigDecimal("99.00").setScale(2, RoundingMode.HALF_UP), refund.getRefundAmount().setScale(2, RoundingMode.HALF_UP));
        assertNotNull(refund.getProcessedAt());
    }

    @Test
    @DisplayName("Scenario 10: Refund record creation details and structure")
    void test10_RefundRecordCreationDetails() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        LeadOpportunity oppB = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopB.getId()))
                .findFirst().orElseThrow();

        // Workshop B starts payment first
        workshopPaymentService.initiatePayment(partnerUserB, new InitiatePaymentRequest(oppB.getId(), PaymentMethod.WALLET, "INIT-LOST-1"));

        // Workshop A claims and wins
        workshopPaymentService.claimOpportunityWithWallet(partnerUserA, oppA.getId(), "CLAIM-WIN-1");

        // Workshop B loses race condition
        workshopPaymentService.claimOpportunityWithWallet(partnerUserB, oppB.getId(), "CLAIM-LOST-1");

        List<Refund> refunds = refundRepository.findByWorkshopIdOrderByCreatedAtDesc(workshopB.getId());
        assertFalse(refunds.isEmpty());
        Refund r = refunds.get(0);

        assertEquals(workshopB.getId(), r.getWorkshop().getId());
        assertEquals(oppB.getId(), r.getOpportunity().getId());
        assertEquals(new BigDecimal("99.00").setScale(2, RoundingMode.HALF_UP), r.getRefundAmount().setScale(2, RoundingMode.HALF_UP));
        assertEquals(RefundReason.OPPORTUNITY_ALREADY_ASSIGNED, r.getRefundReason());
        assertEquals(RefundStatus.SUCCESS, r.getRefundStatus());
        assertNotNull(r.getInitiatedAt());
        assertNotNull(r.getProcessedAt());
    }

    @Test
    @DisplayName("Scenario 11: Financial records are never deleted")
    void test11_FinancialRecordsAreNeverDeleted() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        workshopPaymentService.claimOpportunityWithWallet(partnerUserA, oppA.getId(), "CLAIM-FIN-1");

        List<WorkshopPayment> payments = workshopPaymentRepository.findByOpportunityId(oppA.getId());
        assertFalse(payments.isEmpty());

        WorkshopPayment payment = payments.get(0);
        assertNotNull(payment.getId());
        assertNotNull(payment.getCreatedAt());

        // Verify permanent existence
        assertTrue(workshopPaymentRepository.existsById(payment.getId()));
    }

    @Test
    @DisplayName("Scenario 12: Admin can audit all refunds; partner only sees own refunds")
    void test12_AdminCanAuditAllRefunds_PartnerIsolation() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        LeadOpportunity oppB = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopB.getId()))
                .findFirst().orElseThrow();

        // Workshop B starts payment first
        workshopPaymentService.initiatePayment(partnerUserB, new InitiatePaymentRequest(oppB.getId(), PaymentMethod.WALLET, "INIT-LOST-AUDIT"));

        // Workshop A claims and wins
        workshopPaymentService.claimOpportunityWithWallet(partnerUserA, oppA.getId(), "CLAIM-WIN-AUDIT");

        // Workshop B loses race condition and gets refund
        workshopPaymentService.claimOpportunityWithWallet(partnerUserB, oppB.getId(), "CLAIM-LOST-AUDIT");

        // 1. Partner B views own refunds -> exactly 1 refund record
        mockMvc.perform(get("/api/v1/partner/refunds")
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].refundReason").value("OPPORTUNITY_ALREADY_ASSIGNED"))
                .andExpect(jsonPath("$.data[0].workshopId").value(workshopB.getId()));

        // 2. Partner A views own refunds -> 0 refunds for this workshop
        mockMvc.perform(get("/api/v1/partner/refunds")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 3. Admin audits all refunds -> sees Partner B's refund
        mockMvc.perform(get("/api/v1/admin/marketplace/refunds")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }
}

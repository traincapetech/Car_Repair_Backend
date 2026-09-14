package com.carservice.backend;

import com.carservice.backend.marketplace.dto.CreateServiceRequestRequest;
import com.carservice.backend.marketplace.dto.InitiatePaymentRequest;
import com.carservice.backend.marketplace.dto.TransferOpportunityRequest;
import com.carservice.backend.marketplace.dto.VerifyPaymentRequest;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "razorpay.enabled=true",
        "razorpay.key-id=rzp_test_testkey123",
        "razorpay.key-secret=secret_test_key_xyz_456",
        "razorpay.webhook-secret=webhook_secret_test_789",
        "razorpay.mock-gateway=true"
})
public class RazorpayPaymentIntegrationTest {

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
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private LeadOpportunityRepository leadOpportunityRepository;

    @Autowired
    private WorkshopPaymentRepository workshopPaymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private PaymentGateway paymentGateway;

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

    private ServiceCatalog service1;
    private ServiceCatalog service2;

    private static final String TEST_KEY_SECRET = "secret_test_key_xyz_456";
    private static final String TEST_WEBHOOK_SECRET = "webhook_secret_test_789";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        platformConfigService.updateLeadAcceptanceFee(new BigDecimal("99.00"));

        if (paymentGateway instanceof FakePaymentGateway fake) {
            fake.setAvailable(true);
            fake.setKeyId("rzp_test_testkey123");
            fake.setKeySecret(TEST_KEY_SECRET);
            fake.setWebhookSecret(TEST_WEBHOOK_SECRET);
            fake.setSimulateOrderFailure(false);
            fake.setSimulateSignatureFailure(false);
            fake.setSimulateRefundFailure(false);
            fake.setSimulatedRefundStatus("processed");
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Setup Customer
        customer = new User();
        customer.setName("Kavita Rao");
        customer.setEmail("kavita." + suffix + "@customer.com");
        customer.setPhone("98" + (System.currentTimeMillis() % 100000000L));
        customer.setPassword(passwordEncoder.encode("Password@123"));
        customer.setRole(UserRole.CUSTOMER);
        customer.setIsActive(true);
        customer = userRepository.save(customer);
        customerToken = jwtService.generateAccessToken(customer);

        customerVehicle = new Vehicle(
                customer,
                "Honda",
                "City",
                2021,
                "DL03" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                FuelType.PETROL,
                Transmission.AUTOMATIC
        );
        customerVehicle = vehicleRepository.save(customerVehicle);

        // Setup Services
        service1 = new ServiceCatalog(
                "Full Service " + suffix,
                "Complete vehicle periodic maintenance",
                ServiceCategory.PERIODIC_SERVICE,
                new BigDecimal("2500.00"),
                DiscountType.PERCENTAGE,
                new BigDecimal("5.00"),
                120,
                true
        );
        service1 = serviceCatalogRepository.save(service1);

        service2 = new ServiceCatalog(
                "Brake Inspection " + suffix,
                "Brake pad replacement and bleeding",
                ServiceCategory.BRAKE_SERVICE,
                new BigDecimal("1200.00"),
                DiscountType.NO_DISCOUNT,
                BigDecimal.ZERO,
                45,
                true
        );
        service2 = serviceCatalogRepository.save(service2);

        // Setup Workshop A
        partnerUserA = new User();
        partnerUserA.setName("Partner Apex Auto");
        partnerUserA.setEmail("partner.apex." + suffix + "@apex.com");
        partnerUserA.setPhone("95" + ((System.currentTimeMillis() + 1) % 100000000L));
        partnerUserA.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserA.setRole(UserRole.PARTNER);
        partnerUserA.setIsActive(true);
        partnerUserA = userRepository.save(partnerUserA);
        partnerTokenA = jwtService.generateAccessToken(partnerUserA);

        workshopA = new Workshop(
                partnerUserA,
                "Apex Auto Hub " + suffix,
                partnerUserA.getPhone(),
                partnerUserA.getEmail(),
                "Barakhamba Road",
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
        workshopWalletRepository.save(new WorkshopWallet(workshopA, new BigDecimal("500.00")));

        // Setup Workshop B
        partnerUserB = new User();
        partnerUserB.setName("Partner Speed Motors");
        partnerUserB.setEmail("partner.speed." + suffix + "@speed.com");
        partnerUserB.setPhone("94" + ((System.currentTimeMillis() + 2) % 100000000L));
        partnerUserB.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUserB.setRole(UserRole.PARTNER);
        partnerUserB.setIsActive(true);
        partnerUserB = userRepository.save(partnerUserB);
        partnerTokenB = jwtService.generateAccessToken(partnerUserB);

        workshopB = new Workshop(
                partnerUserB,
                "Speed Motors " + suffix,
                partnerUserB.getPhone(),
                partnerUserB.getEmail(),
                "Pahar Ganj",
                "Delhi",
                "Delhi",
                "110055",
                new BigDecimal("28.6400"),
                new BigDecimal("77.2100"),
                new BigDecimal("30.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopB = workshopRepository.save(workshopB);
        workshopServiceRepository.save(new WorkshopService(workshopB, service1, true));
        workshopServiceRepository.save(new WorkshopService(workshopB, service2, true));
        workshopWalletRepository.save(new WorkshopWallet(workshopB, new BigDecimal("500.00")));
    }

    private Long createCustomerServiceRequest() throws Exception {
        CreateServiceRequestRequest req = new CreateServiceRequestRequest();
        req.setVehicleId(customerVehicle.getId());
        req.setServiceIds(List.of(service1.getId(), service2.getId()));
        req.setCity("Delhi");
        req.setAddress("Connaught Place, New Delhi");
        req.setPincode("110001");
        req.setLatitude(new BigDecimal("28.6315"));
        req.setLongitude(new BigDecimal("77.2167"));
        req.setPreferredDate(LocalDate.now().plusDays(2));
        req.setPreferredTimeSlot("10:00 AM - 01:00 PM");
        req.setCustomerNotes("Please check AC & brakes");

        String res = mockMvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number id = com.jayway.jsonpath.JsonPath.read(res, "$.data.id");
        return id.longValue();
    }

    private String generateSignature(String orderId, String paymentId, String secret) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateWebhookSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Requirements 3, 4, 5, 6: Razorpay order creation with authoritative fee snapshot; client fee injection rejected")
    void testRazorpayOrderCreation_AuthoritativeFee() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Attacker attempts to inject custom amount = 5.00
        String maliciousPayload = "{\"opportunityId\":" + oppA.getId() + ",\"paymentMethod\":\"RAZORPAY\",\"amount\":5.00,\"idempotencyKey\":\"IDEM-TEST-FEE-1\"}";

        mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opportunityId").value(oppA.getId()))
                .andExpect(jsonPath("$.data.amount").value(99.00)) // Server authoritative fee snapshot enforced
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.paymentMethod").value("RAZORPAY"))
                .andExpect(jsonPath("$.data.paymentStatus").value("CREATED"))
                .andExpect(jsonPath("$.data.razorpayOrderId").isNotEmpty())
                .andExpect(jsonPath("$.data.razorpayKeyId").isNotEmpty());
    }

    @Test
    @DisplayName("Requirements 7 & 8: Invalid Razorpay signature rejected with 400; valid signature accepted")
    void testRazorpaySignatureVerification() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        InitiatePaymentRequest initReq = new InitiatePaymentRequest(oppA.getId(), PaymentMethod.RAZORPAY, "IDEM-SIG-1");
        String initRes = mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number paymentIdNum = com.jayway.jsonpath.JsonPath.read(initRes, "$.data.paymentId");
        Long paymentId = paymentIdNum.longValue();
        String orderId = com.jayway.jsonpath.JsonPath.read(initRes, "$.data.razorpayOrderId");
        String paymentRef = "pay_fake_test_001";

        // 1. Invalid signature submitted -> must fail with 400 Bad Request
        VerifyPaymentRequest invalidReq = new VerifyPaymentRequest(orderId, paymentRef, "invalid_forged_signature_hex_123");
        mockMvc.perform(post("/api/v1/partner/payments/" + paymentId + "/verify-razorpay")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("invalid signature")));

        // Payment status must be recorded as FAILED
        WorkshopPayment failedPayment = workshopPaymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.FAILED, failedPayment.getPaymentStatus());
        assertEquals("Invalid Razorpay signature", failedPayment.getFailureReason());

        // Opportunity details MUST still remain locked
        LeadOpportunity oppCheck = leadOpportunityRepository.findById(oppA.getId()).orElseThrow();
        assertFalse(oppCheck.isCustomerDetailsUnlocked());

        // 2. Initiate fresh attempt and verify with valid HMAC-SHA256 signature
        InitiatePaymentRequest initReq2 = new InitiatePaymentRequest(oppA.getId(), PaymentMethod.RAZORPAY, "IDEM-SIG-2");
        String initRes2 = mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq2)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number paymentId2Num = com.jayway.jsonpath.JsonPath.read(initRes2, "$.data.paymentId");
        Long paymentId2 = paymentId2Num.longValue();
        String orderId2 = com.jayway.jsonpath.JsonPath.read(initRes2, "$.data.razorpayOrderId");
        String paymentRef2 = "pay_valid_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String validSig = generateSignature(orderId2, paymentRef2, TEST_KEY_SECRET);

        VerifyPaymentRequest validReq = new VerifyPaymentRequest(orderId2, paymentRef2, validSig);
        mockMvc.perform(post("/api/v1/partner/payments/" + paymentId2 + "/verify-razorpay")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.razorpayPaymentId").value(paymentRef2));

        // Customer details unlocked for winner
        LeadOpportunity wonOpp = leadOpportunityRepository.findById(oppA.getId()).orElseThrow();
        assertTrue(wonOpp.isCustomerDetailsUnlocked());
        assertEquals(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED, wonOpp.getStatus());
    }

    @Test
    @DisplayName("Requirements 9 & 10: Unauthorized workshop & cross-workshop payment access rejected")
    void testWorkshopAuthorizationAndCrossAccess() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // Workshop B cannot initiate payment for Workshop A's opportunity
        InitiatePaymentRequest initReq = new InitiatePaymentRequest(oppA.getId(), PaymentMethod.RAZORPAY, "IDEM-CROSS-AUTH-1");
        mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isBadRequest());

        // Workshop A initiates payment
        String initRes = mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number paymentIdNum = com.jayway.jsonpath.JsonPath.read(initRes, "$.data.paymentId");
        Long paymentId = paymentIdNum.longValue();

        // Workshop B cannot verify Workshop A's payment
        VerifyPaymentRequest verifyReq = new VerifyPaymentRequest("order_123", "pay_123", "sig_123");
        mockMvc.perform(post("/api/v1/partner/payments/" + paymentId + "/verify-razorpay")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest());

        // Workshop B cannot view Workshop A's payment details
        mockMvc.perform(get("/api/v1/partner/payments/" + paymentId)
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Requirements 13, 14, 15, 16, 17, 18, 19, 21: Race Condition - Workshop A pays via Razorpay, Workshop B wins via Wallet, Workshop A refunded")
    void testRaceCondition_WorkshopARazorpayLost_AutoRefunded() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        LeadOpportunity oppB = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopB.getId()))
                .findFirst().orElseThrow();

        // 1. Workshop A initiates Razorpay payment
        InitiatePaymentRequest initReqA = new InitiatePaymentRequest(oppA.getId(), PaymentMethod.RAZORPAY, "IDEM-RACE-A-INIT");
        String initResA = mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReqA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number paymentIdNumA = com.jayway.jsonpath.JsonPath.read(initResA, "$.data.paymentId");
        Long paymentIdA = paymentIdNumA.longValue();
        String orderIdA = com.jayway.jsonpath.JsonPath.read(initResA, "$.data.razorpayOrderId");

        // 2. Meanwhile Workshop B pays through Wallet and wins the atomic claim!
        WalletClaimPaymentRequest walletReqB = new WalletClaimPaymentRequest(oppB.getId(), "IDEM-RACE-B-WIN");
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletReqB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        // Verify Workshop B won
        LeadOpportunity wonOppB = leadOpportunityRepository.findById(oppB.getId()).orElseThrow();
        assertTrue(wonOppB.isCustomerDetailsUnlocked());
        assertEquals(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED, wonOppB.getStatus());

        // 3. Now Workshop A's Razorpay checkout completes and sends verification callback
        String paymentRefA = "pay_rzp_lost_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String validSigA = generateSignature(orderIdA, paymentRefA, TEST_KEY_SECRET);

        VerifyPaymentRequest verifyReqA = new VerifyPaymentRequest(orderIdA, paymentRefA, validSigA);
        mockMvc.perform(post("/api/v1/partner/payments/" + paymentIdA + "/verify-razorpay")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReqA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("REFUNDED")); // Automatically refunded upon atomic claim loss

        // 4. Verify Workshop A's opportunity status is LOST
        LeadOpportunity lostOppA = leadOpportunityRepository.findById(oppA.getId()).orElseThrow();
        assertEquals(OpportunityStatus.LOST, lostOppA.getStatus());
        assertFalse(lostOppA.isCustomerDetailsUnlocked());

        // 5. Verify customer details remain strictly masked for Workshop A
        mockMvc.perform(get("/api/v1/partner/opportunities/" + oppA.getId())
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerDetailsUnlocked").value(false))
                .andExpect(jsonPath("$.data.customerProfile.masked").value(true))
                .andExpect(jsonPath("$.data.customerProfile.phone").value("**********"));

        // 6. Verify permanent, auditable Refund record created for Workshop A
        List<Refund> refundsA = refundRepository.findByOpportunityId(oppA.getId());
        assertEquals(1, refundsA.size());
        Refund refund = refundsA.get(0);
        assertEquals(workshopA.getId(), refund.getWorkshop().getId());
        assertEquals(new BigDecimal("99.00").setScale(2, RoundingMode.HALF_UP), refund.getRefundAmount().setScale(2, RoundingMode.HALF_UP));
        assertEquals(RefundReason.OPPORTUNITY_ALREADY_ASSIGNED, refund.getRefundReason());
        assertEquals(RefundStatus.SUCCESS, refund.getRefundStatus());
        assertNotNull(refund.getRazorpayRefundId());
        assertTrue(refund.getRazorpayRefundId().startsWith("rfnd_"));
        assertNotNull(refund.getProcessedAt());

        // 7. Verify WorkshopPayment is permanently preserved in DB with status REFUNDED
        WorkshopPayment finalPaymentA = workshopPaymentRepository.findById(paymentIdA).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, finalPaymentA.getPaymentStatus());
        assertEquals(paymentRefA, finalPaymentA.getRazorpayPaymentId());
    }

    @Test
    @DisplayName("Requirement 20: Gateway refund failure transitions payment to REFUND_FAILED")
    void testRazorpayRefundFailureTransitionsToRefundFailed() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        LeadOpportunity oppB = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopB.getId()))
                .findFirst().orElseThrow();

        // Workshop A initiates Razorpay payment
        InitiatePaymentRequest initReqA = new InitiatePaymentRequest(oppA.getId(), PaymentMethod.RAZORPAY, "IDEM-REF-FAIL-1");
        String initResA = mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReqA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number paymentIdNumA = com.jayway.jsonpath.JsonPath.read(initResA, "$.data.paymentId");
        Long paymentIdA = paymentIdNumA.longValue();
        String orderIdA = com.jayway.jsonpath.JsonPath.read(initResA, "$.data.razorpayOrderId");

        // Workshop B wins atomic claim via wallet
        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WalletClaimPaymentRequest(oppB.getId(), "CLAIM-B-WIN"))))
                .andExpect(status().isOk());

        // Configure gateway to simulate refund failure
        if (paymentGateway instanceof FakePaymentGateway fake) {
            fake.setSimulateRefundFailure(true);
        }

        String paymentRefA = "pay_rzp_fail_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String validSigA = generateSignature(orderIdA, paymentRefA, TEST_KEY_SECRET);

        mockMvc.perform(post("/api/v1/partner/payments/" + paymentIdA + "/verify-razorpay")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyPaymentRequest(orderIdA, paymentRefA, validSigA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("REFUND_FAILED"));

        WorkshopPayment payment = workshopPaymentRepository.findById(paymentIdA).orElseThrow();
        assertEquals(PaymentStatus.REFUND_FAILED, payment.getPaymentStatus());

        List<Refund> refunds = refundRepository.findByOpportunityId(oppA.getId());
        assertEquals(1, refunds.size());
        assertEquals(RefundStatus.FAILED, refunds.get(0).getRefundStatus());
    }

    @Test
    @DisplayName("Requirements 22 & 23: Razorpay Webhook signature validation, refund reconciliation, and idempotency")
    void testRazorpayWebhookValidationAndProcessing() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        // 1. Initiate payment
        InitiatePaymentRequest initReq = new InitiatePaymentRequest(oppA.getId(), PaymentMethod.RAZORPAY, "IDEM-WEBHOOK-1");
        String initRes = mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String orderId = com.jayway.jsonpath.JsonPath.read(initRes, "$.data.razorpayOrderId");
        Number paymentIdNum = com.jayway.jsonpath.JsonPath.read(initRes, "$.data.paymentId");
        Long paymentId = paymentIdNum.longValue();

        // 2. Forged webhook signature rejected with 400
        String whPaymentId = "pay_wh_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String webhookPayload = "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"" + whPaymentId + "\",\"order_id\":\"" + orderId + "\",\"error_description\":\"Bank authentication timed out\"}}}}";

        mockMvc.perform(post("/api/v1/payments/razorpay/webhook")
                        .header("X-Razorpay-Signature", "forged_invalid_signature_hex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid webhook signature")));

        // 3. Valid webhook signature accepted
        String validWebhookSig = generateWebhookSignature(webhookPayload, TEST_WEBHOOK_SECRET);

        mockMvc.perform(post("/api/v1/payments/razorpay/webhook")
                        .header("X-Razorpay-Signature", validWebhookSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Payment status marked FAILED
        WorkshopPayment failedPayment = workshopPaymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.FAILED, failedPayment.getPaymentStatus());
        assertEquals("Bank authentication timed out", failedPayment.getFailureReason());

        // 4. Duplicate webhook test (Idempotency) -> must return 200 without error or state distortion
        mockMvc.perform(post("/api/v1/payments/razorpay/webhook")
                        .header("X-Razorpay-Signature", validWebhookSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk());

        WorkshopPayment paymentAfterDuplicate = workshopPaymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.FAILED, paymentAfterDuplicate.getPaymentStatus());
    }

    @Test
    @DisplayName("Requirement 24: Cancelled opportunity cannot be paid via Razorpay")
    void testCancelledOpportunityCannotBePaid() throws Exception {
        Long reqId = createCustomerServiceRequest();

        LeadOpportunity oppA = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshopA.getId()))
                .findFirst().orElseThrow();

        oppA.setStatus(OpportunityStatus.CANCELLED);
        leadOpportunityRepository.save(oppA);

        InitiatePaymentRequest initReq = new InitiatePaymentRequest(oppA.getId(), PaymentMethod.RAZORPAY, "IDEM-CANCELLED-1");

        mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot initiate payment for opportunity in state: CANCELLED")));
    }

    @Test
    @DisplayName("Requirement 25: Transferred opportunity creates independent payment lifecycle")
    void testTransferredOpportunityPaymentProtection() throws Exception {
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

        // Workshop A cannot pay for TRANSFERRED opportunity
        InitiatePaymentRequest initReq = new InitiatePaymentRequest(oppA.getId(), PaymentMethod.RAZORPAY, "IDEM-TRANS-PAY");
        mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot initiate payment for opportunity in state: TRANSFERRED")));
    }
}

package com.carservice.backend;

import com.carservice.backend.marketplace.dto.CreateServiceRequestRequest;
import com.carservice.backend.marketplace.dto.InitiatePaymentRequest;
import com.carservice.backend.marketplace.dto.WalletClaimPaymentRequest;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "razorpay.enabled=false",
        "razorpay.key-id=",
        "razorpay.key-secret=",
        "razorpay.webhook-secret=",
        "razorpay.mock-gateway=false"
})
public class RazorpayStartupNoCredentialsTest {

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
    private User partnerUser;
    private String partnerToken;
    private Workshop workshop;
    private ServiceCatalog service;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        platformConfigService.updateLeadAcceptanceFee(new BigDecimal("99.00"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        customer = new User();
        customer.setName("NoCred Customer");
        customer.setEmail("nocred.customer." + suffix + "@test.com");
        customer.setPhone("98" + (System.currentTimeMillis() % 100000000L));
        customer.setPassword(passwordEncoder.encode("Password@123"));
        customer.setRole(UserRole.CUSTOMER);
        customer.setIsActive(true);
        customer = userRepository.save(customer);
        customerToken = jwtService.generateAccessToken(customer);

        customerVehicle = new Vehicle(
                customer,
                "Maruti",
                "Swift",
                2022,
                "DL02" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(),
                FuelType.PETROL,
                Transmission.MANUAL
        );
        customerVehicle = vehicleRepository.save(customerVehicle);

        service = new ServiceCatalog(
                "Standard Service " + suffix,
                "General inspection",
                ServiceCategory.PERIODIC_SERVICE,
                new BigDecimal("1500.00"),
                DiscountType.NO_DISCOUNT,
                BigDecimal.ZERO,
                60,
                true
        );
        service = serviceCatalogRepository.save(service);

        partnerUser = new User();
        partnerUser.setName("NoCred Partner");
        partnerUser.setEmail("nocred.partner." + suffix + "@workshop.com");
        partnerUser.setPhone("95" + ((System.currentTimeMillis() + 1) % 100000000L));
        partnerUser.setPassword(passwordEncoder.encode("Partner@123"));
        partnerUser.setRole(UserRole.PARTNER);
        partnerUser.setIsActive(true);
        partnerUser = userRepository.save(partnerUser);
        partnerToken = jwtService.generateAccessToken(partnerUser);

        workshop = new Workshop(
                partnerUser,
                "NoCred Auto " + suffix,
                partnerUser.getPhone(),
                partnerUser.getEmail(),
                "Central Market",
                "Delhi",
                "Delhi",
                "110001",
                new BigDecimal("28.6315"),
                new BigDecimal("77.2167"),
                new BigDecimal("25.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshop = workshopRepository.save(workshop);
        workshopServiceRepository.save(new WorkshopService(workshop, service, true));

        WorkshopWallet wallet = new WorkshopWallet(workshop, new BigDecimal("500.00"));
        workshopWalletRepository.save(wallet);
    }

    private Long createCustomerRequest() throws Exception {
        CreateServiceRequestRequest req = new CreateServiceRequestRequest();
        req.setVehicleId(customerVehicle.getId());
        req.setServiceIds(List.of(service.getId()));
        req.setCity("Delhi");
        req.setAddress("Connaught Place, New Delhi");
        req.setPincode("110001");
        req.setLatitude(new BigDecimal("28.6315"));
        req.setLongitude(new BigDecimal("77.2167"));
        req.setPreferredDate(LocalDate.now().plusDays(2));
        req.setPreferredTimeSlot("10:00 AM - 01:00 PM");
        req.setCustomerNotes("Test without Razorpay credentials");

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
    @DisplayName("Requirement 1 & 11: Application starts cleanly without Razorpay credentials, Razorpay returns controlled error, Wallet payment works normally")
    void testStartupWithoutCredentials_RazorpayReturnsControlledError_WalletWorks() throws Exception {
        Long reqId = createCustomerRequest();

        LeadOpportunity opp = leadOpportunityRepository.findByServiceRequestId(reqId).stream()
                .filter(o -> o.getWorkshop().getId().equals(workshop.getId()))
                .findFirst().orElseThrow();

        // 1. Attempting Razorpay payment while credentials are missing returns a clean controlled error
        InitiatePaymentRequest rzpReq = new InitiatePaymentRequest(
                opp.getId(),
                PaymentMethod.RAZORPAY,
                "IDEM-NOCRED-1"
        );

        mockMvc.perform(post("/api/v1/partner/payments/initiate")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rzpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Razorpay payment gateway is currently unavailable."));

        // 2. Existing wallet payment continues to work normally
        WalletClaimPaymentRequest walletReq = new WalletClaimPaymentRequest(opp.getId(), "IDEM-NOCRED-WALLET");

        mockMvc.perform(post("/api/v1/partner/payments/pay-wallet")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.paymentMethod").value("WALLET"));

        // Verify opportunity is claimed and customer details unlocked
        LeadOpportunity wonOpp = leadOpportunityRepository.findById(opp.getId()).orElseThrow();
        assertTrue(wonOpp.isCustomerDetailsUnlocked());
        assertEquals(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED, wonOpp.getStatus());
    }
}

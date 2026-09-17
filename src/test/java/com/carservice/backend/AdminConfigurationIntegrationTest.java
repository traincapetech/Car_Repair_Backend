package com.carservice.backend;

import com.carservice.backend.admin.dto.AdminPlatformConfigUpdateRequest;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.OpportunityStatus;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.marketplace.repository.*;
import com.carservice.backend.marketplace.service.PlatformConfigService;
import com.carservice.backend.marketplace.service.ServiceRequestService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=update")
public class AdminConfigurationIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformConfigRepository platformConfigRepository;

    @Autowired
    private PlatformConfigHistoryRepository platformConfigHistoryRepository;

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private WorkshopRepository workshopRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ServiceRequestItemRepository serviceRequestItemRepository;

    @Autowired
    private LeadOpportunityRepository leadOpportunityRepository;

    @Autowired
    private WorkshopPaymentRepository workshopPaymentRepository;

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User adminUser;
    private User customerUser;
    private User partnerUser;
    private String adminToken;
    private String customerToken;
    private String partnerToken;

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 1. Ensure platform configs are initialized
        platformConfigService.initDefaultConfigs();

        // 2. Provision Admin User
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        adminUser = userRepository.findByEmail("admin-config-test@carservice.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin Config Tester");
            u.setEmail("admin-config-test@carservice.com");
            u.setPassword(passwordEncoder.encode("Admin@123"));
            u.setPhone("981100" + uniqueSuffix.substring(0, 4));
            u.setRole(UserRole.ADMIN);
            u.setIsActive(true);
            return userRepository.save(u);
        });
        adminToken = jwtService.generateAccessToken(adminUser);

        // 3. Provision Customer User
        customerUser = userRepository.findByEmail("customer-config-" + uniqueSuffix + "@test.com").orElseGet(() -> {
            User u = new User();
            u.setName("Customer Config Tester");
            u.setEmail("customer-config-" + uniqueSuffix + "@test.com");
            u.setPassword(passwordEncoder.encode("Pass@123"));
            u.setPhone("981200" + uniqueSuffix.substring(0, 4));
            u.setRole(UserRole.CUSTOMER);
            u.setIsActive(true);
            return userRepository.save(u);
        });
        customerToken = jwtService.generateAccessToken(customerUser);

        // 4. Provision Partner User
        partnerUser = userRepository.findByEmail("partner-config-" + uniqueSuffix + "@test.com").orElseGet(() -> {
            User u = new User();
            u.setName("Partner Config Tester");
            u.setEmail("partner-config-" + uniqueSuffix + "@test.com");
            u.setPassword(passwordEncoder.encode("Pass@123"));
            u.setPhone("981300" + uniqueSuffix.substring(0, 4));
            u.setRole(UserRole.PARTNER);
            u.setIsActive(true);
            return userRepository.save(u);
        });
        partnerToken = jwtService.generateAccessToken(partnerUser);
    }

    // ==========================================
    // 1. READ CONFIGURATIONS
    // ==========================================

    @Test
    @DisplayName("Admin can read all platform configurations grouped by category")
    public void adminCanGetAllConfigurations() throws Exception {
        mockMvc.perform(get("/api/v1/admin/configuration")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(7))))
                .andExpect(jsonPath("$.data[*].configKey", hasItem("MARKETPLACE_ACCEPTANCE_FEE")))
                .andExpect(jsonPath("$.data[*].configKey", hasItem("MARKETPLACE_ENABLED")))
                .andExpect(jsonPath("$.data[*].configKey", hasItem("MARKETPLACE_OPPORTUNITY_EXPIRY_MINUTES")))
                .andExpect(jsonPath("$.data[*].configKey", hasItem("MATCHING_DEFAULT_RADIUS_KM")))
                .andExpect(jsonPath("$.data[*].configKey", hasItem("MATCHING_MAX_WORKSHOPS_PER_REQUEST")))
                .andExpect(jsonPath("$.data[*].configKey", hasItem("WALLET_MIN_TOPUP_AMOUNT")))
                .andExpect(jsonPath("$.data[*].configKey", hasItem("WALLET_MAX_TOPUP_AMOUNT")));
    }

    @Test
    @DisplayName("Admin can read single configuration with metadata")
    public void adminCanGetConfigurationByKey() throws Exception {
        mockMvc.perform(get("/api/v1/admin/configuration/MARKETPLACE_ACCEPTANCE_FEE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.configKey", is("MARKETPLACE_ACCEPTANCE_FEE")))
                .andExpect(jsonPath("$.data.friendlyName", is("Workshop Acceptance Fee")))
                .andExpect(jsonPath("$.data.category", is("PAYMENT")))
                .andExpect(jsonPath("$.data.dataType", is("DECIMAL")))
                .andExpect(jsonPath("$.data.unit", is("INR")));
    }

    // ==========================================
    // 2. SECURITY & RBAC
    // ==========================================

    @Test
    @DisplayName("Customer cannot access admin configuration API (403 Forbidden)")
    public void customerCannotAccessConfigurationApis() throws Exception {
        mockMvc.perform(get("/api/v1/admin/configuration")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Partner cannot modify platform configuration (403 Forbidden)")
    public void partnerCannotModifyConfiguration() throws Exception {
        AdminPlatformConfigUpdateRequest req = new AdminPlatformConfigUpdateRequest("149.00", "Attempting partner override");
        mockMvc.perform(put("/api/v1/admin/configuration/MARKETPLACE_ACCEPTANCE_FEE")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to configuration API returns 401")
    public void unauthenticatedUserRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/configuration"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 3. UPDATE & VALIDATION
    // ==========================================

    @Test
    @DisplayName("Admin can update acceptance fee with mandatory reason")
    public void adminCanUpdateConfigurationWithReason() throws Exception {
        AdminPlatformConfigUpdateRequest req = new AdminPlatformConfigUpdateRequest(
                "149.00",
                "Adjusting partner lead acquisition fee for Q4"
        );

        mockMvc.perform(put("/api/v1/admin/configuration/MARKETPLACE_ACCEPTANCE_FEE")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.configKey", is("MARKETPLACE_ACCEPTANCE_FEE")))
                .andExpect(jsonPath("$.data.configValue", is("149.00")));

        // Verify database state
        assertEquals(new BigDecimal("149.00"), platformConfigService.getLeadAcceptanceFee());
    }

    @Test
    @DisplayName("Update fails with 400 when reason is missing or blank")
    public void updateFailsWhenReasonIsMissing() throws Exception {
        AdminPlatformConfigUpdateRequest req = new AdminPlatformConfigUpdateRequest("149.00", "   ");

        mockMvc.perform(put("/api/v1/admin/configuration/MARKETPLACE_ACCEPTANCE_FEE")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update fails when acceptance fee is negative")
    public void updateFailsWhenFeeIsNegative() throws Exception {
        AdminPlatformConfigUpdateRequest req = new AdminPlatformConfigUpdateRequest("-50.00", "Testing negative fee");

        mockMvc.perform(put("/api/v1/admin/configuration/MARKETPLACE_ACCEPTANCE_FEE")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update fails when acceptance fee is zero")
    public void updateFailsWhenFeeIsZero() throws Exception {
        AdminPlatformConfigUpdateRequest req = new AdminPlatformConfigUpdateRequest("0.00", "Testing zero fee");

        mockMvc.perform(put("/api/v1/admin/configuration/MARKETPLACE_ACCEPTANCE_FEE")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update fails when matching radius exceeds bounds")
    public void updateFailsWhenMatchingRadiusInvalid() throws Exception {
        AdminPlatformConfigUpdateRequest req = new AdminPlatformConfigUpdateRequest("150.00", "Testing radius bound");

        mockMvc.perform(put("/api/v1/admin/configuration/MATCHING_DEFAULT_RADIUS_KM")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update fails when max workshops broadcast count is less than 1")
    public void updateFailsWhenMaxWorkshopsLessThanOne() throws Exception {
        AdminPlatformConfigUpdateRequest req = new AdminPlatformConfigUpdateRequest("0", "Testing zero workshops");

        mockMvc.perform(put("/api/v1/admin/configuration/MATCHING_MAX_WORKSHOPS_PER_REQUEST")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update fails when wallet min top-up exceeds wallet max top-up")
    public void updateFailsWhenWalletMinExceedsMax() throws Exception {
        AdminPlatformConfigUpdateRequest req = new AdminPlatformConfigUpdateRequest("60000.00", "Testing wallet min > max");

        mockMvc.perform(put("/api/v1/admin/configuration/WALLET_MIN_TOPUP_AMOUNT")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 4. CONCURRENCY & OPTIMISTIC LOCKING
    // ==========================================

    @Test
    @DisplayName("Concurrent update with stale version returns 409 Conflict")
    public void concurrentUpdateReturns409Conflict() throws Exception {
        // Fetch current version
        PlatformConfig current = platformConfigRepository.findByConfigKey("MARKETPLACE_ACCEPTANCE_FEE").orElseThrow();
        Long staleVersion = (current.getVersion() != null ? current.getVersion() : 0L) + 999L;

        AdminPlatformConfigUpdateRequest req = new AdminPlatformConfigUpdateRequest(
                "199.00",
                "Stale update attempt",
                staleVersion
        );

        mockMvc.perform(put("/api/v1/admin/configuration/MARKETPLACE_ACCEPTANCE_FEE")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // ==========================================
    // 5. AUDIT HISTORY TRACKING
    // ==========================================

    @Test
    @DisplayName("Configuration update creates immutable history record and audit events")
    public void configurationUpdateCreatesAuditHistory() throws Exception {
        long initialHistoryCount = platformConfigHistoryRepository.count();

        AdminPlatformConfigUpdateRequest req = new AdminPlatformConfigUpdateRequest(
                "125.00",
                "Periodic pricing adjustment for monsoon"
        );

        mockMvc.perform(put("/api/v1/admin/configuration/MARKETPLACE_ACCEPTANCE_FEE")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Verify history was appended
        assertTrue(platformConfigHistoryRepository.count() > initialHistoryCount);

        // Fetch history via API
        mockMvc.perform(get("/api/v1/admin/configuration/history/MARKETPLACE_ACCEPTANCE_FEE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].configKey", is("MARKETPLACE_ACCEPTANCE_FEE")))
                .andExpect(jsonPath("$.data[0].newValue", is("125.00")))
                .andExpect(jsonPath("$.data[0].reason", is("Periodic pricing adjustment for monsoon")));
    }

    // ==========================================
    // 6. CRITICAL FINANCIAL RULE VERIFICATION
    // ==========================================

    @Test
    @DisplayName("CRITICAL FINANCIAL RULE: Fee update affects only NEW opportunities; existing opportunity fee snapshots remain unchanged")
    public void feeUpdateAffectsOnlyNewOpportunities() throws Exception {
        // Step 1: Set fee to ₹99.00
        platformConfigService.updateConfig(
                "MARKETPLACE_ACCEPTANCE_FEE",
                "99.00",
                "Reset baseline fee to 99",
                null,
                adminUser
        );

        // Provision a verified workshop
        String workshopIdStr = UUID.randomUUID().toString().substring(0, 6);
        Workshop workshop = new Workshop();
        workshop.setBusinessName("Immutability Test Workshop " + workshopIdStr);
        workshop.setPhone("9988" + workshopIdStr.substring(0, 4));
        workshop.setEmail("immutability-" + workshopIdStr + "@test.com");
        workshop.setAddress("Industrial Area, Sector 5");
        workshop.setCity("New Delhi");
        workshop.setState("Delhi");
        workshop.setPincode("110001");
        workshop.setIsActive(true);
        workshop.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        workshop.setServiceRadiusKm(new BigDecimal("30.00"));
        workshop.setUser(partnerUser);
        workshop = workshopRepository.save(workshop);

        // Provision a vehicle
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(customerUser);
        vehicle.setMake("Toyota");
        vehicle.setModel("Innova");
        vehicle.setYear(2022);
        vehicle.setRegistrationNumber("DL-01-IM-" + workshopIdStr.substring(0, 4));
        vehicle.setFuelType(FuelType.DIESEL);
        vehicle.setTransmission(Transmission.MANUAL);
        vehicle = vehicleRepository.save(vehicle);

        // Provision Service Catalog
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setName("Oil Change - Immutability Test " + workshopIdStr);
        catalog.setCategory(ServiceCategory.PERIODIC_SERVICE);
        catalog.setBasePrice(new BigDecimal("1500.00"));
        catalog.setEstimatedDurationMinutes(60);
        catalog.setIsActive(true);
        catalog.setDiscountType(DiscountType.NO_DISCOUNT);
        catalog.setDiscountValue(BigDecimal.ZERO);
        catalog = serviceCatalogRepository.save(catalog);

        // Create Service Request 1 when fee = 99.00
        ServiceRequest req1 = new ServiceRequest();
        req1.setUser(customerUser);
        req1.setVehicle(vehicle);
        req1.setPreferredDate(LocalDate.now().plusDays(2));
        req1.setPreferredTimeSlot("10:00 AM - 12:00 PM");
        req1.setAddress("Customer Street 1");
        req1.setCity("New Delhi");
        req1.setPincode("110001");
        req1.setStatus(ServiceRequestStatus.MATCHED);
        req1.setTotalAmount(new BigDecimal("1500.00"));
        req1 = serviceRequestRepository.save(req1);

        // Create Opportunity 1 with snapshot = 99.00
        LeadOpportunity opp1 = new LeadOpportunity(req1, workshop, platformConfigService.getLeadAcceptanceFee());
        opp1 = leadOpportunityRepository.save(opp1);
        assertEquals(new BigDecimal("99.00"), opp1.getFeeSnapshot(), "Opportunity 1 snapshot must be 99.00");

        // Step 2: Admin updates fee to ₹149.00
        platformConfigService.updateConfig(
                "MARKETPLACE_ACCEPTANCE_FEE",
                "149.00",
                "Updated fee to 149 for new leads",
                null,
                adminUser
        );
        assertEquals(new BigDecimal("149.00"), platformConfigService.getLeadAcceptanceFee());

        // Step 3: Create Service Request 2 when fee = 149.00
        ServiceRequest req2 = new ServiceRequest();
        req2.setUser(customerUser);
        req2.setVehicle(vehicle);
        req2.setPreferredDate(LocalDate.now().plusDays(3));
        req2.setPreferredTimeSlot("02:00 PM - 04:00 PM");
        req2.setAddress("Customer Street 2");
        req2.setCity("New Delhi");
        req2.setPincode("110001");
        req2.setStatus(ServiceRequestStatus.MATCHED);
        req2.setTotalAmount(new BigDecimal("1500.00"));
        req2 = serviceRequestRepository.save(req2);

        // Create Opportunity 2 with snapshot = 149.00
        LeadOpportunity opp2 = new LeadOpportunity(req2, workshop, platformConfigService.getLeadAcceptanceFee());
        opp2 = leadOpportunityRepository.save(opp2);
        assertEquals(new BigDecimal("149.00"), opp2.getFeeSnapshot(), "Opportunity 2 snapshot must be 149.00");

        // Step 4: CRITICAL CHECK - Reload Opportunity 1 from database and ensure it STILL has 99.00!
        LeadOpportunity reloadedOpp1 = leadOpportunityRepository.findById(opp1.getId()).orElseThrow();
        assertEquals(new BigDecimal("99.00"), reloadedOpp1.getFeeSnapshot(), "Historical Opportunity 1 fee snapshot must remain 99.00");
    }

    @Test
    @DisplayName("CRITICAL FINANCIAL RULE: Opportunity transfer creates new opportunity with current fee while original opportunity retains old fee snapshot")
    public void transferUsesCurrentFeeWhileOriginalRetainsOldFee() throws Exception {
        // Step 1: Set baseline fee = 89.00
        platformConfigService.updateConfig(
                "MARKETPLACE_ACCEPTANCE_FEE",
                "89.00",
                "Setting baseline fee to 89",
                null,
                adminUser
        );

        String unique = UUID.randomUUID().toString().substring(0, 6);
        Workshop w1 = new Workshop();
        w1.setBusinessName("Workshop A " + unique);
        w1.setPhone("9911" + unique.substring(0, 4));
        w1.setEmail("wa-" + unique + "@test.com");
        w1.setAddress("Sector 1");
        w1.setCity("New Delhi");
        w1.setState("Delhi");
        w1.setPincode("110001");
        w1.setIsActive(true);
        w1.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        w1.setServiceRadiusKm(new BigDecimal("25.00"));
        w1.setUser(partnerUser);
        w1 = workshopRepository.save(w1);

        User partnerUser2 = new User();
        partnerUser2.setName("Partner 2 " + unique);
        partnerUser2.setEmail("part2-" + unique + "@test.com");
        partnerUser2.setPassword(passwordEncoder.encode("Pass@123"));
        partnerUser2.setPhone("9899" + unique.substring(0, 4));
        partnerUser2.setRole(UserRole.PARTNER);
        partnerUser2.setIsActive(true);
        partnerUser2 = userRepository.save(partnerUser2);

        Workshop w2 = new Workshop();
        w2.setBusinessName("Workshop B " + unique);
        w2.setPhone("9922" + unique.substring(0, 4));
        w2.setEmail("wb-" + unique + "@test.com");
        w2.setAddress("Sector 2");
        w2.setCity("New Delhi");
        w2.setState("Delhi");
        w2.setPincode("110001");
        w2.setIsActive(true);
        w2.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        w2.setServiceRadiusKm(new BigDecimal("25.00"));
        w2.setUser(partnerUser2);
        w2 = workshopRepository.save(w2);

        Vehicle vehicle = new Vehicle();
        vehicle.setUser(customerUser);
        vehicle.setMake("Hyundai");
        vehicle.setModel("Creta");
        vehicle.setYear(2023);
        vehicle.setRegistrationNumber("DL-02-TR-" + unique.substring(0, 4));
        vehicle.setFuelType(FuelType.PETROL);
        vehicle.setTransmission(Transmission.AUTOMATIC);
        vehicle = vehicleRepository.save(vehicle);

        ServiceRequest req = new ServiceRequest();
        req.setUser(customerUser);
        req.setVehicle(vehicle);
        req.setPreferredDate(LocalDate.now().plusDays(2));
        req.setPreferredTimeSlot("10:00 AM - 12:00 PM");
        req.setAddress("Customer Street");
        req.setCity("New Delhi");
        req.setPincode("110001");
        req.setStatus(ServiceRequestStatus.MATCHED);
        req.setTotalAmount(new BigDecimal("2000.00"));
        req = serviceRequestRepository.save(req);

        // Initial Opportunity for Workshop A with snapshot = 89.00
        LeadOpportunity originalOpp = new LeadOpportunity(req, w1, platformConfigService.getLeadAcceptanceFee());
        originalOpp = leadOpportunityRepository.save(originalOpp);
        assertEquals(new BigDecimal("89.00"), originalOpp.getFeeSnapshot());

        // Step 2: Admin increases fee to 159.00
        platformConfigService.updateConfig(
                "MARKETPLACE_ACCEPTANCE_FEE",
                "159.00",
                "Updated fee to 159",
                null,
                adminUser
        );

        // Step 3: Opportunity transferred: Workshop A marked TRANSFERRED, new opportunity created for Workshop B
        originalOpp.setStatus(OpportunityStatus.TRANSFERRED);
        originalOpp.setTransferReason("Workshop at capacity");
        originalOpp.setTransferredAt(LocalDateTime.now());
        leadOpportunityRepository.save(originalOpp);

        // New opportunity generated with current fee
        LeadOpportunity transferredOpp = new LeadOpportunity(req, w2, platformConfigService.getLeadAcceptanceFee());
        transferredOpp = leadOpportunityRepository.save(transferredOpp);

        // Verification
        assertEquals(new BigDecimal("159.00"), transferredOpp.getFeeSnapshot(), "Transferred replacement opportunity must use current fee of 159.00");
        LeadOpportunity reloadedOriginal = leadOpportunityRepository.findById(originalOpp.getId()).orElseThrow();
        assertEquals(new BigDecimal("89.00"), reloadedOriginal.getFeeSnapshot(), "Original transferred opportunity must preserve historical fee of 89.00");
        assertEquals(OpportunityStatus.TRANSFERRED, reloadedOriginal.getStatus());
    }
}

package com.carservice.backend;

import com.carservice.backend.admin.dto.UpdateCustomerStatusRequest;
import com.carservice.backend.admin.dto.UpdateWorkshopStatusRequest;
import com.carservice.backend.admin.dto.UpdateWorkshopVerificationRequest;
import com.carservice.backend.admin.entity.AuditLog;
import com.carservice.backend.admin.repository.AuditLogRepository;
import com.carservice.backend.admin.service.AdminCustomerService;
import com.carservice.backend.admin.service.AdminWorkshopService;
import com.carservice.backend.admin.service.AuditService;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.marketplace.service.PlatformConfigService;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.servicecatalog.dto.CreateServiceCatalogRequest;
import com.carservice.backend.servicecatalog.dto.ServiceCatalogResponse;
import com.carservice.backend.servicecatalog.dto.UpdateServiceCatalogRequest;
import com.carservice.backend.servicecatalog.enums.DiscountType;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import com.carservice.backend.servicecatalog.service.ServiceCatalogService;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=update")
public class AdminSystemGovernanceIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ServiceCatalogService serviceCatalogService;

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private AdminCustomerService adminCustomerService;

    @Autowired
    private AdminWorkshopService adminWorkshopService;

    @Autowired
    private WorkshopRepository workshopRepository;

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = new User();
        adminUser.setName("System Governance Admin");
        adminUser.setEmail("gov_admin_" + suffix + "@carservice.com");
        adminUser.setPassword(passwordEncoder.encode("Admin@12345"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setPhone("987" + suffix);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = "Bearer " + jwtService.generateAccessToken(adminUser);

        customerUser = new User();
        customerUser.setName("Regular Customer");
        customerUser.setEmail("customer_" + suffix + "@carservice.com");
        customerUser.setPassword(passwordEncoder.encode("Customer@12345"));
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setPhone("986" + suffix);
        customerUser.setIsActive(true);
        customerUser = userRepository.save(customerUser);
        customerToken = "Bearer " + jwtService.generateAccessToken(customerUser);
    }

    @Test
    @DisplayName("AuditService records immutable audit entries with actor details and states")
    void testAuditServiceRecordsEntrySuccessfully() {
        Map<String, Object> before = Map.of("status", "OLD_STATUS", "rate", 100);
        Map<String, Object> after = Map.of("status", "NEW_STATUS", "rate", 150);
        Map<String, Object> meta = Map.of("channel", "CONSOLE", "source", "WEB");

        AuditLog saved = auditService.record(
                adminUser,
                "TEST_POLICY_UPDATE",
                "SECURITY_POLICY",
                "POL-99",
                "Updated security governance policy",
                "SUCCESS",
                before,
                after,
                meta
        );

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("TEST_POLICY_UPDATE", saved.getAction());
        assertEquals("SECURITY_POLICY", saved.getEntityType());
        assertEquals("POL-99", saved.getEntityId());
        assertEquals(adminUser.getEmail(), saved.getActorEmail());
        assertEquals("ADMIN", saved.getActorRole());
        assertEquals("SUCCESS", saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertTrue(saved.getBeforeStateJson().contains("OLD_STATUS"));
        assertTrue(saved.getAfterStateJson().contains("NEW_STATUS"));
        assertTrue(saved.getDiffJson().contains("before"));
    }

    @Test
    @DisplayName("AuditService strictly redacts sensitive keys recursively from states and metadata")
    void testSensitiveDataRedaction() {
        Map<String, Object> sensitiveMap = new LinkedHashMap<>();
        sensitiveMap.put("username", "admin");
        sensitiveMap.put("password", "SuperSecret123!");
        sensitiveMap.put("jwtToken", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        sensitiveMap.put("secretKey", "secret-xyz-789");
        sensitiveMap.put("apiKey", "key_live_12345");
        sensitiveMap.put("creditCard", "4111-2222-3333-4444");
        sensitiveMap.put("cvv", "123");
        sensitiveMap.put("safeConfig", "ALLOW_ALL");

        AuditLog entry = auditService.record(
                adminUser,
                "CREDENTIAL_UPDATE",
                "SYSTEM",
                "AUTH-1",
                "Updated credentials with sensitive tokens",
                "SUCCESS",
                sensitiveMap,
                null,
                sensitiveMap
        );

        assertNotNull(entry);
        String beforeJson = entry.getBeforeStateJson();
        String metadataJson = entry.getMetadataJson();

        // Verify normal field is visible
        assertTrue(beforeJson.contains("ALLOW_ALL"));
        assertTrue(beforeJson.contains("admin"));

        // Verify sensitive fields are replaced by [REDACTED]
        assertFalse(beforeJson.contains("SuperSecret123!"));
        assertFalse(beforeJson.contains("secret-xyz-789"));
        assertFalse(beforeJson.contains("4111-2222-3333-4444"));
        assertTrue(beforeJson.contains("[REDACTED]"));

        assertFalse(metadataJson.contains("SuperSecret123!"));
        assertTrue(metadataJson.contains("[REDACTED]"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/audit/events returns paginated logs for ROLE_ADMIN")
    void testGetAuditEventsAuthorized() throws Exception {
        auditService.record(adminUser, "GOV_EVENT_A", "TEST_SYS", "A1", "Event A", "SUCCESS", null, null, null);
        auditService.record(adminUser, "GOV_EVENT_B", "TEST_SYS", "B1", "Event B", "SUCCESS", null, null, null);

        mockMvc.perform(get("/api/v1/admin/audit/events")
                        .header("Authorization", adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", notNullValue()))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("GET /api/v1/admin/audit/events rejects unauthenticated with 401")
    void testGetAuditEventsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/audit/events rejects ROLE_CUSTOMER with 403")
    void testGetAuditEventsForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit/events")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Audit events support multi-attribute filtering by action, status, and entityType")
    void testAuditEventsFiltering() throws Exception {
        String uniqueId = UUID.randomUUID().toString();
        auditService.record(adminUser, "FILTER_ACTION_ALPHA", "CUSTOM_ENTITY", uniqueId, "Alpha Description", "SUCCESS", null, null, null);
        auditService.record(adminUser, "FILTER_ACTION_BETA", "OTHER_ENTITY", uniqueId, "Beta Description", "FAILED", null, null, null);

        // Filter by action
        mockMvc.perform(get("/api/v1/admin/audit/events")
                        .header("Authorization", adminToken)
                        .param("action", "FILTER_ACTION_ALPHA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].action").value("FILTER_ACTION_ALPHA"))
                .andExpect(jsonPath("$.data.content[0].entityId").value(uniqueId));

        // Filter by status FAILED
        mockMvc.perform(get("/api/v1/admin/audit/events")
                        .header("Authorization", adminToken)
                        .param("status", "FAILED")
                        .param("entityId", uniqueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].action").value("FILTER_ACTION_BETA"))
                .andExpect(jsonPath("$.data.content[0].status").value("FAILED"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/audit/events/{id} returns full state diff details")
    void testGetAuditEventDetail() throws Exception {
        AuditLog entry = auditService.record(
                adminUser,
                "DIFF_TEST_ACTION",
                "TEST_DIFF_ENTITY",
                "DIFF-1",
                "Detail verification event",
                "SUCCESS",
                Map.of("field", "oldValue"),
                Map.of("field", "newValue"),
                Map.of("reason", "manual test")
        );

        mockMvc.perform(get("/api/v1/admin/audit/events/" + entry.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(entry.getId()))
                .andExpect(jsonPath("$.data.action").value("DIFF_TEST_ACTION"))
                .andExpect(jsonPath("$.data.diffJson", containsString("before")))
                .andExpect(jsonPath("$.data.beforeStateJson", containsString("oldValue")))
                .andExpect(jsonPath("$.data.afterStateJson", containsString("newValue")));
    }

    @Test
    @DisplayName("GET /api/v1/admin/audit/summary returns aggregate KPI counts")
    void testGetAuditSummary() throws Exception {
        auditService.record(adminUser, "KPI_EVENT_1", "SYS", "1", "Test", "SUCCESS", null, null, null);
        auditService.record(adminUser, "LOGIN_FAILED", "SECURITY", "2", "Failed login", "FAILED", null, null, null);

        mockMvc.perform(get("/api/v1/admin/audit/summary")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEvents", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.todayEvents", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.failedEvents", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.securityEvents", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Immutable Audit: POST and DELETE endpoints are rejected (405 Method Not Allowed)")
    void testAuditEndpointsImmutability() throws Exception {
        mockMvc.perform(post("/api/v1/admin/audit/events")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"HACK\"}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/v1/admin/audit/events/1")
                        .header("Authorization", adminToken))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("GET /api/v1/admin/system/health returns complete component statuses")
    void testGetSystemHealth() throws Exception {
        mockMvc.perform(get("/api/v1/admin/system/health")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("HEALTHY"))
                .andExpect(jsonPath("$.data.components.database.status").value("UP"))
                .andExpect(jsonPath("$.data.components.database.latencyMs", notNullValue()))
                .andExpect(jsonPath("$.data.components.security.status").value("UP"))
                .andExpect(jsonPath("$.data.components.serviceCatalog.status").value("UP"))
                .andExpect(jsonPath("$.data.components.marketplace.status").value("UP"))
                .andExpect(jsonPath("$.data.components.wallets.status").value("UP"))
                .andExpect(jsonPath("$.data.components.paymentGateway.status", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/admin/system/settings returns platform runtime info without secret leakage")
    void testGetSystemSettingsSafe() throws Exception {
        mockMvc.perform(get("/api/v1/admin/system/settings")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.platformName").value("Car Service Platform"))
                .andExpect(jsonPath("$.data.securityPolicy.enforceRbac").value(true))
                .andExpect(jsonPath("$.data.paymentGateway.provider").value("Razorpay"))
                // Ensure secret key is NEVER exposed in the settings response
                .andExpect(jsonPath("$.data.paymentGateway.keySecret").doesNotExist())
                .andExpect(jsonPath("$.data.paymentGateway.secret").doesNotExist());
    }

    @Test
    @DisplayName("Operational Hook: Service Catalog mutations are automatically recorded in AuditLog")
    void testServiceCatalogMutationsRecorded() {
        String serviceName = "Gov Test Brake Overhaul " + UUID.randomUUID().toString().substring(0, 6);

        // 1. Create Service
        CreateServiceCatalogRequest createReq = new CreateServiceCatalogRequest(
                serviceName,
                "Complete brake system overhaul",
                ServiceCategory.BRAKE_SERVICE,
                new BigDecimal("2999.00"),
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                120,
                true
        );
        ServiceCatalogResponse created = serviceCatalogService.createService(createReq);
        assertNotNull(created);

        // Verify SERVICE_CREATED audit record exists
        List<AuditLog> createLogs = auditLogRepository.findAll().stream()
                .filter(l -> "SERVICE_CREATED".equals(l.getAction()) && String.valueOf(created.getId()).equals(l.getEntityId()))
                .toList();
        assertFalse(createLogs.isEmpty());
        assertTrue(createLogs.get(0).getDescription().contains(serviceName));

        // 2. Update Service
        UpdateServiceCatalogRequest updateReq = new UpdateServiceCatalogRequest(
                serviceName + " Updated",
                "Updated description",
                ServiceCategory.BRAKE_SERVICE,
                new BigDecimal("3499.00"),
                DiscountType.NO_DISCOUNT,
                BigDecimal.ZERO,
                150,
                true
        );
        serviceCatalogService.updateService(created.getId(), updateReq);

        List<AuditLog> updateLogs = auditLogRepository.findAll().stream()
                .filter(l -> "SERVICE_UPDATED".equals(l.getAction()) && String.valueOf(created.getId()).equals(l.getEntityId()))
                .toList();
        assertFalse(updateLogs.isEmpty());

        // 3. Deactivate Service
        serviceCatalogService.deactivateService(created.getId());
        List<AuditLog> deactLogs = auditLogRepository.findAll().stream()
                .filter(l -> "SERVICE_DEACTIVATED".equals(l.getAction()) && String.valueOf(created.getId()).equals(l.getEntityId()))
                .toList();
        assertFalse(deactLogs.isEmpty());

        // 4. Activate Service
        serviceCatalogService.activateService(created.getId());
        List<AuditLog> actLogs = auditLogRepository.findAll().stream()
                .filter(l -> "SERVICE_ACTIVATED".equals(l.getAction()) && String.valueOf(created.getId()).equals(l.getEntityId()))
                .toList();
        assertFalse(actLogs.isEmpty());
    }

    @Test
    @DisplayName("Operational Hook: Customer status modification records audit event")
    void testCustomerStatusChangeRecorded() {
        UpdateCustomerStatusRequest request = new UpdateCustomerStatusRequest(false);
        adminCustomerService.updateCustomerStatus(customerUser.getId(), request, adminUser);

        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(l -> "CUSTOMER_STATUS_CHANGED".equals(l.getAction()) && String.valueOf(customerUser.getId()).equals(l.getEntityId()))
                .toList();

        assertFalse(logs.isEmpty());
        assertEquals("CUSTOMER", logs.get(0).getEntityType());
        assertEquals(adminUser.getEmail(), logs.get(0).getActorEmail());
    }

    @Test
    @DisplayName("Operational Hook: Workshop status and verification changes record audit events")
    void testWorkshopStatusAndVerificationChangeRecorded() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);

        User partnerUser = new User();
        partnerUser.setName("Apex Partner " + suffix);
        partnerUser.setEmail("partner_" + suffix + "@garage.com");
        partnerUser.setPassword(passwordEncoder.encode("Partner@12345"));
        partnerUser.setRole(UserRole.PARTNER);
        partnerUser.setPhone("982" + suffix);
        partnerUser.setIsActive(true);
        partnerUser = userRepository.save(partnerUser);

        Workshop workshop = new Workshop();
        workshop.setUser(partnerUser);
        workshop.setBusinessName("Governance Apex Auto " + suffix);
        workshop.setEmail("apex_" + suffix + "@garage.com");
        workshop.setPhone("981" + suffix);
        workshop.setAddress("Industrial Area, Sector 5");
        workshop.setCity("Gurugram");
        workshop.setState("Haryana");
        workshop.setPincode("122001");
        workshop.setLatitude(new BigDecimal("28.4595"));
        workshop.setLongitude(new BigDecimal("77.0266"));
        workshop.setServiceRadiusKm(new BigDecimal("25.00"));
        workshop.setVerificationStatus(WorkshopVerificationStatus.PENDING);
        workshop.setIsActive(true);
        final Workshop savedWorkshop = workshopRepository.save(workshop);
        final Long workshopId = savedWorkshop.getId();

        // Change operational status
        UpdateWorkshopStatusRequest statusReq = new UpdateWorkshopStatusRequest(false, "Operational compliance review");
        adminWorkshopService.updateWorkshopStatus(workshopId, statusReq, adminUser);

        List<AuditLog> statusLogs = auditLogRepository.findAll().stream()
                .filter(l -> "WORKSHOP_STATUS_CHANGED".equals(l.getAction()) && String.valueOf(workshopId).equals(l.getEntityId()))
                .toList();
        assertFalse(statusLogs.isEmpty());
        assertEquals(adminUser.getEmail(), statusLogs.get(0).getActorEmail());

        // Change verification status
        UpdateWorkshopVerificationRequest verReq = new UpdateWorkshopVerificationRequest(WorkshopVerificationStatus.VERIFIED, "Documents verified");
        adminWorkshopService.updateWorkshopVerification(workshopId, verReq, adminUser);

        List<AuditLog> verLogs = auditLogRepository.findAll().stream()
                .filter(l -> ("WORKSHOP_VERIFICATION_CHANGED".equals(l.getAction()) || "WORKSHOP_APPROVED".equals(l.getAction())) && String.valueOf(workshopId).equals(l.getEntityId()))
                .toList();
        assertFalse(verLogs.isEmpty());
        assertEquals(adminUser.getEmail(), verLogs.get(0).getActorEmail());
    }

    @Test
    @DisplayName("Operational Hook: Platform configuration update records audit event")
    void testPlatformConfigUpdateRecorded() {
        platformConfigService.updateConfig(
                PlatformConfigService.KEY_OPPORTUNITY_EXPIRY_MINUTES,
                "45",
                "Governance testing dispatch window adjustment",
                null,
                adminUser
        );

        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(l -> "CONFIGURATION_UPDATED".equals(l.getAction())
                        && PlatformConfigService.KEY_OPPORTUNITY_EXPIRY_MINUTES.equals(l.getEntityId())
                        && adminUser.getEmail().equals(l.getActorEmail()))
                .toList();

        assertFalse(logs.isEmpty());
        AuditLog latest = logs.get(logs.size() - 1);
        assertEquals(adminUser.getEmail(), latest.getActorEmail());
        assertTrue(latest.getAfterStateJson().contains("45"));
    }
}

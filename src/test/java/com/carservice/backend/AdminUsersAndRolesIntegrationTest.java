package com.carservice.backend;

import com.carservice.backend.admin.dto.UpdateUserRoleRequest;
import com.carservice.backend.admin.dto.UpdateUserStatusRequest;
import com.carservice.backend.admin.entity.AuditLog;
import com.carservice.backend.admin.repository.AuditLogRepository;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.enums.UserStatus;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
public class AdminUsersAndRolesIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User superAdminUser;
    private User normalAdminUser;
    private User customerUser;
    private User partnerUser;

    private String superAdminToken;
    private String normalAdminToken;
    private String customerToken;
    private String partnerToken;

    private String runId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Super Admin
        superAdminUser = new User();
        superAdminUser.setName("Super Admin " + runId);
        superAdminUser.setEmail("super_" + runId + "@carservice.com");
        superAdminUser.setPhone("91" + Math.abs(runId.hashCode() % 100000000));
        if (superAdminUser.getPhone().length() < 10) superAdminUser.setPhone("9199" + runId.substring(0, 6));
        superAdminUser.setPassword(passwordEncoder.encode("SuperAdmin@12345"));
        superAdminUser.setRole(UserRole.SUPER_ADMIN);
        superAdminUser.setStatus(UserStatus.ACTIVE);
        superAdminUser.setIsActive(true);
        superAdminUser = userRepository.save(superAdminUser);
        superAdminToken = jwtService.generateAccessToken(superAdminUser);

        // 2. Normal Admin
        normalAdminUser = new User();
        normalAdminUser.setName("Operations Admin " + runId);
        normalAdminUser.setEmail("admin_" + runId + "@carservice.com");
        normalAdminUser.setPhone("92" + Math.abs(runId.hashCode() % 100000000));
        if (normalAdminUser.getPhone().length() < 10) normalAdminUser.setPhone("9299" + runId.substring(0, 6));
        normalAdminUser.setPassword(passwordEncoder.encode("Admin@12345"));
        normalAdminUser.setRole(UserRole.ADMIN);
        normalAdminUser.setStatus(UserStatus.ACTIVE);
        normalAdminUser.setIsActive(true);
        normalAdminUser = userRepository.save(normalAdminUser);
        normalAdminToken = jwtService.generateAccessToken(normalAdminUser);

        // 3. Customer
        customerUser = new User();
        customerUser.setName("Customer " + runId);
        customerUser.setEmail("cust_" + runId + "@carservice.com");
        customerUser.setPhone("93" + Math.abs(runId.hashCode() % 100000000));
        if (customerUser.getPhone().length() < 10) customerUser.setPhone("9399" + runId.substring(0, 6));
        customerUser.setPassword(passwordEncoder.encode("Cust@12345"));
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setStatus(UserStatus.ACTIVE);
        customerUser.setIsActive(true);
        customerUser = userRepository.save(customerUser);
        customerToken = jwtService.generateAccessToken(customerUser);

        // 4. Partner with associated workshop
        partnerUser = new User();
        partnerUser.setName("Partner " + runId);
        partnerUser.setEmail("part_" + runId + "@carservice.com");
        partnerUser.setPhone("94" + Math.abs(runId.hashCode() % 100000000));
        if (partnerUser.getPhone().length() < 10) partnerUser.setPhone("9499" + runId.substring(0, 6));
        partnerUser.setPassword(passwordEncoder.encode("Part@12345"));
        partnerUser.setRole(UserRole.PARTNER);
        partnerUser.setStatus(UserStatus.ACTIVE);
        partnerUser.setIsActive(true);
        partnerUser = userRepository.save(partnerUser);
        partnerToken = jwtService.generateAccessToken(partnerUser);

        Workshop workshop = new Workshop();
        workshop.setUser(partnerUser);
        workshop.setBusinessName("Garage Apex " + runId);
        workshop.setPhone(partnerUser.getPhone());
        workshop.setEmail(partnerUser.getEmail());
        workshop.setAddress("42 Industrial Area, Sector 5");
        workshop.setCity("Mumbai");
        workshop.setState("Maharashtra");
        workshop.setPincode("400001");
        workshop.setServiceRadiusKm(new BigDecimal("15.00"));
        workshop.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        workshop.setIsActive(true);
        workshopRepository.save(workshop);

        // Create vehicle for customer
        Vehicle v = new Vehicle();
        v.setUser(customerUser);
        v.setMake("Toyota");
        v.setModel("Innova");
        v.setYear(2023);
        v.setRegistrationNumber("MH" + runId.substring(0, 2).toUpperCase() + "XY" + (int) (Math.random() * 8999 + 1000));
        v.setFuelType(FuelType.DIESEL);
        v.setTransmission(Transmission.MANUAL);
        vehicleRepository.save(v);
    }

    @Test
    @DisplayName("Security: Unauthenticated access to /api/v1/admin/users must return 401")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: Non-admin users (Customer / Partner) accessing /api/v1/admin/users must return 403")
    void testNonAdminAccessReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Users List: Admin can retrieve paginated user list with sensitive fields withheld")
    void testGetUsersListPaginated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + normalAdminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(4)));
    }

    @Test
    @DisplayName("Users Filter: Search query filters correctly across name and email")
    void testGetUsersWithSearchFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + normalAdminToken)
                        .param("search", "cust_" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].email").value(customerUser.getEmail()));
    }

    @Test
    @DisplayName("Users Filter: Role filter correctly isolates users by role")
    void testGetUsersWithRoleFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + normalAdminToken)
                        .param("role", "PARTNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[*].role", everyItem(is("PARTNER"))));
    }

    @Test
    @DisplayName("Users Filter: Status filter correctly isolates users by lifecycle state")
    void testGetUsersWithStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + normalAdminToken)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[*].status", everyItem(is("ACTIVE"))));
    }

    @Test
    @DisplayName("Summary: Retrieves aggregate user distribution and status metrics")
    void testGetUserSummary() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/summary")
                        .header("Authorization", "Bearer " + normalAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.data.activeUsers", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.data.adminUsers", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.partnerUsers", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.customerUsers", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Detail: Retrieves complete profile with associated customer stats and permissions")
    void testGetCustomerUserDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + customerUser.getId())
                        .header("Authorization", "Bearer " + normalAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(customerUser.getId()))
                .andExpect(jsonPath("$.data.email").value(customerUser.getEmail()))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.customer.vehiclesCount").value(1));
    }

    @Test
    @DisplayName("Detail: Retrieves partner profile with associated workshop summary")
    void testGetPartnerUserDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + partnerUser.getId())
                        .header("Authorization", "Bearer " + normalAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(partnerUser.getId()))
                .andExpect(jsonPath("$.data.role").value("PARTNER"))
                .andExpect(jsonPath("$.data.workshop").exists())
                .andExpect(jsonPath("$.data.workshop.businessName").value("Garage Apex " + runId))
                .andExpect(jsonPath("$.data.workshop.verificationStatus").value("VERIFIED"));
    }

    @Test
    @DisplayName("Status Mutation: Admin can suspend a customer with audit logging")
    void testSuspendCustomerUserWithAudit() throws Exception {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setStatus(UserStatus.SUSPENDED);
        req.setReason("Policy violation reported by operations team");

        mockMvc.perform(patch("/api/v1/admin/users/" + customerUser.getId() + "/status")
                        .header("Authorization", "Bearer " + normalAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.isActive").value(false));

        // Verify database persistence
        User updated = userRepository.findById(customerUser.getId()).orElseThrow();
        assertEquals(UserStatus.SUSPENDED, updated.getStatus());
        assertFalse(updated.getIsActive());

        // Verify Audit Log
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("USER", String.valueOf(customerUser.getId()));
        assertFalse(logs.isEmpty());
        AuditLog latest = logs.get(0);
        assertEquals("USER_STATUS_CHANGED", latest.getAction());
        assertEquals("SUCCESS", latest.getStatus());
        assertTrue(latest.getDescription().contains("SUSPENDED"));
    }

    @Test
    @DisplayName("Status Mutation: Admin can reactivate a suspended user")
    void testReactivateSuspendedUser() throws Exception {
        // First suspend
        customerUser.setStatus(UserStatus.SUSPENDED);
        userRepository.save(customerUser);

        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setStatus(UserStatus.ACTIVE);
        req.setReason("Customer dispute resolved successfully");

        mockMvc.perform(patch("/api/v1/admin/users/" + customerUser.getId() + "/status")
                        .header("Authorization", "Bearer " + normalAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.isActive").value(true));

        User updated = userRepository.findById(customerUser.getId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, updated.getStatus());
        assertTrue(updated.getIsActive());
    }

    @Test
    @DisplayName("Safeguard 1: Normal ADMIN cannot modify a SUPER_ADMIN account status (returns 403)")
    void testNormalAdminCannotModifySuperAdmin() throws Exception {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setStatus(UserStatus.SUSPENDED);
        req.setReason("Unauthorized suspension attempt");

        mockMvc.perform(patch("/api/v1/admin/users/" + superAdminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + normalAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Safeguard 2: Admin cannot deactivate or suspend their own active session")
    void testAdminCannotDeactivateSelf() throws Exception {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setStatus(UserStatus.INACTIVE);
        req.setReason("Accidental self deactivation");

        mockMvc.perform(patch("/api/v1/admin/users/" + normalAdminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + normalAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Role Mutation: Super Admin can promote a user role with audit logging")
    void testSuperAdminCanUpdateRole() throws Exception {
        UpdateUserRoleRequest req = new UpdateUserRoleRequest();
        req.setRole(UserRole.SUPPORT_AGENT);
        req.setReason("Promoted to support agent tier");

        mockMvc.perform(patch("/api/v1/admin/users/" + customerUser.getId() + "/role")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("SUPPORT_AGENT"));

        User updated = userRepository.findById(customerUser.getId()).orElseThrow();
        assertEquals(UserRole.SUPPORT_AGENT, updated.getRole());

        // Verify Audit Log
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("USER", String.valueOf(customerUser.getId()));
        assertFalse(logs.isEmpty());
        AuditLog latest = logs.get(0);
        assertEquals("USER_ROLE_CHANGED", latest.getAction());
        assertTrue(latest.getDescription().contains("SUPPORT_AGENT"));
    }

    @Test
    @DisplayName("Safeguard 3: Normal Admin cannot modify user roles (returns 403)")
    void testNormalAdminCannotUpdateRole() throws Exception {
        UpdateUserRoleRequest req = new UpdateUserRoleRequest();
        req.setRole(UserRole.ADMIN);
        req.setReason("Privilege escalation attempt");

        mockMvc.perform(patch("/api/v1/admin/users/" + customerUser.getId() + "/role")
                        .header("Authorization", "Bearer " + normalAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Safeguard 4: Super Admin cannot revoke own SUPER_ADMIN role")
    void testSuperAdminCannotRevokeOwnRole() throws Exception {
        UpdateUserRoleRequest req = new UpdateUserRoleRequest();
        req.setRole(UserRole.ADMIN);
        req.setReason("Self downgrade attempt");

        mockMvc.perform(patch("/api/v1/admin/users/" + superAdminUser.getId() + "/role")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Roles Catalog: Admin can view system roles and permissions catalog")
    void testGetRolesCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles")
                        .header("Authorization", "Bearer " + normalAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(UserRole.values().length)))
                .andExpect(jsonPath("$.data[?(@.role == 'SUPER_ADMIN')].isPrivileged").value(hasItem(true)))
                .andExpect(jsonPath("$.data[?(@.role == 'CUSTOMER')].isPrivileged").value(hasItem(false)))
                .andExpect(jsonPath("$.data[0].permissions").isArray());
    }
}

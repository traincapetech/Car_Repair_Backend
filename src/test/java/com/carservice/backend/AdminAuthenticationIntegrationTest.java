package com.carservice.backend;

import com.carservice.backend.security.jwt.JwtService;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
public class AdminAuthenticationIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User customerUser;
    private User partnerUser;

    private String adminToken;
    private String customerToken;
    private String partnerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String runId = UUID.randomUUID().toString().substring(0, 8);

        // Provision test admin
        adminUser = new User();
        adminUser.setName("Admin Tester " + runId);
        adminUser.setEmail("admin_" + runId + "@test.com");
        adminUser.setPhone("9" + (int) (Math.random() * 900000000 + 100000000));
        adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);

        // Provision test customer
        customerUser = new User();
        customerUser.setName("Customer Tester " + runId);
        customerUser.setEmail("customer_" + runId + "@test.com");
        customerUser.setPhone("8" + (int) (Math.random() * 900000000 + 100000000));
        customerUser.setPassword(passwordEncoder.encode("CustomerPass123!"));
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setIsActive(true);
        customerUser = userRepository.save(customerUser);
        customerToken = jwtService.generateAccessToken(customerUser);

        // Provision test partner
        partnerUser = new User();
        partnerUser.setName("Partner Tester " + runId);
        partnerUser.setEmail("partner_" + runId + "@test.com");
        partnerUser.setPhone("7" + (int) (Math.random() * 900000000 + 100000000));
        partnerUser.setPassword(passwordEncoder.encode("PartnerPass123!"));
        partnerUser.setRole(UserRole.PARTNER);
        partnerUser.setIsActive(true);
        partnerUser = userRepository.save(partnerUser);
        partnerToken = jwtService.generateAccessToken(partnerUser);
    }

    @Test
    @DisplayName("1. ADMIN can successfully access GET /api/v1/admin/me")
    void test1_adminCanAccessAdminMeEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Admin session profile retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.email").value(adminUser.getEmail()))
                .andExpect(jsonPath("$.data.name").value(adminUser.getName()))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("2. CUSTOMER is strictly rejected from GET /api/v1/admin/me with 403 Forbidden")
    void test2_customerCannotAccessAdminMeEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("3. PARTNER is strictly rejected from GET /api/v1/admin/me with 403 Forbidden")
    void test3_partnerCannotAccessAdminMeEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("4. Unauthenticated request to /api/v1/admin/me returns 401 Unauthorized")
    void test4_unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("5. Response does not leak password, password hash, or credentials")
    void test5_adminResponseDoesNotExposePasswordOrHash() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.hash").doesNotExist())
                .andExpect(jsonPath("$.data.credentials").doesNotExist());
    }

    @Test
    @DisplayName("6. Response does not leak JWT, refresh token, or internal secrets")
    void test6_adminResponseDoesNotExposeTokensOrSecrets() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.secret").doesNotExist())
                .andExpect(jsonPath("$.data.jwt").doesNotExist());
    }

    @Test
    @DisplayName("7. Admin health endpoint works for ADMIN and rejects CUSTOMER/PARTNER")
    void test7_adminHealthEndpointRBAC() throws Exception {
        // Admin access succeeds
        mockMvc.perform(get("/api/v1/admin/health")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.subsystem").value("Admin Governance & RBAC"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        // Customer access rejected
        mockMvc.perform(get("/api/v1/admin/health")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Partner access rejected
        mockMvc.perform(get("/api/v1/admin/health")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isForbidden());

        // Unauthenticated access rejected
        mockMvc.perform(get("/api/v1/admin/health"))
                .andExpect(status().isUnauthorized());
    }
}

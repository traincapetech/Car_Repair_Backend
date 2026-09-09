package com.carservice.backend;

import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class ServiceCatalogIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customerUser;
    private User adminUser;
    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        customerUser = new User();
        customerUser.setName("Customer User");
        customerUser.setEmail("cust." + suffix + "@test.com");
        customerUser.setPhone("9" + (System.currentTimeMillis() % 1000000000L));
        customerUser.setPassword(passwordEncoder.encode("Password@123"));
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setIsActive(true);
        customerUser = userRepository.save(customerUser);
        customerToken = jwtService.generateAccessToken(customerUser);

        adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setEmail("admin." + suffix + "@test.com");
        adminUser.setPhone("8" + ((System.currentTimeMillis() + 1) % 1000000000L));
        adminUser.setPassword(passwordEncoder.encode("AdminPassword@123"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);
    }

    @Test
    @DisplayName("1. Unauthenticated GET /api/v1/services is rejected (401 Unauthorized)")
    void testGetServices_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("2. Authenticated customer GET active services (200 OK)")
    void testGetActiveServices_Customer() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog activeService = new ServiceCatalog(
                "Active General Service " + suffix,
                "Comprehensive general inspection",
                ServiceCategory.GENERAL_SERVICE,
                new BigDecimal("1299.00"),
                90,
                true
        );
        serviceCatalogRepository.save(activeService);

        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", isA(java.util.List.class)))
                .andExpect(jsonPath("$.data[*].name", hasItem("Active General Service " + suffix)));
    }

    @Test
    @DisplayName("3. Customer cannot create service (403 Forbidden)")
    void testCreateService_CustomerForbidden() throws Exception {
        String jsonPayload = """
                {
                    "name": "Unauthorized Service",
                    "description": "Customer trying to create",
                    "category": "ENGINE_SERVICE",
                    "basePrice": 4999.00,
                    "estimatedDurationMinutes": 180
                }
                """;

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("4. Customer cannot update service (403 Forbidden)")
    void testUpdateService_CustomerForbidden() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Test Service " + suffix,
                "Initial description",
                ServiceCategory.BRAKE_SERVICE,
                new BigDecimal("899.00"),
                60,
                true
        );
        service = serviceCatalogRepository.save(service);

        String updatePayload = """
                {
                    "name": "Hacked Brake Service",
                    "description": "Customer modified",
                    "category": "BRAKE_SERVICE",
                    "basePrice": 199.00,
                    "estimatedDurationMinutes": 30
                }
                """;

        mockMvc.perform(put("/api/v1/services/" + service.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("5. Customer cannot activate/deactivate service (403 Forbidden)")
    void testToggleActivation_CustomerForbidden() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Toggle Service " + suffix,
                "Toggle test description",
                ServiceCategory.BATTERY_SERVICE,
                new BigDecimal("499.00"),
                30,
                true
        );
        service = serviceCatalogRepository.save(service);

        mockMvc.perform(patch("/api/v1/services/" + service.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));

        mockMvc.perform(patch("/api/v1/services/" + service.getId() + "/activate")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("6. Admin can create service (201 Created)")
    void testCreateService_AdminSuccess() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String serviceName = "Periodic Maintenance " + suffix;

        String jsonPayload = String.format("""
                {
                    "name": "%s",
                    "description": "Standard 10,000km periodic maintenance service",
                    "category": "PERIODIC_SERVICE",
                    "basePrice": 2499.50,
                    "estimatedDurationMinutes": 150,
                    "isActive": true
                }
                """, serviceName);

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Service created successfully"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value(serviceName))
                .andExpect(jsonPath("$.data.category").value("PERIODIC_SERVICE"))
                .andExpect(jsonPath("$.data.basePrice").value(2499.50))
                .andExpect(jsonPath("$.data.estimatedDurationMinutes").value(150))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("7. Admin can update service (200 OK)")
    void testUpdateService_AdminSuccess() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Original AC Service " + suffix,
                "Basic AC check",
                ServiceCategory.AC_SERVICE,
                new BigDecimal("999.00"),
                60,
                true
        );
        service = serviceCatalogRepository.save(service);

        String updatePayload = String.format("""
                {
                    "name": "Advanced AC Service %s",
                    "description": "Complete gas refill and coil cleanup",
                    "category": "AC_SERVICE",
                    "basePrice": 1799.00,
                    "estimatedDurationMinutes": 90,
                    "isActive": true
                }
                """, suffix);

        mockMvc.perform(put("/api/v1/services/" + service.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Service updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Advanced AC Service " + suffix))
                .andExpect(jsonPath("$.data.basePrice").value(1799.00))
                .andExpect(jsonPath("$.data.estimatedDurationMinutes").value(90));
    }

    @Test
    @DisplayName("8. Admin can deactivate and activate service")
    void testDeactivateAndActivateService_AdminSuccess() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog service = new ServiceCatalog(
                "Wheel Alignment " + suffix,
                "Laser 3D wheel alignment",
                ServiceCategory.WHEEL_ALIGNMENT,
                new BigDecimal("699.00"),
                45,
                true
        );
        service = serviceCatalogRepository.save(service);

        // Deactivate
        mockMvc.perform(patch("/api/v1/services/" + service.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Service deactivated successfully"))
                .andExpect(jsonPath("$.data.isActive").value(false));

        // Verify DB
        ServiceCatalog deactivated = serviceCatalogRepository.findById(service.getId()).orElseThrow();
        assertFalse(deactivated.getIsActive());

        // Activate
        mockMvc.perform(patch("/api/v1/services/" + service.getId() + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Service activated successfully"))
                .andExpect(jsonPath("$.data.isActive").value(true));

        // Verify DB
        ServiceCatalog activated = serviceCatalogRepository.findById(service.getId()).orElseThrow();
        assertTrue(activated.getIsActive());
    }

    @Test
    @DisplayName("9. Customer cannot see inactive service in listing or single fetch (404 Not Found)")
    void testCustomerCannotSeeInactiveService() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog inactiveService = new ServiceCatalog(
                "Inactive Detailing " + suffix,
                "Ceramic coating",
                ServiceCategory.DETAILING,
                new BigDecimal("9999.00"),
                360,
                false
        );
        inactiveService = serviceCatalogRepository.save(inactiveService);

        // Customer listing must NOT contain inactive service
        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", not(hasItem("Inactive Detailing " + suffix))));

        // Customer single fetch must return 404
        mockMvc.perform(get("/api/v1/services/" + inactiveService.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    @DisplayName("10. Admin can see inactive service in listing and single fetch (200 OK)")
    void testAdminCanSeeInactiveService() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        ServiceCatalog inactiveService = new ServiceCatalog(
                "Admin Viewable Inactive " + suffix,
                "Hidden from customer",
                ServiceCategory.DIAGNOSTICS,
                new BigDecimal("1499.00"),
                60,
                false
        );
        inactiveService = serviceCatalogRepository.save(inactiveService);

        // Admin listing should include inactive service
        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", hasItem("Admin Viewable Inactive " + suffix)));

        // Admin single fetch returns 200 OK
        mockMvc.perform(get("/api/v1/services/" + inactiveService.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(inactiveService.getId()))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    @DisplayName("11. Duplicate service name is rejected (409 Conflict)")
    void testDuplicateServiceName_Rejected() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String serviceName = "Brake Pad Replacement " + suffix;

        ServiceCatalog existing = new ServiceCatalog(
                serviceName,
                "Front brake pads",
                ServiceCategory.BRAKE_SERVICE,
                new BigDecimal("1199.00"),
                45,
                true
        );
        serviceCatalogRepository.save(existing);

        // Attempt creation with same name (case-insensitive)
        String jsonPayload = String.format("""
                {
                    "name": "%s",
                    "description": "Duplicate attempt",
                    "category": "BRAKE_SERVICE",
                    "basePrice": 1299.00,
                    "estimatedDurationMinutes": 50
                }
                """, serviceName.toLowerCase());

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("12. Invalid price (negative or 0) rejected (400 Bad Request)")
    void testInvalidPrice_Rejected() throws Exception {
        String jsonPayload = """
                {
                    "name": "Zero Price Service",
                    "description": "Testing invalid zero price",
                    "category": "GENERAL_SERVICE",
                    "basePrice": 0.00,
                    "estimatedDurationMinutes": 60
                }
                """;

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.basePrice").isNotEmpty());
    }

    @Test
    @DisplayName("13. Invalid duration (zero or negative) rejected (400 Bad Request)")
    void testInvalidDuration_Rejected() throws Exception {
        String jsonPayload = """
                {
                    "name": "Zero Duration Service",
                    "description": "Testing invalid duration",
                    "category": "GENERAL_SERVICE",
                    "basePrice": 999.00,
                    "estimatedDurationMinutes": 0
                }
                """;

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.estimatedDurationMinutes").isNotEmpty());
    }

    @Test
    @DisplayName("14. Invalid category enum rejected (400 Bad Request)")
    void testInvalidCategory_Rejected() throws Exception {
        String jsonPayload = """
                {
                    "name": "Invalid Category Service",
                    "description": "Testing invalid category",
                    "category": "NOT_A_REAL_CATEGORY",
                    "basePrice": 999.00,
                    "estimatedDurationMinutes": 60
                }
                """;

        mockMvc.perform(post("/api/v1/services")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("15. Nonexistent service returns 404 Not Found")
    void testNonexistentService_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/services/9999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }
}

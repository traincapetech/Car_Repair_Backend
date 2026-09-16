package com.carservice.backend;

import com.carservice.backend.admin.dto.UpdateCustomerStatusRequest;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.enums.DiscountType;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import com.carservice.backend.user.dto.LoginRequest;
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
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
public class AdminCustomerManagementIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User customerUserA;
    private User customerUserB;
    private User partnerUser;

    private String adminToken;
    private String customerTokenA;
    private String partnerToken;

    private Vehicle vehicleA;
    private Vehicle vehicleB;
    private Booking bookingA;
    private ServiceRequest serviceRequestA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Admin
        adminUser = new User();
        adminUser.setName("Admin Tester " + runId);
        adminUser.setEmail("adm_" + runId + "@carservice.com");
        adminUser.setPhone("91" + runId.hashCode() % 100000000);
        if (adminUser.getPhone().length() < 10) adminUser.setPhone("9188" + runId.substring(0, 6));
        adminUser.setPassword(passwordEncoder.encode("Admin@12345"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);

        // 2. Create Customer A
        customerUserA = new User();
        customerUserA.setName("Customer Alpha " + runId);
        customerUserA.setEmail("cust_a_" + runId + "@carservice.com");
        customerUserA.setPhone("92" + Math.abs(runId.hashCode() % 100000000));
        if (customerUserA.getPhone().length() < 10) customerUserA.setPhone("9288" + runId.substring(0, 6));
        customerUserA.setPassword(passwordEncoder.encode("Cust@12345"));
        customerUserA.setRole(UserRole.CUSTOMER);
        customerUserA.setIsActive(true);
        customerUserA = userRepository.save(customerUserA);
        customerTokenA = jwtService.generateAccessToken(customerUserA);

        // 3. Create Customer B
        customerUserB = new User();
        customerUserB.setName("Customer Beta " + runId);
        customerUserB.setEmail("cust_b_" + runId + "@carservice.com");
        customerUserB.setPhone("93" + Math.abs(runId.hashCode() % 100000000));
        if (customerUserB.getPhone().length() < 10) customerUserB.setPhone("9388" + runId.substring(0, 6));
        customerUserB.setPassword(passwordEncoder.encode("Cust@12345"));
        customerUserB.setRole(UserRole.CUSTOMER);
        customerUserB.setIsActive(true);
        customerUserB = userRepository.save(customerUserB);

        // 4. Create Partner
        partnerUser = new User();
        partnerUser.setName("Partner Tester " + runId);
        partnerUser.setEmail("part_" + runId + "@carservice.com");
        partnerUser.setPhone("94" + Math.abs(runId.hashCode() % 100000000));
        if (partnerUser.getPhone().length() < 10) partnerUser.setPhone("9488" + runId.substring(0, 6));
        partnerUser.setPassword(passwordEncoder.encode("Part@12345"));
        partnerUser.setRole(UserRole.PARTNER);
        partnerUser.setIsActive(true);
        partnerUser = userRepository.save(partnerUser);
        partnerToken = jwtService.generateAccessToken(partnerUser);

        // 5. Create Vehicle for Customer A
        vehicleA = new Vehicle();
        vehicleA.setUser(customerUserA);
        vehicleA.setMake("Hyundai");
        vehicleA.setModel("Creta");
        vehicleA.setYear(2023);
        vehicleA.setRegistrationNumber("DL" + runId.substring(0, 2).toUpperCase() + "AB" + (int)(Math.random() * 8999 + 1000));
        vehicleA.setFuelType(FuelType.PETROL);
        vehicleA.setTransmission(Transmission.AUTOMATIC);
        vehicleA = vehicleRepository.save(vehicleA);

        // 6. Create Vehicle for Customer B
        vehicleB = new Vehicle();
        vehicleB.setUser(customerUserB);
        vehicleB.setMake("Tata");
        vehicleB.setModel("Nexon");
        vehicleB.setYear(2022);
        vehicleB.setRegistrationNumber("MH" + runId.substring(0, 2).toUpperCase() + "CD" + (int)(Math.random() * 8999 + 1000));
        vehicleB.setFuelType(FuelType.DIESEL);
        vehicleB.setTransmission(Transmission.MANUAL);
        vehicleB = vehicleRepository.save(vehicleB);

        // 7. Create Booking for Customer A
        com.carservice.backend.servicecatalog.entity.ServiceCatalog testService = new com.carservice.backend.servicecatalog.entity.ServiceCatalog(
                "Periodic Service " + runId,
                "Periodic car maintenance",
                com.carservice.backend.servicecatalog.enums.ServiceCategory.GENERAL_SERVICE,
                new BigDecimal("2499.00"),
                120,
                true
        );
        testService = serviceCatalogRepository.save(testService);

        bookingA = new Booking();
        bookingA.setUser(customerUserA);
        bookingA.setVehicle(vehicleA);
        bookingA.setService(testService);
        bookingA.setBookingReference("BK-TEST-" + runId);
        bookingA.setBookingDate(LocalDate.now().plusDays(2));
        bookingA.setBookingTime(LocalTime.of(10, 0));
        bookingA.setTimeSlot("10:00 AM - 12:00 PM");
        bookingA.setStatus(BookingStatus.CONFIRMED);
        bookingA.setServiceNameSnapshot("Periodic Maintenance Service");
        bookingA.setServicePriceSnapshot(BigDecimal.valueOf(2499.00));
        bookingA.setEstimatedPrice(BigDecimal.valueOf(2499.00));
        bookingA.setTotalAmount(BigDecimal.valueOf(2499.00));
        bookingA.setCity("Mumbai");
        bookingA.setAddress("45 Sea Breeze Apt");
        bookingA.setPincode("400001");
        bookingA = bookingRepository.save(bookingA);

        // 8. Create ServiceRequest for Customer A
        serviceRequestA = new ServiceRequest();
        serviceRequestA.setUser(customerUserA);
        serviceRequestA.setVehicle(vehicleA);
        serviceRequestA.setRequestReference("SR-TEST-" + runId);
        serviceRequestA.setCity("Mumbai");
        serviceRequestA.setAddress("45 Sea Breeze Apt");
        serviceRequestA.setPincode("400001");
        serviceRequestA.setPreferredDate(LocalDate.now().plusDays(2));
        serviceRequestA.setPreferredTimeSlot("10:00 AM - 12:00 PM");
        serviceRequestA.setStatus(ServiceRequestStatus.SUBMITTED);
        serviceRequestA.setTotalAmount(BigDecimal.valueOf(2499.00));
        serviceRequestA = serviceRequestRepository.save(serviceRequestA);
    }

    @Test
    @DisplayName("Scenario 1: Admin can list customers with pagination")
    void testAdminCanListCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Scenario 2: Customer cannot access admin customer endpoint")
    void testCustomerCannotAccessAdminCustomerEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + customerTokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 3: Partner cannot access admin customer endpoint")
    void testPartnerCannotAccessAdminCustomerEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 4: Unauthenticated request returns 401")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 5: Admin can retrieve detailed customer profile")
    void testAdminCanRetrieveCustomerDetails() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers/" + customerUserA.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(customerUserA.getId()))
                .andExpect(jsonPath("$.data.name").value(customerUserA.getName()))
                .andExpect(jsonPath("$.data.email").value(customerUserA.getEmail()))
                .andExpect(jsonPath("$.data.totalVehicles").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalBookings").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.confirmedBookings").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalServiceRequests").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Scenario 6: Unknown customer returns 404")
    void testUnknownCustomerReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers/99999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Scenario 7: Admin can view customer registered vehicles")
    void testAdminCanViewCustomerVehicles() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers/" + customerUserA.getId() + "/vehicles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].registrationNumber").value(vehicleA.getRegistrationNumber()))
                .andExpect(jsonPath("$.data[0].make").value("Hyundai"))
                .andExpect(jsonPath("$.data[0].model").value("Creta"));
    }

    @Test
    @DisplayName("Scenario 8: Admin can view customer bookings")
    void testAdminCanViewCustomerBookings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers/" + customerUserA.getId() + "/bookings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].bookingReference").value(bookingA.getBookingReference()))
                .andExpect(jsonPath("$.data.content[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Scenario 9: Admin can view customer service request history")
    void testAdminCanViewCustomerServiceRequests() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers/" + customerUserA.getId() + "/service-requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].requestReference").value(serviceRequestA.getRequestReference()));
    }

    @Test
    @DisplayName("Scenario 10: Admin can deactivate customer")
    void testAdminCanDeactivateCustomer() throws Exception {
        UpdateCustomerStatusRequest req = new UpdateCustomerStatusRequest(false);

        mockMvc.perform(put("/api/v1/admin/customers/" + customerUserA.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));

        User updated = userRepository.findById(customerUserA.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
    }

    @Test
    @DisplayName("Scenario 11: Admin can reactivate deactivated customer")
    void testAdminCanReactivateCustomer() throws Exception {
        // First deactivate
        customerUserA.setIsActive(false);
        userRepository.save(customerUserA);

        UpdateCustomerStatusRequest req = new UpdateCustomerStatusRequest(true);

        mockMvc.perform(put("/api/v1/admin/customers/" + customerUserA.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));

        User updated = userRepository.findById(customerUserA.getId()).orElseThrow();
        assertTrue(updated.getIsActive());
    }

    @Test
    @DisplayName("Scenario 12: Deactivated customer cannot authenticate or access protected resources")
    void testDeactivatedCustomerCannotAuthenticateOrAccess() throws Exception {
        // Deactivate Customer A
        customerUserA.setIsActive(false);
        userRepository.save(customerUserA);

        // 1. Existing token rejected by JwtAuthenticationFilter
        mockMvc.perform(get("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + customerTokenA))
                .andExpect(status().isUnauthorized());

        // 2. Login attempt rejected
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(customerUserA.getEmail());
        loginReq.setPassword("Cust@12345");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("inactive")));
    }

    @Test
    @DisplayName("Scenario 13: Admin cannot deactivate their own administrative account")
    void testAdminCannotDeactivateThemselves() throws Exception {
        UpdateCustomerStatusRequest req = new UpdateCustomerStatusRequest(false);

        mockMvc.perform(put("/api/v1/admin/customers/" + adminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Administrators cannot deactivate their own account")));

        User adminInDb = userRepository.findById(adminUser.getId()).orElseThrow();
        assertTrue(adminInDb.getIsActive());
    }

    @Test
    @DisplayName("Scenario 14: Customer management does not operate on ADMIN or PARTNER roles")
    void testCannotOperateOnAdminOrPartnerRoles() throws Exception {
        // Calling customer detail on partner account must return 404 without leaking partner data
        mockMvc.perform(get("/api/v1/admin/customers/" + partnerUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        // Attempting status change on partner must also fail
        UpdateCustomerStatusRequest req = new UpdateCustomerStatusRequest(false);
        mockMvc.perform(put("/api/v1/admin/customers/" + partnerUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 15: Admin cannot modify password through customer management")
    void testAdminCannotModifyPasswordThroughCustomerManagement() throws Exception {
        String originalPasswordHash = customerUserA.getPassword();

        // Pass extra malicious JSON fields attempting to override password and role
        String payload = "{\"isActive\":false,\"password\":\"NewHackedPassword123\",\"role\":\"ADMIN\"}";

        mockMvc.perform(put("/api/v1/admin/customers/" + customerUserA.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        User inDb = userRepository.findById(customerUserA.getId()).orElseThrow();
        assertEquals(originalPasswordHash, inDb.getPassword());
        assertEquals(UserRole.CUSTOMER, inDb.getRole());
    }

    @Test
    @DisplayName("Scenario 16: Sensitive authentication fields are never returned")
    void testSensitiveFieldsNeverReturned() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers/" + customerUserA.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.jwtSecret").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Scenario 17: Database pagination works correctly")
    void testPaginationWorksCorrectly() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Scenario 18: Search works correctly across name, email, and phone")
    void testSearchWorksCorrectly() throws Exception {
        // Search by unique email
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", customerUserA.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(customerUserA.getId()));

        // Search by unique phone
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", customerUserB.getPhone()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(customerUserB.getId()));
    }

    @Test
    @DisplayName("Scenario 19: Active and inactive status filtering works")
    void testStatusFilterWorks() throws Exception {
        // Set customer B to inactive
        customerUserB.setIsActive(false);
        userRepository.save(customerUserB);

        // Filter ACTIVE
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "ACTIVE")
                        .param("search", customerUserB.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", empty()));

        // Filter INACTIVE
        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "INACTIVE")
                        .param("search", customerUserB.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(customerUserB.getId()));
    }

    @Test
    @DisplayName("Scenario 20: Customer data is strictly scoped and not mixed between accounts")
    void testCustomerDataNotMixed() throws Exception {
        // Customer A has vehicleA, Customer B has vehicleB
        mockMvc.perform(get("/api/v1/admin/customers/" + customerUserA.getId() + "/vehicles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].registrationNumber", hasItem(vehicleA.getRegistrationNumber())))
                .andExpect(jsonPath("$.data[*].registrationNumber", not(hasItem(vehicleB.getRegistrationNumber()))));

        mockMvc.perform(get("/api/v1/admin/customers/" + customerUserB.getId() + "/vehicles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].registrationNumber", hasItem(vehicleB.getRegistrationNumber())))
                .andExpect(jsonPath("$.data[*].registrationNumber", not(hasItem(vehicleA.getRegistrationNumber()))));
    }
}

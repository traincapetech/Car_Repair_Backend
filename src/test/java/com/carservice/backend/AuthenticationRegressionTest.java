package com.carservice.backend;

import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.security.repository.RefreshTokenRepository;
import com.carservice.backend.security.service.RefreshTokenService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.jayway.jsonpath.JsonPath;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class AuthenticationRegressionTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("1. Customer Registration -> HTTP 201 Created and valid response contract")
    void testCustomerRegistration_Success() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "reg.test." + uniqueSuffix + "@example.com";
        String phone = "9" + String.format("%09d", Math.abs(System.currentTimeMillis() % 1000000000L));

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Regression User\",\"email\":\"" + email + "\",\"phone\":\"" + phone + "\",\"password\":\"TestPassword123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer registered successfully"))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("2. Duplicate Registration -> HTTP 409 Conflict")
    void testCustomerRegistration_DuplicateEmail() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "dup.test." + uniqueSuffix + "@example.com";
        String phone1 = "9" + String.format("%09d", Math.abs(System.currentTimeMillis() % 1000000000L));

        // First registration
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"First User\",\"email\":\"" + email + "\",\"phone\":\"" + phone1 + "\",\"password\":\"TestPassword123\"}"))
                .andExpect(status().isCreated());

        // Duplicate registration with same email
        String phone2 = "8" + String.format("%09d", Math.abs(System.currentTimeMillis() % 1000000000L));
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Duplicate User\",\"email\":\"" + email + "\",\"phone\":\"" + phone2 + "\",\"password\":\"TestPassword123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    @DisplayName("3. Login Success -> HTTP 200 with tokens and no password")
    void testLogin_Success() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "login.test." + uniqueSuffix + "@example.com";
        String phone = "9" + String.format("%09d", Math.abs(System.currentTimeMillis() % 1000000000L));

        User user = new User();
        user.setName("Login Test User");
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("TestPassword123"));
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"TestPassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    @DisplayName("4. Login Failure -> HTTP 401 Invalid email or password")
    void testLogin_InvalidPassword() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "invalid.login." + uniqueSuffix + "@example.com";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"WrongPassword123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("5. Profile Update and Persistence -> HTTP 200")
    void testProfileUpdate() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "profile.test." + uniqueSuffix + "@example.com";
        String phone = "9" + String.format("%09d", Math.abs(System.currentTimeMillis() % 1000000000L));

        User user = new User();
        user.setName("Original Name");
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("TestPassword123"));
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(true);
        user = userRepository.save(user);

        String token = jwtService.generateAccessToken(user);
        String updatedPhone = "7" + String.format("%09d", Math.abs(System.currentTimeMillis() % 1000000000L));

        mockMvc.perform(put("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Name\",\"phone\":\"" + updatedPhone + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.phone").value(updatedPhone));

        // Persisted verification
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.phone").value(updatedPhone));
    }

    @Test
    @DisplayName("6. Refresh Token Rotation and Old Token Invalidation -> HTTP 200 then HTTP 401")
    void testRefreshTokenRotation() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "rotate.test." + uniqueSuffix + "@example.com";
        String phone = "9" + (System.currentTimeMillis() % 1000000000L);

        User user = new User();
        user.setName("Rotation User");
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("TestPassword123"));
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(true);
        user = userRepository.save(user);

        String initialRefreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.saveRefreshToken(initialRefreshToken, user, jwtService.getRefreshTokenExpiryDate());

        // Perform rotation
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + initialRefreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        String newRefreshToken = JsonPath.read(result.getResponse().getContentAsString(), "$.data.refreshToken");
        assertNotEquals(initialRefreshToken, newRefreshToken);

        // Attempt reuse of rotated token -> must fail with 401
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + initialRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    @Test
    @DisplayName("7. Role Authorization -> Customer denied Admin access with HTTP 403")
    void testRoleAuthorization_CustomerDeniedAdmin() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "authz.test." + uniqueSuffix + "@example.com";
        String phone = "9" + (System.currentTimeMillis() % 1000000000L);

        User user = new User();
        user.setName("Customer Only");
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("TestPassword123"));
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(true);
        user = userRepository.save(user);

        String token = jwtService.generateAccessToken(user);

        mockMvc.perform(get("/api/v1/test/customer")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("8. Account Deactivation -> Immediate rejection on login")
    void testAccountDeactivation_BlocksLogin() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "deact.test." + uniqueSuffix + "@example.com";
        String phone = "9" + (System.currentTimeMillis() % 1000000000L);

        User user = new User();
        user.setName("Deact User");
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("TestPassword123"));
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(true);
        user = userRepository.save(user);

        String token = jwtService.generateAccessToken(user);

        // Deactivate account
        mockMvc.perform(put("/api/v1/auth/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"TestPassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Subsequent login attempt -> 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"TestPassword123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User account is inactive"));
    }
}

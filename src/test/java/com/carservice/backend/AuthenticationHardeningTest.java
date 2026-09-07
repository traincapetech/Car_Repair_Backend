package com.carservice.backend;

import com.carservice.backend.security.entity.RefreshToken;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.security.repository.PasswordResetTokenRepository;
import com.carservice.backend.security.repository.RefreshTokenRepository;
import com.carservice.backend.security.service.PasswordResetTokenService;
import com.carservice.backend.security.service.RefreshTokenService;
import com.carservice.backend.security.util.TokenHashUtil;
import com.carservice.backend.user.dto.ChangePasswordRequest;
import com.carservice.backend.user.dto.CustomerRegistrationRequest;
import com.carservice.backend.user.dto.DeactivateAccountRequest;
import com.carservice.backend.user.dto.LoginRequest;
import com.carservice.backend.user.dto.LogoutRequest;
import com.carservice.backend.user.dto.RefreshTokenRequest;
import com.carservice.backend.user.dto.ResetPasswordRequest;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import com.carservice.backend.user.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AuthenticationHardeningTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordResetTokenService passwordResetTokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private User activeCustomer;
    private User adminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create or load active test customer
        String customerEmail = "test.customer." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        activeCustomer = new User();
        activeCustomer.setName("Active Customer");
        activeCustomer.setEmail(customerEmail);
        activeCustomer.setPhone("9" + (System.currentTimeMillis() % 1000000000L));
        activeCustomer.setPassword(passwordEncoder.encode("Password@123"));
        activeCustomer.setRole(UserRole.CUSTOMER);
        activeCustomer.setIsActive(true);
        activeCustomer = userRepository.save(activeCustomer);

        // Create or load test admin
        String adminEmail = "test.admin." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        adminUser = new User();
        adminUser.setName("Test Admin");
        adminUser.setEmail(adminEmail);
        adminUser.setPhone("8" + (System.currentTimeMillis() % 1000000000L));
        adminUser.setPassword(passwordEncoder.encode("AdminPassword@123"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
    }

    @Test
    @DisplayName("A. Valid access token -> 200")
    void testA_ValidAccessToken() throws Exception {
        String token = jwtService.generateAccessToken(activeCustomer);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(activeCustomer.getEmail()));
    }

    @Test
    @DisplayName("B. Missing token -> 401")
    void testB_MissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("C. Malformed token -> 401")
    void testC_MalformedToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.malformed.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("D. Expired token -> 401")
    void testD_ExpiredToken() throws Exception {
        // Generate an already expired access token
        String expiredToken = Jwts.builder()
                .subject(activeCustomer.getEmail())
                .claim("userId", activeCustomer.getId())
                .claim("role", activeCustomer.getRole().name())
                .claim(JwtService.CLAIM_TOKEN_TYPE, JwtService.TOKEN_TYPE_ACCESS)
                .issuedAt(new Date(System.currentTimeMillis() - 100000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("E. Invalid signature -> 401")
    void testE_InvalidSignature() throws Exception {
        // Generate token signed with an untrusted secret
        String differentSecret = "AnotherSecretKeyMustBeAtLeast32BytesLongForHmacSha256!";
        String tamperedToken = Jwts.builder()
                .subject(activeCustomer.getEmail())
                .claim("userId", activeCustomer.getId())
                .claim("role", activeCustomer.getRole().name())
                .claim(JwtService.CLAIM_TOKEN_TYPE, JwtService.TOKEN_TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("F. Access token used as refresh token -> 401")
    void testF_AccessTokenUsedAsRefreshToken() throws Exception {
        String accessToken = jwtService.generateAccessToken(activeCustomer);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + accessToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    @DisplayName("G. Refresh token used as access token -> 401")
    void testG_RefreshTokenUsedAsAccessToken() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(activeCustomer);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("H. Revoked refresh token -> 401")
    void testH_RevokedRefreshToken() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(activeCustomer);
        refreshTokenService.saveRefreshToken(refreshToken, activeCustomer, jwtService.getRefreshTokenExpiryDate());

        // Revoke the token
        RefreshToken storedToken = refreshTokenService.findByToken(refreshToken);
        refreshTokenService.revokeToken(storedToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    @Test
    @DisplayName("I. Expired refresh token -> 401")
    void testI_ExpiredRefreshToken() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(activeCustomer);
        // Save with expiration in past
        refreshTokenService.saveRefreshToken(refreshToken, activeCustomer, LocalDateTime.now().minusMinutes(5));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token has expired"));
    }

    @Test
    @DisplayName("J. Inactive user login -> 401")
    void testJ_InactiveUserLogin() throws Exception {
        activeCustomer.setIsActive(false);
        userRepository.save(activeCustomer);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + activeCustomer.getEmail() + "\",\"password\":\"Password@123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User account is inactive"));
    }

    @Test
    @DisplayName("K. Inactive user refresh -> 401")
    void testK_InactiveUserRefresh() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(activeCustomer);
        refreshTokenService.saveRefreshToken(refreshToken, activeCustomer, jwtService.getRefreshTokenExpiryDate());

        // Deactivate user
        activeCustomer.setIsActive(false);
        userRepository.save(activeCustomer);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User account is inactive"));
    }

    @Test
    @DisplayName("L. CUSTOMER accessing ADMIN endpoint -> 403")
    void testL_CustomerAccessingAdminEndpoint() throws Exception {
        String customerToken = jwtService.generateAccessToken(activeCustomer);

        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("M. ADMIN accessing ADMIN endpoint -> 200")
    void testM_AdminAccessingAdminEndpoint() throws Exception {
        String adminToken = jwtService.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("N. Password change invalidates existing sessions/tokens")
    void testN_PasswordChangeInvalidatesExistingTokens() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(activeCustomer);
        refreshTokenService.saveRefreshToken(refreshToken, activeCustomer, jwtService.getRefreshTokenExpiryDate());

        String customerToken = jwtService.generateAccessToken(activeCustomer);

        // Change password
        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password@123\",\"newPassword\":\"NewPassword@456\",\"confirmPassword\":\"NewPassword@456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Attempt to refresh using previous token -> must be rejected (401)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    @Test
    @DisplayName("O. Password reset invalidates existing sessions/tokens")
    void testO_PasswordResetInvalidatesExistingTokens() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(activeCustomer);
        refreshTokenService.saveRefreshToken(refreshToken, activeCustomer, jwtService.getRefreshTokenExpiryDate());

        // Generate reset token
        String resetToken = passwordResetTokenService.createPasswordResetToken(activeCustomer);

        // Reset password
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetToken\":\"" + resetToken + "\",\"newPassword\":\"ResetPassword@789\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Attempt to refresh using previous token -> must be rejected (401)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    @Test
    @DisplayName("P. Deactivated user cannot authenticate")
    void testP_DeactivatedUserCannotAuthenticate() throws Exception {
        String customerToken = jwtService.generateAccessToken(activeCustomer);

        // Deactivate account
        mockMvc.perform(put("/api/v1/auth/deactivate")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Password@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Attempting to access protected endpoint using previous access token -> 401
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("Q. Logout revokes refresh token")
    void testQ_LogoutRevokesRefreshToken() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(activeCustomer);
        refreshTokenService.saveRefreshToken(refreshToken, activeCustomer, jwtService.getRefreshTokenExpiryDate());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        RefreshToken stored = refreshTokenService.findByToken(refreshToken);
        assertTrue(stored.getRevoked());
    }

    @Test
    @DisplayName("R. Logged-out refresh token cannot be reused")
    void testR_LoggedOutRefreshTokenCannotBeReused() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(activeCustomer);
        refreshTokenService.saveRefreshToken(refreshToken, activeCustomer, jwtService.getRefreshTokenExpiryDate());

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        // Attempting to refresh with the logged-out token -> 401
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    @Test
    @DisplayName("User Enumeration Prevention: Forgot password returns 200 generic message for unknown email")
    void testUserEnumerationPrevention_UnknownEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown.nonexistent.user@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If the account exists, a password reset request has been created."));
    }
}

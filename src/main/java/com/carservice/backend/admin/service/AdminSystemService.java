package com.carservice.backend.admin.service;

import com.carservice.backend.admin.dto.AdminSystemHealthResponse;
import com.carservice.backend.admin.dto.AdminSystemSettingsResponse;
import com.carservice.backend.marketplace.entity.PlatformConfig;
import com.carservice.backend.marketplace.repository.PlatformConfigRepository;
import com.carservice.backend.marketplace.repository.WorkshopWalletRepository;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import com.carservice.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminSystemService {

    private static final Logger log = LoggerFactory.getLogger(AdminSystemService.class);

    private final DataSource dataSource;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final PlatformConfigRepository platformConfigRepository;
    private final WorkshopWalletRepository workshopWalletRepository;
    private final UserRepository userRepository;
    private final Environment environment;

    @Value("${jwt.access-token-expiration:900000}")
    private long jwtAccessExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long jwtRefreshExpiration;

    @Value("${password-reset.token-expiration:15}")
    private int passwordResetExpiration;

    @Value("${razorpay.enabled:false}")
    private boolean razorpayEnabled;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.mock-gateway:false}")
    private boolean razorpayMockGateway;

    public AdminSystemService(
            DataSource dataSource,
            ServiceCatalogRepository serviceCatalogRepository,
            PlatformConfigRepository platformConfigRepository,
            WorkshopWalletRepository workshopWalletRepository,
            UserRepository userRepository,
            Environment environment
    ) {
        this.dataSource = dataSource;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.platformConfigRepository = platformConfigRepository;
        this.workshopWalletRepository = workshopWalletRepository;
        this.userRepository = userRepository;
        this.environment = environment;
    }

    @Transactional(readOnly = true)
    public AdminSystemHealthResponse getSystemHealth() {
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        Map<String, Object> components = new LinkedHashMap<>();

        // 1. Database Health & Ping Latency
        boolean dbHealthy = false;
        long dbLatencyMs = -1;
        String dbError = null;
        try {
            long start = System.currentTimeMillis();
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
                dbLatencyMs = System.currentTimeMillis() - start;
                dbHealthy = true;
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            dbError = e.getMessage();
        }

        Map<String, Object> dbComponent = new LinkedHashMap<>();
        dbComponent.put("status", dbHealthy ? "UP" : "DOWN");
        dbComponent.put("engine", "MySQL 8.x");
        dbComponent.put("latencyMs", dbLatencyMs);
        if (dbError != null) {
            dbComponent.put("error", dbError);
        }
        components.put("database", dbComponent);

        // 2. Auth & RBAC Subsystem
        Map<String, Object> authComponent = new LinkedHashMap<>();
        authComponent.put("status", "UP");
        authComponent.put("authMechanism", "JWT (HMAC-SHA256) & BCrypt");
        authComponent.put("rbacEnforced", true);
        authComponent.put("totalUsers", userRepository.count());
        components.put("security", authComponent);

        // 3. Service Catalog Subsystem
        Map<String, Object> catalogComponent = new LinkedHashMap<>();
        long totalServices = serviceCatalogRepository.count();
        long activeServices = serviceCatalogRepository.countByIsActiveTrue();
        catalogComponent.put("status", "UP");
        catalogComponent.put("totalServices", totalServices);
        catalogComponent.put("activeServices", activeServices);
        components.put("serviceCatalog", catalogComponent);

        // 4. Marketplace Engine Subsystem
        Map<String, Object> marketplaceComponent = new LinkedHashMap<>();
        long configCount = platformConfigRepository.count();
        marketplaceComponent.put("status", "UP");
        marketplaceComponent.put("activeRulesCount", configCount);
        marketplaceComponent.put("matchingEngine", "ACTIVE");
        components.put("marketplace", marketplaceComponent);

        // 5. Wallet & Financial Accounting Subsystem
        Map<String, Object> walletComponent = new LinkedHashMap<>();
        walletComponent.put("status", "UP");
        walletComponent.put("walletsCount", workshopWalletRepository.count());
        components.put("wallets", walletComponent);

        // 6. Razorpay Gateway Status (strictly masked/safe)
        Map<String, Object> paymentComponent = new LinkedHashMap<>();
        paymentComponent.put("status", razorpayEnabled ? "ENABLED" : "TEST_OR_DISABLED");
        paymentComponent.put("mockGatewayActive", razorpayMockGateway);
        paymentComponent.put("keyIdConfigured", razorpayKeyId != null && !razorpayKeyId.isBlank());
        components.put("paymentGateway", paymentComponent);

        String overallStatus = dbHealthy ? "HEALTHY" : "DEGRADED";

        return new AdminSystemHealthResponse(
                overallStatus,
                "Car Service Platform - Enterprise Core",
                "1.0.0",
                LocalDateTime.now().toString(),
                uptimeSeconds,
                components
        );
    }

    @Transactional(readOnly = true)
    public AdminSystemSettingsResponse getSystemSettings() {
        String activeProfiles = String.join(", ", environment.getActiveProfiles());
        if (activeProfiles.isBlank()) {
            activeProfiles = "default";
        }

        // Marketplace Rules Map
        Map<String, Object> rules = new LinkedHashMap<>();
        List<PlatformConfig> configs = platformConfigRepository.findAll();
        for (PlatformConfig config : configs) {
            rules.put(config.getConfigKey(), Map.of(
                    "value", config.getConfigValue(),
                    "description", config.getDescription() != null ? config.getDescription() : "",
                    "type", config.getDataType() != null ? config.getDataType() : "STRING",
                    "updatedAt", config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : ""
            ));
        }

        // Security Policies
        Map<String, Object> securityPolicy = new LinkedHashMap<>();
        securityPolicy.put("accessTokenValidityMinutes", jwtAccessExpiration / (1000 * 60));
        securityPolicy.put("refreshTokenValidityDays", jwtRefreshExpiration / (1000 * 60 * 60 * 24));
        securityPolicy.put("passwordResetTokenValidityMinutes", passwordResetExpiration);
        securityPolicy.put("enforceRbac", true);
        securityPolicy.put("allowedRoles", List.of("CUSTOMER", "PARTNER", "ADMIN"));
        securityPolicy.put("passwordHashing", "BCrypt (Standard Salted)");

        // Payment Gateway (masked)
        Map<String, Object> paymentGateway = new LinkedHashMap<>();
        paymentGateway.put("provider", "Razorpay");
        paymentGateway.put("currency", "INR");
        paymentGateway.put("enabled", razorpayEnabled);
        paymentGateway.put("mockGateway", razorpayMockGateway);
        if (razorpayKeyId != null && razorpayKeyId.length() > 6) {
            paymentGateway.put("keyIdMasked", razorpayKeyId.substring(0, 4) + "..." + razorpayKeyId.substring(razorpayKeyId.length() - 2));
        } else if (razorpayKeyId != null && !razorpayKeyId.isBlank()) {
            paymentGateway.put("keyIdMasked", "rzp_***");
        } else {
            paymentGateway.put("keyIdMasked", "NOT_CONFIGURED");
        }

        return new AdminSystemSettingsResponse(
                "Car Service Platform",
                environment.getProperty("spring.profiles.active", "development"),
                "1.0.0",
                "MySQL 8.x / InnoDB",
                System.getProperty("java.version"),
                SpringBootVersion.getVersion(),
                LocalDateTime.now().toString(),
                activeProfiles,
                rules,
                securityPolicy,
                paymentGateway
        );
    }
}

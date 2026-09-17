package com.carservice.backend.marketplace.service;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.dto.PlatformConfigResponse;
import com.carservice.backend.marketplace.entity.PlatformConfig;
import com.carservice.backend.marketplace.entity.PlatformConfigHistory;
import com.carservice.backend.marketplace.enums.MarketplaceEventType;
import com.carservice.backend.marketplace.repository.PlatformConfigHistoryRepository;
import com.carservice.backend.marketplace.repository.PlatformConfigRepository;
import com.carservice.backend.user.entity.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlatformConfigService {

    private static final Logger log = LoggerFactory.getLogger(PlatformConfigService.class);

    // Canonical Configuration Keys
    public static final String KEY_LEAD_FEE = "MARKETPLACE_ACCEPTANCE_FEE";
    public static final String LEGACY_KEY_LEAD_FEE = "lead_acceptance_fee";
    public static final String KEY_MARKETPLACE_ENABLED = "MARKETPLACE_ENABLED";
    public static final String KEY_OPPORTUNITY_EXPIRY_MINUTES = "MARKETPLACE_OPPORTUNITY_EXPIRY_MINUTES";
    public static final String KEY_MATCHING_DEFAULT_RADIUS_KM = "MATCHING_DEFAULT_RADIUS_KM";
    public static final String KEY_MATCHING_MAX_WORKSHOPS = "MATCHING_MAX_WORKSHOPS_PER_REQUEST";
    public static final String KEY_WALLET_MIN_TOPUP = "WALLET_MIN_TOPUP_AMOUNT";
    public static final String KEY_WALLET_MAX_TOPUP = "WALLET_MAX_TOPUP_AMOUNT";

    // Defaults
    public static final BigDecimal DEFAULT_LEAD_FEE = new BigDecimal("99.00");
    public static final boolean DEFAULT_MARKETPLACE_ENABLED = true;
    public static final int DEFAULT_OPPORTUNITY_EXPIRY_MINUTES = 30;
    public static final double DEFAULT_MATCHING_RADIUS_KM = 25.0;
    public static final int DEFAULT_MATCHING_MAX_WORKSHOPS = 8;
    public static final BigDecimal DEFAULT_WALLET_MIN_TOPUP = new BigDecimal("100.00");
    public static final BigDecimal DEFAULT_WALLET_MAX_TOPUP = new BigDecimal("50000.00");

    private final PlatformConfigRepository platformConfigRepository;
    private final PlatformConfigHistoryRepository platformConfigHistoryRepository;
    private final MarketplaceAuditService auditService;

    public PlatformConfigService(
            PlatformConfigRepository platformConfigRepository,
            PlatformConfigHistoryRepository platformConfigHistoryRepository,
            MarketplaceAuditService auditService
    ) {
        this.platformConfigRepository = platformConfigRepository;
        this.platformConfigHistoryRepository = platformConfigHistoryRepository;
        this.auditService = auditService;
    }

    @PostConstruct
    @Transactional
    public void initDefaultConfigs() {
        // Seed default configs if not already present
        ensureConfig(
                KEY_LEAD_FEE,
                DEFAULT_LEAD_FEE.toPlainString(),
                "Workshop Acceptance Fee",
                "DECIMAL",
                "PAYMENT",
                "INR",
                new BigDecimal("1.00"),
                new BigDecimal("10000.00"),
                "Amount a partner workshop pays to claim a lead and unlock customer contact details. Applies to new opportunities only."
        );

        ensureConfig(
                KEY_MARKETPLACE_ENABLED,
                String.valueOf(DEFAULT_MARKETPLACE_ENABLED),
                "Marketplace Dispatch Engine",
                "BOOLEAN",
                "MARKETPLACE",
                "Boolean",
                null,
                null,
                "Master switch enabling automatic partner matching and opportunity broadcasting for new customer service requests."
        );

        ensureConfig(
                KEY_OPPORTUNITY_EXPIRY_MINUTES,
                String.valueOf(DEFAULT_OPPORTUNITY_EXPIRY_MINUTES),
                "Opportunity Acceptance Window",
                "INTEGER",
                "MARKETPLACE",
                "minutes",
                new BigDecimal("5"),
                new BigDecimal("1440"),
                "Validity window in minutes an opportunity remains available for a workshop before expiring."
        );

        ensureConfig(
                KEY_MATCHING_DEFAULT_RADIUS_KM,
                new BigDecimal(DEFAULT_MATCHING_RADIUS_KM).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "Default Matching Radius",
                "DECIMAL",
                "MATCHING",
                "km",
                new BigDecimal("1.00"),
                new BigDecimal("100.00"),
                "Default geographical search radius in kilometers when evaluating workshop coverage."
        );

        ensureConfig(
                KEY_MATCHING_MAX_WORKSHOPS,
                String.valueOf(DEFAULT_MATCHING_MAX_WORKSHOPS),
                "Max Workshops Broadcast Limit",
                "INTEGER",
                "MATCHING",
                "workshops",
                new BigDecimal("1"),
                new BigDecimal("50"),
                "Maximum number of eligible partner workshops that can be simultaneously notified for a single service request."
        );

        ensureConfig(
                KEY_WALLET_MIN_TOPUP,
                DEFAULT_WALLET_MIN_TOPUP.toPlainString(),
                "Minimum Wallet Top-up",
                "DECIMAL",
                "WALLET",
                "INR",
                new BigDecimal("10.00"),
                new BigDecimal("10000.00"),
                "Minimum rupee balance a workshop partner can add to their marketplace wallet per transaction."
        );

        ensureConfig(
                KEY_WALLET_MAX_TOPUP,
                DEFAULT_WALLET_MAX_TOPUP.toPlainString(),
                "Maximum Wallet Top-up",
                "DECIMAL",
                "WALLET",
                "INR",
                new BigDecimal("1000.00"),
                new BigDecimal("500000.00"),
                "Maximum rupee balance a workshop partner can add to their marketplace wallet in a single transaction."
        );

        // Synchronize legacy key if present
        Optional<PlatformConfig> legacyFee = platformConfigRepository.findByConfigKey(LEGACY_KEY_LEAD_FEE);
        if (legacyFee.isPresent()) {
            Optional<PlatformConfig> canonicalFee = platformConfigRepository.findByConfigKey(KEY_LEAD_FEE);
            if (canonicalFee.isPresent() && !canonicalFee.get().getConfigValue().equals(legacyFee.get().getConfigValue())) {
                canonicalFee.get().setConfigValue(legacyFee.get().getConfigValue());
                platformConfigRepository.save(canonicalFee.get());
            }
        }
    }

    private void ensureConfig(
            String key,
            String defaultValue,
            String friendlyName,
            String dataType,
            String category,
            String unit,
            BigDecimal minVal,
            BigDecimal maxVal,
            String description
    ) {
        if (platformConfigRepository.findByConfigKey(key).isEmpty()) {
            PlatformConfig config = new PlatformConfig(
                    key,
                    defaultValue,
                    friendlyName,
                    dataType,
                    category,
                    unit,
                    minVal,
                    maxVal,
                    description,
                    "SYSTEM"
            );
            platformConfigRepository.save(config);
            log.info("Initialized default platform configuration: {}", key);
        }
    }

    // ==========================================
    // TYPED DOMAIN ACCESSORS
    // ==========================================

    @Transactional(readOnly = true)
    public BigDecimal getLeadAcceptanceFee() {
        return platformConfigRepository.findByConfigKey(KEY_LEAD_FEE)
                .or(() -> platformConfigRepository.findByConfigKey(LEGACY_KEY_LEAD_FEE))
                .map(config -> {
                    try {
                        return new BigDecimal(config.getConfigValue()).setScale(2, RoundingMode.HALF_UP);
                    } catch (Exception e) {
                        return DEFAULT_LEAD_FEE;
                    }
                })
                .orElse(DEFAULT_LEAD_FEE);
    }

    @Transactional(readOnly = true)
    public boolean isMarketplaceEnabled() {
        return platformConfigRepository.findByConfigKey(KEY_MARKETPLACE_ENABLED)
                .map(c -> Boolean.parseBoolean(c.getConfigValue()))
                .orElse(DEFAULT_MARKETPLACE_ENABLED);
    }

    @Transactional(readOnly = true)
    public int getOpportunityExpiryMinutes() {
        return platformConfigRepository.findByConfigKey(KEY_OPPORTUNITY_EXPIRY_MINUTES)
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getConfigValue());
                    } catch (Exception e) {
                        return DEFAULT_OPPORTUNITY_EXPIRY_MINUTES;
                    }
                })
                .orElse(DEFAULT_OPPORTUNITY_EXPIRY_MINUTES);
    }

    @Transactional(readOnly = true)
    public double getMatchingDefaultRadiusKm() {
        return platformConfigRepository.findByConfigKey(KEY_MATCHING_DEFAULT_RADIUS_KM)
                .map(c -> {
                    try {
                        return Double.parseDouble(c.getConfigValue());
                    } catch (Exception e) {
                        return DEFAULT_MATCHING_RADIUS_KM;
                    }
                })
                .orElse(DEFAULT_MATCHING_RADIUS_KM);
    }

    @Transactional(readOnly = true)
    public int getMaxWorkshopsPerRequest() {
        return platformConfigRepository.findByConfigKey(KEY_MATCHING_MAX_WORKSHOPS)
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getConfigValue());
                    } catch (Exception e) {
                        return DEFAULT_MATCHING_MAX_WORKSHOPS;
                    }
                })
                .orElse(DEFAULT_MATCHING_MAX_WORKSHOPS);
    }

    @Transactional(readOnly = true)
    public BigDecimal getWalletMinTopup() {
        return platformConfigRepository.findByConfigKey(KEY_WALLET_MIN_TOPUP)
                .map(c -> {
                    try {
                        return new BigDecimal(c.getConfigValue()).setScale(2, RoundingMode.HALF_UP);
                    } catch (Exception e) {
                        return DEFAULT_WALLET_MIN_TOPUP;
                    }
                })
                .orElse(DEFAULT_WALLET_MIN_TOPUP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getWalletMaxTopup() {
        return platformConfigRepository.findByConfigKey(KEY_WALLET_MAX_TOPUP)
                .map(c -> {
                    try {
                        return new BigDecimal(c.getConfigValue()).setScale(2, RoundingMode.HALF_UP);
                    } catch (Exception e) {
                        return DEFAULT_WALLET_MAX_TOPUP;
                    }
                })
                .orElse(DEFAULT_WALLET_MAX_TOPUP);
    }

    // ==========================================
    // BACKWARD-COMPATIBLE FEE UPDATE METHOD
    // ==========================================

    @Transactional
    public PlatformConfigResponse updateLeadAcceptanceFee(BigDecimal newFee) {
        if (newFee == null || newFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Lead acceptance fee must be greater than zero");
        }
        BigDecimal normalizedFee = newFee.setScale(2, RoundingMode.HALF_UP);
        PlatformConfig saved = updateConfig(
                KEY_LEAD_FEE,
                normalizedFee.toPlainString(),
                "Updated lead acceptance fee",
                null,
                null
        );

        // Also sync legacy key if it exists
        platformConfigRepository.findByConfigKey(LEGACY_KEY_LEAD_FEE).ifPresent(legacy -> {
            legacy.setConfigValue(normalizedFee.toPlainString());
            platformConfigRepository.save(legacy);
        });

        return new PlatformConfigResponse(
                saved.getConfigKey(),
                saved.getConfigValue(),
                saved.getDescription(),
                saved.getUpdatedAt()
        );
    }

    // ==========================================
    // ROBUST CONFIGURATION UPDATE & VALIDATION
    // ==========================================

    @Transactional
    public PlatformConfig updateConfig(
            String key,
            String newValue,
            String reason,
            Long expectedVersion,
            User admin
    ) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A justification reason is required for updating platform configuration.");
        }
        if (newValue == null || newValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration value cannot be empty.");
        }

        String targetKey = key.trim();
        // Support legacy key transparently
        if (LEGACY_KEY_LEAD_FEE.equalsIgnoreCase(targetKey)) {
            targetKey = KEY_LEAD_FEE;
        }

        PlatformConfig config = platformConfigRepository.findByConfigKey(targetKey)
                .orElseThrow(() -> new ResourceNotFoundException("Platform configuration not found for key: " + key));

        // Optimistic Concurrency Check
        if (expectedVersion != null && config.getVersion() != null) {
            if (!config.getVersion().equals(expectedVersion)) {
                log.warn("Optimistic locking conflict on {}: expected version {} but found {}",
                        targetKey, expectedVersion, config.getVersion());
                throw new OptimisticLockingFailureException(
                        "Configuration '" + targetKey + "' was updated by another administrator. Please refresh and try again."
                );
            }
        }

        String normalizedValue = validateAndNormalizeValue(config, newValue.trim());
        String oldValue = config.getConfigValue();

        // Update config state
        config.setConfigValue(normalizedValue);
        config.setUpdatedBy(admin != null ? admin.getEmail() : "SYSTEM");
        config.setUpdatedAt(LocalDateTime.now());
        PlatformConfig savedConfig = platformConfigRepository.save(config);

        // If updating fee, keep legacy key in sync
        if (KEY_LEAD_FEE.equals(targetKey)) {
            platformConfigRepository.findByConfigKey(LEGACY_KEY_LEAD_FEE).ifPresent(legacy -> {
                legacy.setConfigValue(normalizedValue);
                platformConfigRepository.save(legacy);
            });
        }

        // Record History
        PlatformConfigHistory history = new PlatformConfigHistory(
                savedConfig.getConfigKey(),
                oldValue,
                normalizedValue,
                savedConfig.getCategory(),
                admin != null ? admin.getEmail() : "SYSTEM",
                admin != null ? admin.getId() : null,
                reason.trim()
        );
        platformConfigHistoryRepository.save(history);

        // Record Audit Event
        auditService.recordEvent(
                MarketplaceEventType.CONFIGURATION_CHANGED,
                null,
                null,
                null,
                admin != null ? admin.getId() : null,
                "Platform configuration '" + savedConfig.getConfigKey() + "' updated from '" + oldValue + "' to '" + normalizedValue + "'. Reason: " + reason.trim(),
                "{\"configKey\": \"" + savedConfig.getConfigKey() + "\", \"oldValue\": \"" + oldValue + "\", \"newValue\": \"" + normalizedValue + "\", \"reason\": \"" + reason.trim() + "\"}"
        );

        log.info("Successfully updated platform configuration '{}': {} -> {} (reason: {})",
                targetKey, oldValue, normalizedValue, reason.trim());

        return savedConfig;
    }

    private String validateAndNormalizeValue(PlatformConfig config, String rawValue) {
        String dataType = config.getDataType() != null ? config.getDataType().toUpperCase() : "STRING";

        switch (dataType) {
            case "DECIMAL": {
                BigDecimal decimalVal;
                try {
                    decimalVal = new BigDecimal(rawValue).setScale(2, RoundingMode.HALF_UP);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Value must be a valid decimal number: " + rawValue);
                }

                if (KEY_LEAD_FEE.equals(config.getConfigKey()) && decimalVal.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Workshop acceptance fee must be greater than zero.");
                }

                if (config.getMinVal() != null && decimalVal.compareTo(config.getMinVal()) < 0) {
                    throw new IllegalArgumentException(
                            config.getFriendlyName() + " cannot be less than " + config.getMinVal() + " " + (config.getUnit() != null ? config.getUnit() : "")
                    );
                }
                if (config.getMaxVal() != null && decimalVal.compareTo(config.getMaxVal()) > 0) {
                    throw new IllegalArgumentException(
                            config.getFriendlyName() + " cannot exceed " + config.getMaxVal() + " " + (config.getUnit() != null ? config.getUnit() : "")
                    );
                }

                // Cross-field wallet bounds validation
                if (KEY_WALLET_MIN_TOPUP.equals(config.getConfigKey())) {
                    BigDecimal currentMax = getWalletMaxTopup();
                    if (decimalVal.compareTo(currentMax) > 0) {
                        throw new IllegalArgumentException("Minimum top-up amount (" + decimalVal + ") cannot exceed maximum top-up amount (" + currentMax + ").");
                    }
                } else if (KEY_WALLET_MAX_TOPUP.equals(config.getConfigKey())) {
                    BigDecimal currentMin = getWalletMinTopup();
                    if (decimalVal.compareTo(currentMin) < 0) {
                        throw new IllegalArgumentException("Maximum top-up amount (" + decimalVal + ") cannot be less than minimum top-up amount (" + currentMin + ").");
                    }
                }

                return decimalVal.toPlainString();
            }

            case "INTEGER": {
                int intVal;
                try {
                    intVal = Integer.parseInt(rawValue);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Value must be a valid integer: " + rawValue);
                }

                if (config.getMinVal() != null && intVal < config.getMinVal().intValue()) {
                    throw new IllegalArgumentException(
                            config.getFriendlyName() + " cannot be less than " + config.getMinVal().intValue() + " " + (config.getUnit() != null ? config.getUnit() : "")
                    );
                }
                if (config.getMaxVal() != null && intVal > config.getMaxVal().intValue()) {
                    throw new IllegalArgumentException(
                            config.getFriendlyName() + " cannot exceed " + config.getMaxVal().intValue() + " " + (config.getUnit() != null ? config.getUnit() : "")
                    );
                }

                return String.valueOf(intVal);
            }

            case "BOOLEAN": {
                if (!"true".equalsIgnoreCase(rawValue) && !"false".equalsIgnoreCase(rawValue)) {
                    throw new IllegalArgumentException("Value must be either 'true' or 'false'.");
                }
                return String.valueOf(Boolean.parseBoolean(rawValue));
            }

            default:
                return rawValue;
        }
    }

    // ==========================================
    // QUERIES
    // ==========================================

    @Transactional(readOnly = true)
    public List<PlatformConfig> getAllConfigsWithMetadata() {
        return platformConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PlatformConfigResponse> getAllConfigs() {
        return platformConfigRepository.findAll().stream()
                .map(c -> new PlatformConfigResponse(c.getConfigKey(), c.getConfigValue(), c.getDescription(), c.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlatformConfig getConfigByKey(String key) {
        String targetKey = key.trim();
        if (LEGACY_KEY_LEAD_FEE.equalsIgnoreCase(targetKey)) {
            targetKey = KEY_LEAD_FEE;
        }
        return platformConfigRepository.findByConfigKey(targetKey)
                .orElseThrow(() -> new ResourceNotFoundException("Platform configuration not found for key: " + key));
    }

    @Transactional(readOnly = true)
    public List<PlatformConfigHistory> getHistoryForKey(String key) {
        String targetKey = key.trim();
        if (LEGACY_KEY_LEAD_FEE.equalsIgnoreCase(targetKey)) {
            targetKey = KEY_LEAD_FEE;
        }
        return platformConfigHistoryRepository.findByConfigKeyOrderByChangedAtDesc(targetKey);
    }

    @Transactional(readOnly = true)
    public List<PlatformConfigHistory> getAllHistory() {
        return platformConfigHistoryRepository.findAllByOrderByChangedAtDesc();
    }

    @Transactional(readOnly = true)
    public String getConfigValue(String key, String defaultValue) {
        return platformConfigRepository.findByConfigKey(key)
                .map(PlatformConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Transactional
    public void setConfigValue(String key, String value, String description) {
        PlatformConfig config = platformConfigRepository.findByConfigKey(key)
                .orElseGet(() -> new PlatformConfig(key, value, description));
        config.setConfigValue(value);
        if (description != null) {
            config.setDescription(description);
        }
        platformConfigRepository.save(config);
    }
}

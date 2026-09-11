package com.carservice.backend.marketplace.service;

import com.carservice.backend.marketplace.dto.PlatformConfigResponse;
import com.carservice.backend.marketplace.entity.PlatformConfig;
import com.carservice.backend.marketplace.repository.PlatformConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlatformConfigService {

    public static final String KEY_LEAD_FEE = "lead_acceptance_fee";
    public static final BigDecimal DEFAULT_LEAD_FEE = new BigDecimal("99.00");

    private final PlatformConfigRepository platformConfigRepository;

    public PlatformConfigService(PlatformConfigRepository platformConfigRepository) {
        this.platformConfigRepository = platformConfigRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal getLeadAcceptanceFee() {
        return platformConfigRepository.findByConfigKey(KEY_LEAD_FEE)
                .map(config -> {
                    try {
                        return new BigDecimal(config.getConfigValue()).setScale(2, RoundingMode.HALF_UP);
                    } catch (Exception e) {
                        return DEFAULT_LEAD_FEE;
                    }
                })
                .orElse(DEFAULT_LEAD_FEE);
    }

    @Transactional
    public PlatformConfigResponse updateLeadAcceptanceFee(BigDecimal newFee) {
        if (newFee == null || newFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Lead fee cannot be negative");
        }
        BigDecimal normalizedFee = newFee.setScale(2, RoundingMode.HALF_UP);

        PlatformConfig config = platformConfigRepository.findByConfigKey(KEY_LEAD_FEE)
                .orElseGet(() -> new PlatformConfig(KEY_LEAD_FEE, normalizedFee.toPlainString(), "Dynamic workshop lead acceptance fee in INR"));

        config.setConfigValue(normalizedFee.toPlainString());
        PlatformConfig saved = platformConfigRepository.save(config);

        return new PlatformConfigResponse(
                saved.getConfigKey(),
                saved.getConfigValue(),
                saved.getDescription(),
                saved.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<PlatformConfigResponse> getAllConfigs() {
        return platformConfigRepository.findAll().stream()
                .map(c -> new PlatformConfigResponse(c.getConfigKey(), c.getConfigValue(), c.getDescription(), c.getUpdatedAt()))
                .collect(Collectors.toList());
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

package com.carservice.backend.admin.controller;

import com.carservice.backend.admin.dto.AdminPlatformConfigHistoryResponse;
import com.carservice.backend.admin.dto.AdminPlatformConfigResponse;
import com.carservice.backend.admin.dto.AdminPlatformConfigUpdateRequest;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.entity.PlatformConfig;
import com.carservice.backend.marketplace.service.PlatformConfigService;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/configuration")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlatformConfigController {

    private final PlatformConfigService platformConfigService;

    public AdminPlatformConfigController(PlatformConfigService platformConfigService) {
        this.platformConfigService = platformConfigService;
    }

    /**
     * Get all platform configurations with metadata.
     * GET /api/v1/admin/configuration
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminPlatformConfigResponse>>> getAllConfigurations() {
        List<AdminPlatformConfigResponse> responses = platformConfigService.getAllConfigsWithMetadata().stream()
                .map(AdminPlatformConfigResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Platform configurations retrieved successfully", responses));
    }

    /**
     * Get single configuration by key.
     * GET /api/v1/admin/configuration/{key}
     */
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<AdminPlatformConfigResponse>> getConfiguration(@PathVariable String key) {
        PlatformConfig config = platformConfigService.getConfigByKey(key);
        return ResponseEntity.ok(ApiResponse.success("Platform configuration retrieved", new AdminPlatformConfigResponse(config)));
    }

    /**
     * Update configuration value with mandatory justification reason and optimistic locking version check.
     * PUT /api/v1/admin/configuration/{key}
     */
    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<AdminPlatformConfigResponse>> updateConfiguration(
            @PathVariable String key,
            @Valid @RequestBody AdminPlatformConfigUpdateRequest request,
            Authentication authentication
    ) {
        User adminUser = (authentication != null && authentication.getPrincipal() instanceof User)
                ? (User) authentication.getPrincipal()
                : null;

        try {
            PlatformConfig updated = platformConfigService.updateConfig(
                    key,
                    request.getValue(),
                    request.getReason(),
                    request.getVersion(),
                    adminUser
            );
            return ResponseEntity.ok(ApiResponse.success(
                    "Configuration '" + updated.getConfigKey() + "' updated successfully",
                    new AdminPlatformConfigResponse(updated)
            ));
        } catch (OptimisticLockingFailureException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    /**
     * Get configuration change audit history across all keys.
     * GET /api/v1/admin/configuration/history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AdminPlatformConfigHistoryResponse>>> getAllHistory() {
        List<AdminPlatformConfigHistoryResponse> history = platformConfigService.getAllHistory().stream()
                .map(AdminPlatformConfigHistoryResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Configuration change history retrieved successfully", history));
    }

    /**
     * Get configuration change audit history for a specific key.
     * GET /api/v1/admin/configuration/history/{key}
     */
    @GetMapping("/history/{key}")
    public ResponseEntity<ApiResponse<List<AdminPlatformConfigHistoryResponse>>> getHistoryForKey(@PathVariable String key) {
        List<AdminPlatformConfigHistoryResponse> history = platformConfigService.getHistoryForKey(key).stream()
                .map(AdminPlatformConfigHistoryResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Configuration history for '" + key + "' retrieved successfully", history));
    }
}

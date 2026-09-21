package com.carservice.backend.admin.controller;

import com.carservice.backend.admin.dto.AdminSystemHealthResponse;
import com.carservice.backend.admin.dto.AdminSystemSettingsResponse;
import com.carservice.backend.admin.service.AdminSystemService;
import com.carservice.backend.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative System Health and Operational Controls Controller.
 * Protected strictly by ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/system")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemController {

    private final AdminSystemService adminSystemService;

    public AdminSystemController(AdminSystemService adminSystemService) {
        this.adminSystemService = adminSystemService;
    }

    /**
     * Live component operational health check.
     * GET /api/v1/admin/system/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<AdminSystemHealthResponse>> getSystemHealth() {
        AdminSystemHealthResponse health = adminSystemService.getSystemHealth();
        return ResponseEntity.ok(ApiResponse.success("System operational health retrieved successfully", health));
    }

    /**
     * Platform operational settings and active system configurations.
     * GET /api/v1/admin/system/settings
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<AdminSystemSettingsResponse>> getSystemSettings() {
        AdminSystemSettingsResponse settings = adminSystemService.getSystemSettings();
        return ResponseEntity.ok(ApiResponse.success("System operational settings retrieved successfully", settings));
    }
}

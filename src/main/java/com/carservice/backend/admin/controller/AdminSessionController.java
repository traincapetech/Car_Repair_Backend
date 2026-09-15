package com.carservice.backend.admin.controller;

import com.carservice.backend.admin.dto.AdminProfileResponse;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Administrative Session and Core Health Controller.
 * Protected by strict server-side RBAC (ROLE_ADMIN).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSessionController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminProfileResponse>> getAdminProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        AdminProfileResponse profile = new AdminProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Admin session profile retrieved successfully", profile)
        );
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminHealth(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("subsystem", "Admin Governance & RBAC");
        health.put("version", "1.0.0");
        health.put("authenticatedAdmin", user.getEmail());
        health.put("role", user.getRole().name());
        health.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(
                ApiResponse.success("Admin subsystem is healthy and operational", health)
        );
    }
}

package com.carservice.backend.user.controller;

import com.carservice.backend.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class RoleTestController {

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> customerEndpoint() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Customer access granted",
                        "You have CUSTOMER access"
                )
        );
    }

    @GetMapping("/advisor")
    @PreAuthorize("hasRole('SERVICE_ADVISOR')")
    public ResponseEntity<ApiResponse<String>> advisorEndpoint() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Service advisor access granted",
                        "You have SERVICE_ADVISOR access"
                )
        );
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminEndpoint() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Admin access granted",
                        "You have ADMIN access"
                )
        );
    }
}
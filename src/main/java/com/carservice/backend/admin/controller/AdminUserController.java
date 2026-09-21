package com.carservice.backend.admin.controller;

import com.carservice.backend.admin.dto.*;
import com.carservice.backend.admin.service.AdminUserService;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<AdminUserListResponse>>> getUsers(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "DESC") String direction
    ) {
        Page<AdminUserListResponse> users = adminUserService.getUsers(page, size, search, role, status, sort, direction);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/users/summary")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserSummaryResponse>> getUserSummary() {
        AdminUserSummaryResponse summary = adminUserService.getUserSummary();
        return ResponseEntity.ok(ApiResponse.success("User summary retrieved successfully", summary));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetail(@PathVariable Long id) {
        AdminUserDetailResponse detail = adminUserService.getUserDetail(id);
        return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", detail));
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('USER_STATUS_CHANGE') or hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        User adminUser = getAuthenticatedUser(authentication);
        AdminUserDetailResponse updated = adminUserService.updateUserStatus(id, request, adminUser);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", updated));
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        User adminUser = getAuthenticatedUser(authentication);
        AdminUserDetailResponse updated = adminUserService.updateUserRole(id, request, adminUser);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", updated));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_VIEW') or hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminRoleResponse>>> getRoles() {
        List<AdminRoleResponse> roles = adminUserService.getRoles();
        return ResponseEntity.ok(ApiResponse.success("Roles catalog retrieved successfully", roles));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }
}

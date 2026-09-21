package com.carservice.backend.admin.controller;

import com.carservice.backend.admin.dto.AdminAuditLogDetailResponse;
import com.carservice.backend.admin.dto.AdminAuditLogResponse;
import com.carservice.backend.admin.dto.AdminAuditSummaryResponse;
import com.carservice.backend.admin.service.AuditService;
import com.carservice.backend.common.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Enterprise Audit Log Controller.
 * Restricted strictly to ROLE_ADMIN.
 * Immutable: No mutating endpoints (POST/PUT/DELETE) exist.
 */
@RestController
@RequestMapping("/api/v1/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Search and paginate immutable audit logs with multi-attribute filtering.
     * GET /api/v1/admin/audit/events
     */
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<Page<AdminAuditLogResponse>>> getAuditEvents(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(100, Math.max(1, size));

        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String validSortField = "createdAt";
        if ("action".equalsIgnoreCase(sort) || "entityType".equalsIgnoreCase(sort) || "status".equalsIgnoreCase(sort)) {
            validSortField = sort;
        }

        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(sortDirection, validSortField));

        Page<AdminAuditLogResponse> logs = auditService.searchAuditLogs(
                action,
                entityType,
                entityId,
                status,
                actorUserId,
                actorEmail,
                startDate,
                endDate,
                search,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success("Audit events retrieved successfully", logs));
    }

    /**
     * Get detailed audit entry with JSON before/after states and full diff.
     * GET /api/v1/admin/audit/events/{id}
     */
    @GetMapping("/events/{id}")
    public ResponseEntity<ApiResponse<AdminAuditLogDetailResponse>> getAuditEventDetail(@PathVariable Long id) {
        AdminAuditLogDetailResponse detail = auditService.getAuditLogDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Audit event detail retrieved", detail));
    }

    /**
     * Get aggregate KPI metrics for audit events.
     * GET /api/v1/admin/audit/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminAuditSummaryResponse>> getAuditSummary() {
        AdminAuditSummaryResponse summary = auditService.getAuditSummary();
        return ResponseEntity.ok(ApiResponse.success("Audit summary retrieved successfully", summary));
    }
}

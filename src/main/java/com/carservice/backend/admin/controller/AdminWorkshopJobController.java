package com.carservice.backend.admin.controller;

import com.carservice.backend.admin.dto.AdminUpdateJobStatusRequest;
import com.carservice.backend.admin.dto.AdminWorkshopJobDetailResponse;
import com.carservice.backend.admin.dto.AdminWorkshopJobListResponse;
import com.carservice.backend.admin.dto.AdminWorkshopJobSummaryResponse;
import com.carservice.backend.admin.service.AdminWorkshopJobService;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/workshop-jobs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWorkshopJobController {

    private final AdminWorkshopJobService adminWorkshopJobService;

    public AdminWorkshopJobController(AdminWorkshopJobService adminWorkshopJobService) {
        this.adminWorkshopJobService = adminWorkshopJobService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminWorkshopJobSummaryResponse>> getJobSummary() {
        AdminWorkshopJobSummaryResponse summary = adminWorkshopJobService.getJobSummary();
        return ResponseEntity.ok(ApiResponse.success("Workshop job summary metrics retrieved successfully", summary));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminWorkshopJobListResponse>>> getWorkshopJobs(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long workshopId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "DESC") String direction
    ) {
        WorkshopJobStatus jobStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                jobStatus = WorkshopJobStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Sort.Direction sortDir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sort == null || sort.trim().isEmpty()) ? "createdAt" : sort.trim();
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(sortDir, sortProperty));

        Page<AdminWorkshopJobListResponse> result = adminWorkshopJobService.getWorkshopJobs(
                search,
                jobStatus,
                workshopId,
                customerId,
                startDate,
                endDate,
                pageRequest
        );
        return ResponseEntity.ok(ApiResponse.success("Workshop jobs retrieved successfully", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminWorkshopJobDetailResponse>> getJobDetail(
            @PathVariable Long id
    ) {
        AdminWorkshopJobDetailResponse response = adminWorkshopJobService.getJobDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Workshop job 360 detail retrieved successfully", response));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminWorkshopJobDetailResponse>> updateJobStatus(
            @AuthenticationPrincipal User adminUser,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateJobStatusRequest request
    ) {
        AdminWorkshopJobDetailResponse response = adminWorkshopJobService.updateJobStatus(adminUser, id, request);
        return ResponseEntity.ok(ApiResponse.success("Workshop job status updated successfully by administration", response));
    }
}

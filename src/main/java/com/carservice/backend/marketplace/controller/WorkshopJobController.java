package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.UpdateJobStatusRequest;
import com.carservice.backend.marketplace.dto.WorkshopJobResponse;
import com.carservice.backend.marketplace.service.WorkshopJobService;
import com.carservice.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workshops/jobs")
@PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
public class WorkshopJobController {

    private final WorkshopJobService workshopJobService;

    public WorkshopJobController(WorkshopJobService workshopJobService) {
        this.workshopJobService = workshopJobService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkshopJobResponse>>> getMyJobs(
            Authentication authentication,
            @RequestParam(required = false) String status
    ) {
        User currentUser = (User) authentication.getPrincipal();
        List<WorkshopJobResponse> jobs = workshopJobService.getWorkshopJobs(currentUser, status);

        return ResponseEntity.ok(ApiResponse.success("Workshop jobs retrieved successfully", jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkshopJobResponse>> getJobDetails(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        WorkshopJobResponse job = workshopJobService.getJobDetails(currentUser, id);

        return ResponseEntity.ok(ApiResponse.success("Workshop job details retrieved successfully", job));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<WorkshopJobResponse>> updateJobStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobStatusRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        WorkshopJobResponse updated = workshopJobService.updateJobStatus(currentUser, id, request);

        return ResponseEntity.ok(ApiResponse.success("Workshop job status updated to " + updated.getStatus(), updated));
    }
}

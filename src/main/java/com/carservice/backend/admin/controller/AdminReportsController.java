package com.carservice.backend.admin.controller;

import com.carservice.backend.admin.dto.reports.*;
import com.carservice.backend.admin.service.AdminReportsService;
import com.carservice.backend.common.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportsController {

    private final AdminReportsService adminReportsService;

    public AdminReportsController(AdminReportsService adminReportsService) {
        this.adminReportsService = adminReportsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminPlatformOverviewReport>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        AdminPlatformOverviewReport report = adminReportsService.getPlatformOverview(from, to);
        return ResponseEntity.ok(ApiResponse.success("Platform overview report retrieved successfully", report));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<AdminCustomerReport>> getCustomerReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        AdminCustomerReport report = adminReportsService.getCustomerReport(from, to);
        return ResponseEntity.ok(ApiResponse.success("Customer report retrieved successfully", report));
    }

    @GetMapping("/workshops")
    public ResponseEntity<ApiResponse<AdminWorkshopReport>> getWorkshopReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        AdminWorkshopReport report = adminReportsService.getWorkshopReport(from, to);
        return ResponseEntity.ok(ApiResponse.success("Workshop report retrieved successfully", report));
    }

    @GetMapping("/service-requests")
    public ResponseEntity<ApiResponse<AdminServiceRequestReport>> getServiceRequestReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long workshopId
    ) {
        AdminServiceRequestReport report = adminReportsService.getServiceRequestReport(from, to, workshopId);
        return ResponseEntity.ok(ApiResponse.success("Service request report retrieved successfully", report));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<AdminBookingReport>> getBookingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long workshopId
    ) {
        AdminBookingReport report = adminReportsService.getBookingReport(from, to, workshopId);
        return ResponseEntity.ok(ApiResponse.success("Booking report retrieved successfully", report));
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<AdminServiceCatalogReport>> getServiceCatalogReport() {
        AdminServiceCatalogReport report = adminReportsService.getServiceCatalogReport();
        return ResponseEntity.ok(ApiResponse.success("Service catalog report retrieved successfully", report));
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<Page<AdminReportActivityItem>>> getActivity(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        Page<AdminReportActivityItem> activityPage = adminReportsService.getActivity(search, type, status, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success("Recent activity retrieved successfully", activityPage));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false, defaultValue = "OVERVIEW") String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String search
    ) {
        byte[] csvData = adminReportsService.generateCsvExport(reportType, from, to, search);
        String filename = "platform-report-" + reportType.toLowerCase() + "-" + System.currentTimeMillis() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}

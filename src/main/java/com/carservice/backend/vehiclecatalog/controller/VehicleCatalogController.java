package com.carservice.backend.vehiclecatalog.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.vehiclecatalog.dto.VehicleBrandResponse;
import com.carservice.backend.vehiclecatalog.dto.VehicleModelResponse;
import com.carservice.backend.vehiclecatalog.service.VehicleCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-catalog")
public class VehicleCatalogController {

    private final VehicleCatalogService vehicleCatalogService;

    public VehicleCatalogController(VehicleCatalogService vehicleCatalogService) {
        this.vehicleCatalogService = vehicleCatalogService;
    }

    /**
     * Get all active passenger vehicle brands available in India.
     */
    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<VehicleBrandResponse>>> getBrands() {
        List<VehicleBrandResponse> brands = vehicleCatalogService.getAllActiveBrands();
        return ResponseEntity.ok(ApiResponse.success("Vehicle brands fetched successfully", brands));
    }

    /**
     * Get all active models for a specific brand by ID.
     */
    @GetMapping("/brands/{brandId}/models")
    public ResponseEntity<ApiResponse<List<VehicleModelResponse>>> getModelsByBrandId(
            @PathVariable Long brandId
    ) {
        List<VehicleModelResponse> models = vehicleCatalogService.getModelsByBrandId(brandId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle models fetched successfully", models));
    }

    /**
     * Get all active models for a specific brand by brand name.
     */
    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<VehicleModelResponse>>> getModelsByBrandName(
            @RequestParam("brand") String brandName
    ) {
        List<VehicleModelResponse> models = vehicleCatalogService.getModelsByBrandName(brandName);
        return ResponseEntity.ok(ApiResponse.success("Vehicle models fetched successfully", models));
    }
}

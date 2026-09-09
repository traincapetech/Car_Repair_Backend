package com.carservice.backend.vehicle.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.vehicle.dto.CreateVehicleRequest;
import com.carservice.backend.vehicle.dto.UpdateVehicleRequest;
import com.carservice.backend.vehicle.dto.VehicleResponse;
import com.carservice.backend.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            Authentication authentication,
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        VehicleResponse response = vehicleService.createVehicle(currentUser, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle added successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getMyVehicles(
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        List<VehicleResponse> responses = vehicleService.getMyVehicles(currentUser);

        return ResponseEntity.ok(
                ApiResponse.success("Vehicles fetched successfully", responses)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getMyVehicle(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        VehicleResponse response = vehicleService.getMyVehicle(currentUser, id);

        return ResponseEntity.ok(
                ApiResponse.success("Vehicle fetched successfully", response)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        VehicleResponse response = vehicleService.updateVehicle(currentUser, id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Vehicle updated successfully", response)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        vehicleService.deleteVehicle(currentUser, id);

        return ResponseEntity.ok(
                ApiResponse.success("Vehicle deleted successfully", null)
        );
    }
}

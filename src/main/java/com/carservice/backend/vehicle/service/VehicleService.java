package com.carservice.backend.vehicle.service;

import com.carservice.backend.common.exception.VehicleAlreadyExistsException;
import com.carservice.backend.common.exception.VehicleNotFoundException;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.vehicle.dto.CreateVehicleRequest;
import com.carservice.backend.vehicle.dto.UpdateVehicleRequest;
import com.carservice.backend.vehicle.dto.VehicleResponse;
import com.carservice.backend.vehicle.entity.Vehicle;
import com.carservice.backend.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public VehicleResponse createVehicle(User user, CreateVehicleRequest request) {
        String normalizedRegistrationNumber = normalizeRegistrationNumber(request.getRegistrationNumber());

        if (vehicleRepository.existsByRegistrationNumber(normalizedRegistrationNumber)) {
            throw new VehicleAlreadyExistsException(
                    "Vehicle with registration number '" + normalizedRegistrationNumber + "' already exists"
            );
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUser(user);
        vehicle.setMake(request.getMake().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setYear(request.getYear());
        vehicle.setRegistrationNumber(normalizedRegistrationNumber);
        vehicle.setFuelType(request.getFuelType());
        vehicle.setTransmission(request.getTransmission());

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(savedVehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(User user) {
        List<Vehicle> vehicles = vehicleRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        return vehicles.stream()
                .map(VehicleResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getMyVehicle(User user, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdAndUserId(vehicleId, user.getId())
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found with id: " + vehicleId));

        return VehicleResponse.fromEntity(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(User user, Long vehicleId, UpdateVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findByIdAndUserId(vehicleId, user.getId())
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found with id: " + vehicleId));

        String normalizedRegistrationNumber = normalizeRegistrationNumber(request.getRegistrationNumber());

        if (!vehicle.getRegistrationNumber().equalsIgnoreCase(normalizedRegistrationNumber)) {
            if (vehicleRepository.existsByRegistrationNumberAndIdNot(normalizedRegistrationNumber, vehicleId)) {
                throw new VehicleAlreadyExistsException(
                        "Vehicle with registration number '" + normalizedRegistrationNumber + "' already exists"
                );
            }
        }

        vehicle.setMake(request.getMake().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setYear(request.getYear());
        vehicle.setRegistrationNumber(normalizedRegistrationNumber);
        vehicle.setFuelType(request.getFuelType());
        vehicle.setTransmission(request.getTransmission());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(updatedVehicle);
    }

    @Transactional
    public void deleteVehicle(User user, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdAndUserId(vehicleId, user.getId())
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found with id: " + vehicleId));

        vehicleRepository.delete(vehicle);
    }

    public String normalizeRegistrationNumber(String registrationNumber) {
        if (registrationNumber == null) {
            return "";
        }
        return registrationNumber.trim().replaceAll("[\\s-]", "").toUpperCase();
    }
}

package com.carservice.backend.admin.dto;

import com.carservice.backend.vehicle.enums.FuelType;
import com.carservice.backend.vehicle.enums.Transmission;

import java.time.LocalDateTime;

public class AdminCustomerVehicleResponse {
    private Long id;
    private String make;
    private String model;
    private Integer year;
    private String registrationNumber;
    private FuelType fuelType;
    private Transmission transmission;
    private LocalDateTime createdAt;

    public AdminCustomerVehicleResponse() {
    }

    public AdminCustomerVehicleResponse(
            Long id,
            String make,
            String model,
            Integer year,
            String registrationNumber,
            FuelType fuelType,
            Transmission transmission,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.year = year;
        this.registrationNumber = registrationNumber;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public Transmission getTransmission() {
        return transmission;
    }

    public void setTransmission(Transmission transmission) {
        this.transmission = transmission;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

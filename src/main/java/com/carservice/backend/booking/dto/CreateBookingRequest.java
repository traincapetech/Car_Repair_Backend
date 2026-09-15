package com.carservice.backend.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

public class CreateBookingRequest {

    @NotNull(message = "Vehicle ID is required")
    @Positive(message = "Vehicle ID must be positive")
    private Long vehicleId;

    @Positive(message = "Service ID must be positive")
    private Long serviceId;

    private List<@Positive(message = "Each service ID must be positive") Long> serviceIds;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    private LocalTime bookingTime;

    private String timeSlot;

    @Size(max = 1000, message = "Customer notes cannot exceed 1000 characters")
    private String customerNotes;

    private String city;
    private String address;
    private String pincode;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;

    public CreateBookingRequest() {
    }

    public CreateBookingRequest(
            Long vehicleId,
            Long serviceId,
            LocalDate bookingDate,
            LocalTime bookingTime,
            String customerNotes
    ) {
        this.vehicleId = vehicleId;
        this.serviceId = serviceId;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.customerNotes = customerNotes;
    }

    public CreateBookingRequest(
            Long vehicleId,
            Long serviceId,
            LocalDate bookingDate,
            String timeSlot,
            String customerNotes
    ) {
        this.vehicleId = vehicleId;
        this.serviceId = serviceId;
        this.bookingDate = bookingDate;
        this.timeSlot = timeSlot;
        this.customerNotes = customerNotes;
    }

    public CreateBookingRequest(
            Long vehicleId,
            List<Long> serviceIds,
            LocalDate bookingDate,
            String timeSlot,
            String customerNotes
    ) {
        this.vehicleId = vehicleId;
        this.serviceIds = serviceIds;
        this.bookingDate = bookingDate;
        this.timeSlot = timeSlot;
        this.customerNotes = customerNotes;
    }

    public List<Long> getEffectiveServiceIds() {
        if (serviceIds != null && !serviceIds.isEmpty()) {
            return serviceIds;
        }
        if (serviceId != null) {
            return List.of(serviceId);
        }
        return Collections.emptyList();
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public List<Long> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(List<Long> serviceIds) {
        this.serviceIds = serviceIds;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public java.math.BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(java.math.BigDecimal latitude) {
        this.latitude = latitude;
    }

    public java.math.BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(java.math.BigDecimal longitude) {
        this.longitude = longitude;
    }
}

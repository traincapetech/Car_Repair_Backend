package com.carservice.backend.booking.dto;

import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingResponse {

    private Long id;
    private VehicleSummary vehicle;
    private ServiceSummary service;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private BookingStatus status;
    private String customerNotes;
    private BigDecimal estimatedPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookingResponse() {
    }

    public BookingResponse(
            Long id,
            VehicleSummary vehicle,
            ServiceSummary service,
            LocalDate bookingDate,
            LocalTime bookingTime,
            BookingStatus status,
            String customerNotes,
            BigDecimal estimatedPrice,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.vehicle = vehicle;
        this.service = service;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.status = status;
        this.customerNotes = customerNotes;
        this.estimatedPrice = estimatedPrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static BookingResponse fromEntity(Booking booking) {
        if (booking == null) {
            return null;
        }

        VehicleSummary vehicleSummary = null;
        if (booking.getVehicle() != null) {
            vehicleSummary = new VehicleSummary(
                    booking.getVehicle().getId(),
                    booking.getVehicle().getMake(),
                    booking.getVehicle().getModel(),
                    booking.getVehicle().getRegistrationNumber()
            );
        }

        ServiceSummary serviceSummary = null;
        if (booking.getService() != null) {
            serviceSummary = new ServiceSummary(
                    booking.getService().getId(),
                    booking.getService().getName(),
                    booking.getService().getCategory(),
                    booking.getService().getBasePrice(),
                    booking.getService().getEstimatedDurationMinutes()
            );
        }

        return new BookingResponse(
                booking.getId(),
                vehicleSummary,
                serviceSummary,
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getStatus(),
                booking.getCustomerNotes(),
                booking.getEstimatedPrice(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VehicleSummary getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleSummary vehicle) {
        this.vehicle = vehicle;
    }

    public ServiceSummary getService() {
        return service;
    }

    public void setService(ServiceSummary service) {
        this.service = service;
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

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class VehicleSummary {
        private Long id;
        private String make;
        private String model;
        private String registrationNumber;

        public VehicleSummary() {
        }

        public VehicleSummary(Long id, String make, String model, String registrationNumber) {
            this.id = id;
            this.make = make;
            this.model = model;
            this.registrationNumber = registrationNumber;
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

        public String getRegistrationNumber() {
            return registrationNumber;
        }

        public void setRegistrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
        }
    }

    public static class ServiceSummary {
        private Long id;
        private String name;
        private ServiceCategory category;
        private BigDecimal basePrice;
        private Integer estimatedDurationMinutes;

        public ServiceSummary() {
        }

        public ServiceSummary(Long id, String name, ServiceCategory category, BigDecimal basePrice, Integer estimatedDurationMinutes) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.basePrice = basePrice;
            this.estimatedDurationMinutes = estimatedDurationMinutes;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ServiceCategory getCategory() {
            return category;
        }

        public void setCategory(ServiceCategory category) {
            this.category = category;
        }

        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
        }

        public Integer getEstimatedDurationMinutes() {
            return estimatedDurationMinutes;
        }

        public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
            this.estimatedDurationMinutes = estimatedDurationMinutes;
        }
    }
}

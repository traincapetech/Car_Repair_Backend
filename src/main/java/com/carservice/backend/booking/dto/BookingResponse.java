package com.carservice.backend.booking.dto;

import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.entity.BookingService;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.servicecatalog.enums.DiscountType;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BookingResponse {

    private Long id;
    private String bookingReference;
    private VehicleSummary vehicle;
    private List<BookingServiceItemResponse> services = new ArrayList<>();
    private BigDecimal totalAmount;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private String timeSlot;
    private BookingStatus status;
    private String customerNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;

    // Backward compatibility fields:
    private ServiceSummary service;
    private String serviceNameSnapshot;
    private BigDecimal servicePriceSnapshot;
    private BigDecimal estimatedPrice;
    private BigDecimal price;

    // Lifecycle and workshop tracking fields:
    private String serviceRequestReference;
    private Long serviceRequestId;
    private Long assignedWorkshopId;
    private String assignedWorkshopName;
    private String assignedWorkshopPhone;
    private String assignedWorkshopAddress;
    private String jobStatus;
    private String serviceRequestStatus;
    private Boolean isCancellable;

    public BookingResponse() {
    }

    public BookingResponse(
            Long id,
            String bookingReference,
            VehicleSummary vehicle,
            List<BookingServiceItemResponse> services,
            BigDecimal totalAmount,
            ServiceSummary service,
            String serviceNameSnapshot,
            BigDecimal servicePriceSnapshot,
            BigDecimal estimatedPrice,
            LocalDate bookingDate,
            LocalTime bookingTime,
            String timeSlot,
            BookingStatus status,
            String customerNotes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime cancelledAt
    ) {
        this.id = id;
        this.bookingReference = bookingReference;
        this.vehicle = vehicle;
        this.services = services != null ? services : new ArrayList<>();
        this.totalAmount = totalAmount;
        this.service = service;
        this.serviceNameSnapshot = serviceNameSnapshot;
        this.servicePriceSnapshot = servicePriceSnapshot;
        this.estimatedPrice = estimatedPrice;
        this.price = totalAmount != null ? totalAmount : (servicePriceSnapshot != null ? servicePriceSnapshot : estimatedPrice);
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.timeSlot = timeSlot;
        this.status = status;
        this.customerNotes = customerNotes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.cancelledAt = cancelledAt;
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

        List<BookingServiceItemResponse> servicesList = new ArrayList<>();
        if (booking.getBookingServices() != null && !booking.getBookingServices().isEmpty()) {
            for (BookingService bs : booking.getBookingServices()) {
                servicesList.add(new BookingServiceItemResponse(
                        bs.getServiceCatalog() != null ? bs.getServiceCatalog().getId() : null,
                        bs.getServiceNameSnapshot(),
                        bs.getBasePriceSnapshot(),
                        bs.getDiscountTypeSnapshot(),
                        bs.getDiscountValueSnapshot(),
                        bs.getFinalPriceSnapshot()
                ));
            }
        } else if (booking.getService() != null) {
            servicesList.add(new BookingServiceItemResponse(
                    booking.getService().getId(),
                    booking.getServiceNameSnapshot() != null ? booking.getServiceNameSnapshot() : booking.getService().getName(),
                    booking.getServicePriceSnapshot() != null ? booking.getServicePriceSnapshot() : booking.getService().getBasePrice(),
                    DiscountType.NO_DISCOUNT,
                    BigDecimal.ZERO,
                    booking.getServicePriceSnapshot() != null ? booking.getServicePriceSnapshot() : booking.getService().getBasePrice()
            ));
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
        } else if (booking.getBookingServices() != null && !booking.getBookingServices().isEmpty()) {
            BookingService first = booking.getBookingServices().get(0);
            if (first.getServiceCatalog() != null) {
                serviceSummary = new ServiceSummary(
                        first.getServiceCatalog().getId(),
                        first.getServiceNameSnapshot(),
                        first.getServiceCatalog().getCategory(),
                        first.getBasePriceSnapshot(),
                        first.getServiceCatalog().getEstimatedDurationMinutes()
                );
            }
        }

        BigDecimal totalAmountVal = booking.getTotalAmount() != null
                ? booking.getTotalAmount()
                : (booking.getServicePriceSnapshot() != null ? booking.getServicePriceSnapshot() : booking.getEstimatedPrice());

        String serviceNameSnapshotVal = booking.getServiceNameSnapshot();
        if (serviceNameSnapshotVal == null && !servicesList.isEmpty()) {
            serviceNameSnapshotVal = servicesList.get(0).getServiceName();
        }

        BigDecimal servicePriceSnapshotVal = booking.getServicePriceSnapshot();
        if (servicePriceSnapshotVal == null) {
            servicePriceSnapshotVal = totalAmountVal;
        }

        BookingResponse res = new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                vehicleSummary,
                servicesList,
                totalAmountVal,
                serviceSummary,
                serviceNameSnapshotVal,
                servicePriceSnapshotVal,
                totalAmountVal,
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getTimeSlot(),
                booking.getStatus(),
                booking.getCustomerNotes(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                booking.getCancelledAt()
        );

        if (booking.getServiceRequest() != null) {
            com.carservice.backend.marketplace.entity.ServiceRequest sr = booking.getServiceRequest();
            res.setServiceRequestId(sr.getId());
            res.setServiceRequestReference(sr.getRequestReference());
            if (sr.getStatus() != null) {
                res.setServiceRequestStatus(sr.getStatus().name());
            }
            if (sr.getAssignedWorkshop() != null) {
                res.setAssignedWorkshopId(sr.getAssignedWorkshop().getId());
                res.setAssignedWorkshopName(sr.getAssignedWorkshop().getBusinessName());
                res.setAssignedWorkshopPhone(sr.getAssignedWorkshop().getPhone());
                res.setAssignedWorkshopAddress(sr.getAssignedWorkshop().getAddress() + ", " + sr.getAssignedWorkshop().getCity());
            }
            if (sr.getCurrentJob() != null) {
                res.setJobStatus(sr.getCurrentJob().getStatus().name());
            }
        } else if (booking.getServiceRequestReference() != null) {
            res.setServiceRequestReference(booking.getServiceRequestReference());
        }

        // Determine if cancellable
        boolean cancellable = true;
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            cancellable = false;
        } else if (booking.getServiceRequest() != null && booking.getServiceRequest().getCurrentJob() != null) {
            cancellable = !booking.getServiceRequest().getCurrentJob().isPhysicalWorkStarted();
        }
        res.setIsCancellable(cancellable);

        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public VehicleSummary getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleSummary vehicle) {
        this.vehicle = vehicle;
    }

    public List<BookingServiceItemResponse> getServices() {
        return services;
    }

    public void setServices(List<BookingServiceItemResponse> services) {
        this.services = services;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ServiceSummary getService() {
        return service;
    }

    public void setService(ServiceSummary service) {
        this.service = service;
    }

    public String getServiceNameSnapshot() {
        return serviceNameSnapshot;
    }

    public void setServiceNameSnapshot(String serviceNameSnapshot) {
        this.serviceNameSnapshot = serviceNameSnapshot;
    }

    public BigDecimal getServicePriceSnapshot() {
        return servicePriceSnapshot;
    }

    public void setServicePriceSnapshot(BigDecimal servicePriceSnapshot) {
        this.servicePriceSnapshot = servicePriceSnapshot;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public BigDecimal getPrice() {
        return servicePriceSnapshot != null ? servicePriceSnapshot : estimatedPrice;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getServiceRequestReference() {
        return serviceRequestReference;
    }

    public void setServiceRequestReference(String serviceRequestReference) {
        this.serviceRequestReference = serviceRequestReference;
    }

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Long serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public Long getAssignedWorkshopId() {
        return assignedWorkshopId;
    }

    public void setAssignedWorkshopId(Long assignedWorkshopId) {
        this.assignedWorkshopId = assignedWorkshopId;
    }

    public String getAssignedWorkshopName() {
        return assignedWorkshopName;
    }

    public void setAssignedWorkshopName(String assignedWorkshopName) {
        this.assignedWorkshopName = assignedWorkshopName;
    }

    public String getAssignedWorkshopPhone() {
        return assignedWorkshopPhone;
    }

    public void setAssignedWorkshopPhone(String assignedWorkshopPhone) {
        this.assignedWorkshopPhone = assignedWorkshopPhone;
    }

    public String getAssignedWorkshopAddress() {
        return assignedWorkshopAddress;
    }

    public void setAssignedWorkshopAddress(String assignedWorkshopAddress) {
        this.assignedWorkshopAddress = assignedWorkshopAddress;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getServiceRequestStatus() {
        return serviceRequestStatus;
    }

    public void setServiceRequestStatus(String serviceRequestStatus) {
        this.serviceRequestStatus = serviceRequestStatus;
    }

    public Boolean getIsCancellable() {
        return isCancellable;
    }

    public void setIsCancellable(Boolean cancellable) {
        isCancellable = cancellable;
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

    public static class BookingServiceItemResponse {
        private Long serviceId;
        private String serviceName;
        private BigDecimal basePrice;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal finalPrice;

        public BookingServiceItemResponse() {
        }

        public BookingServiceItemResponse(
                Long serviceId,
                String serviceName,
                BigDecimal basePrice,
                DiscountType discountType,
                BigDecimal discountValue,
                BigDecimal finalPrice
        ) {
            this.serviceId = serviceId;
            this.serviceName = serviceName;
            this.basePrice = basePrice;
            this.discountType = discountType != null ? discountType : DiscountType.NO_DISCOUNT;
            this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
            this.finalPrice = finalPrice != null ? finalPrice : basePrice;
        }

        public Long getServiceId() {
            return serviceId;
        }

        public void setServiceId(Long serviceId) {
            this.serviceId = serviceId;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
        }

        public DiscountType getDiscountType() {
            return discountType;
        }

        public void setDiscountType(DiscountType discountType) {
            this.discountType = discountType;
        }

        public BigDecimal getDiscountValue() {
            return discountValue;
        }

        public void setDiscountValue(BigDecimal discountValue) {
            this.discountValue = discountValue;
        }

        public BigDecimal getFinalPrice() {
            return finalPrice;
        }

        public void setFinalPrice(BigDecimal finalPrice) {
            this.finalPrice = finalPrice;
        }
    }
}

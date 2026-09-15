package com.carservice.backend.booking.entity;

import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.enums.BookingTimeSlot;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.vehicle.entity.Vehicle;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_booking_reference", columnList = "booking_reference"),
                @Index(name = "idx_bookings_user_id", columnList = "user_id"),
                @Index(name = "idx_bookings_vehicle_id", columnList = "vehicle_id"),
                @Index(name = "idx_bookings_service_id", columnList = "service_id"),
                @Index(name = "idx_bookings_booking_date", columnList = "booking_date"),
                @Index(name = "idx_bookings_status", columnList = "status")
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", length = 32, unique = true)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingService> bookingServices = new ArrayList<>();

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = true)
    private ServiceCatalog service;

    @Column(name = "service_name_snapshot")
    private String serviceNameSnapshot;

    @Column(name = "service_price_snapshot", precision = 10, scale = 2)
    private BigDecimal servicePriceSnapshot;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "booking_time", nullable = false)
    private LocalTime bookingTime;

    @Column(name = "time_slot", length = 50)
    private String timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "customer_notes", length = 1000)
    private String customerNotes;

    @Column(name = "estimated_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "service_request_reference", length = 32)
    private String serviceRequestReference;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private com.carservice.backend.marketplace.entity.ServiceRequest serviceRequest;

    public Booking() {
    }

    public Booking(
            User user,
            Vehicle vehicle,
            ServiceCatalog service,
            LocalDate bookingDate,
            LocalTime bookingTime,
            BookingStatus status,
            String customerNotes,
            BigDecimal estimatedPrice
    ) {
        this.user = user;
        this.vehicle = vehicle;
        this.service = service;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.status = status;
        this.customerNotes = customerNotes;
        this.estimatedPrice = estimatedPrice != null ? estimatedPrice.setScale(2, RoundingMode.HALF_UP) : null;
        this.servicePriceSnapshot = this.estimatedPrice;
        this.totalAmount = this.estimatedPrice;
        if (service != null) {
            this.serviceNameSnapshot = service.getName();
        }
        if (bookingTime != null) {
            this.timeSlot = BookingTimeSlot.fromBookingTime(bookingTime).getSlot();
        }
    }

    public void addBookingService(BookingService bookingService) {
        if (this.bookingServices == null) {
            this.bookingServices = new ArrayList<>();
        }
        this.bookingServices.add(bookingService);
        bookingService.setBooking(this);
    }

    public void removeBookingService(BookingService bookingService) {
        if (this.bookingServices != null) {
            this.bookingServices.remove(bookingService);
            bookingService.setBooking(null);
        }
    }

    public BigDecimal calculateTotalAmount() {
        if (this.bookingServices == null || this.bookingServices.isEmpty()) {
            return this.totalAmount != null ? this.totalAmount : (this.estimatedPrice != null ? this.estimatedPrice : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BookingService item : this.bookingServices) {
            if (item.getFinalPriceSnapshot() != null) {
                sum = sum.add(item.getFinalPriceSnapshot());
            } else if (item.getBasePriceSnapshot() != null) {
                sum = sum.add(item.getBasePriceSnapshot());
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = BookingStatus.PENDING;
        }
        if (this.bookingReference == null) {
            this.bookingReference = generateBookingReference(this.bookingDate != null ? this.bookingDate : LocalDate.now());
        }
        if (this.totalAmount == null) {
            this.totalAmount = calculateTotalAmount();
        }
        if (this.estimatedPrice == null) {
            this.estimatedPrice = this.totalAmount != null ? this.totalAmount : (this.servicePriceSnapshot != null ? this.servicePriceSnapshot : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        if (this.servicePriceSnapshot == null) {
            this.servicePriceSnapshot = this.totalAmount;
        }
        if (this.serviceNameSnapshot == null && this.service != null) {
            this.serviceNameSnapshot = this.service.getName();
        }
        if (this.timeSlot == null && this.bookingTime != null) {
            this.timeSlot = BookingTimeSlot.fromBookingTime(this.bookingTime).getSlot();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == BookingStatus.CANCELLED && this.cancelledAt == null) {
            this.cancelledAt = LocalDateTime.now();
        }
    }

    public static String generateBookingReference(LocalDate date) {
        String dateStr = date != null ? date.format(DateTimeFormatter.BASIC_ISO_DATE) : LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int randomNum = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "CSB-" + dateStr + "-" + randomNum;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public ServiceCatalog getService() {
        return service;
    }

    public void setService(ServiceCatalog service) {
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
        if (this.estimatedPrice == null) {
            this.estimatedPrice = servicePriceSnapshot;
        }
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
        if (status == BookingStatus.CANCELLED && this.cancelledAt == null) {
            this.cancelledAt = LocalDateTime.now();
        }
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice != null ? estimatedPrice : servicePriceSnapshot;
    }

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
        if (this.servicePriceSnapshot == null) {
            this.servicePriceSnapshot = estimatedPrice;
        }
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

    public List<BookingService> getBookingServices() {
        return bookingServices;
    }

    public void setBookingServices(List<BookingService> bookingServices) {
        this.bookingServices = bookingServices;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount != null ? totalAmount : (estimatedPrice != null ? estimatedPrice : servicePriceSnapshot);
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : null;
        if (this.estimatedPrice == null) {
            this.estimatedPrice = this.totalAmount;
        }
        if (this.servicePriceSnapshot == null) {
            this.servicePriceSnapshot = this.totalAmount;
        }
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
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

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getServiceRequestReference() {
        return serviceRequestReference;
    }

    public void setServiceRequestReference(String serviceRequestReference) {
        this.serviceRequestReference = serviceRequestReference;
    }

    public com.carservice.backend.marketplace.entity.ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(com.carservice.backend.marketplace.entity.ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
        if (serviceRequest != null && (this.serviceRequestReference == null || this.serviceRequestReference.isBlank())) {
            this.serviceRequestReference = serviceRequest.getRequestReference();
        }
    }
}

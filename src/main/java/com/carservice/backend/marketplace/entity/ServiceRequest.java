package com.carservice.backend.marketplace.entity;

import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.vehicle.entity.Vehicle;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(
        name = "service_requests",
        indexes = {
                @Index(name = "idx_sr_reference", columnList = "request_reference"),
                @Index(name = "idx_sr_user_id", columnList = "user_id"),
                @Index(name = "idx_sr_city", columnList = "city"),
                @Index(name = "idx_sr_status", columnList = "status"),
                @Index(name = "idx_sr_preferred_date", columnList = "preferred_date")
        }
)
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_reference", length = 32, unique = true, nullable = false)
    private String requestReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @Column(name = "preferred_time_slot", nullable = false, length = 50)
    private String preferredTimeSlot;

    @Column(name = "customer_notes", length = 1000)
    private String customerNotes;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceRequestStatus status = ServiceRequestStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_workshop_id")
    private Workshop assignedWorkshop;

    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ServiceRequestItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LeadOpportunity> opportunities = new ArrayList<>();

    @Column(name = "booking_reference", length = 32)
    private String bookingReference;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private com.carservice.backend.booking.entity.Booking booking;

    @OneToOne(mappedBy = "serviceRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private WorkshopJob currentJob;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ServiceRequest() {
    }

    public ServiceRequest(
            User user,
            Vehicle vehicle,
            String city,
            String address,
            String pincode,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDate preferredDate,
            String preferredTimeSlot,
            String customerNotes
    ) {
        this.user = user;
        this.vehicle = vehicle;
        this.city = city;
        this.address = address;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.preferredDate = preferredDate;
        this.preferredTimeSlot = preferredTimeSlot;
        this.customerNotes = customerNotes;
        this.status = ServiceRequestStatus.SUBMITTED;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.requestReference == null) {
            this.requestReference = generateRequestReference();
        }
        if (this.status == null) {
            this.status = ServiceRequestStatus.SUBMITTED;
        }
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static String generateRequestReference() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "SR-" + datePart + "-" + randomPart;
    }

    public void addItem(ServiceRequestItem item) {
        this.items.add(item);
        item.setServiceRequest(this);
    }

    public void calculateTotalAmount() {
        this.totalAmount = this.items.stream()
                .map(ServiceRequestItem::getFinalPriceSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestReference() {
        return requestReference;
    }

    public void setRequestReference(String requestReference) {
        this.requestReference = requestReference;
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

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }

    public String getPreferredTimeSlot() {
        return preferredTimeSlot;
    }

    public void setPreferredTimeSlot(String preferredTimeSlot) {
        this.preferredTimeSlot = preferredTimeSlot;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ServiceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceRequestStatus status) {
        this.status = status;
    }

    public Workshop getAssignedWorkshop() {
        return assignedWorkshop;
    }

    public void setAssignedWorkshop(Workshop assignedWorkshop) {
        this.assignedWorkshop = assignedWorkshop;
    }

    public List<ServiceRequestItem> getItems() {
        return items;
    }

    public void setItems(List<ServiceRequestItem> items) {
        this.items = items;
    }

    public List<LeadOpportunity> getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(List<LeadOpportunity> opportunities) {
        this.opportunities = opportunities;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public com.carservice.backend.booking.entity.Booking getBooking() {
        return booking;
    }

    public void setBooking(com.carservice.backend.booking.entity.Booking booking) {
        this.booking = booking;
        if (booking != null && (this.bookingReference == null || this.bookingReference.isBlank())) {
            this.bookingReference = booking.getBookingReference();
        }
    }

    public WorkshopJob getCurrentJob() {
        return currentJob;
    }

    public void setCurrentJob(WorkshopJob currentJob) {
        this.currentJob = currentJob;
    }
}

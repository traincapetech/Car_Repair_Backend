package com.carservice.backend.marketplace.entity;

import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "workshops",
        indexes = {
                @Index(name = "idx_workshops_city", columnList = "city"),
                @Index(name = "idx_workshops_pincode", columnList = "pincode"),
                @Index(name = "idx_workshops_verification_status", columnList = "verification_status"),
                @Index(name = "idx_workshops_is_active", columnList = "is_active")
        }
)
public class Workshop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "service_radius_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal serviceRadiusKm = new BigDecimal("15.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private WorkshopVerificationStatus verificationStatus = WorkshopVerificationStatus.PENDING;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkshopService> supportedServices = new ArrayList<>();

    @OneToOne(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private WorkshopWallet wallet;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Workshop() {
    }

    public Workshop(
            User user,
            String businessName,
            String phone,
            String email,
            String address,
            String city,
            String state,
            String pincode,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal serviceRadiusKm,
            WorkshopVerificationStatus verificationStatus,
            Boolean isActive
    ) {
        this.user = user;
        this.businessName = businessName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.serviceRadiusKm = serviceRadiusKm != null ? serviceRadiusKm.setScale(2, RoundingMode.HALF_UP) : new BigDecimal("15.00");
        this.verificationStatus = verificationStatus != null ? verificationStatus : WorkshopVerificationStatus.PENDING;
        this.isActive = isActive != null ? isActive : true;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.serviceRadiusKm == null) {
            this.serviceRadiusKm = new BigDecimal("15.00");
        }
        if (this.verificationStatus == null) {
            this.verificationStatus = WorkshopVerificationStatus.PENDING;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addSupportedService(WorkshopService service) {
        this.supportedServices.add(service);
        service.setWorkshop(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public BigDecimal getServiceRadiusKm() {
        return serviceRadiusKm;
    }

    public void setServiceRadiusKm(BigDecimal serviceRadiusKm) {
        this.serviceRadiusKm = serviceRadiusKm;
    }

    public WorkshopVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(WorkshopVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public List<WorkshopService> getSupportedServices() {
        return supportedServices;
    }

    public void setSupportedServices(List<WorkshopService> supportedServices) {
        this.supportedServices = supportedServices;
    }

    public WorkshopWallet getWallet() {
        return wallet;
    }

    public void setWallet(WorkshopWallet wallet) {
        this.wallet = wallet;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

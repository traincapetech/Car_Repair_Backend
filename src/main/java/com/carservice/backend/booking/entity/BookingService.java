package com.carservice.backend.booking.entity;

import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.enums.DiscountType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "booking_services",
        indexes = {
                @Index(name = "idx_booking_services_booking_id", columnList = "booking_id"),
                @Index(name = "idx_booking_services_service_id", columnList = "service_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_booking_services_booking_service", columnNames = {"booking_id", "service_id"})
        }
)
public class BookingService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceCatalog serviceCatalog;

    @Column(name = "service_name_snapshot", nullable = false)
    private String serviceNameSnapshot;

    @Column(name = "base_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePriceSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type_snapshot", nullable = false, length = 30)
    private DiscountType discountTypeSnapshot = DiscountType.NO_DISCOUNT;

    @Column(name = "discount_value_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValueSnapshot = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "final_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalPriceSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BookingService() {
    }

    public BookingService(
            Booking booking,
            ServiceCatalog serviceCatalog,
            String serviceNameSnapshot,
            BigDecimal basePriceSnapshot,
            DiscountType discountTypeSnapshot,
            BigDecimal discountValueSnapshot,
            BigDecimal finalPriceSnapshot
    ) {
        this.booking = booking;
        this.serviceCatalog = serviceCatalog;
        this.serviceNameSnapshot = serviceNameSnapshot;
        this.basePriceSnapshot = basePriceSnapshot != null ? basePriceSnapshot.setScale(2, RoundingMode.HALF_UP) : null;
        this.discountTypeSnapshot = discountTypeSnapshot != null ? discountTypeSnapshot : DiscountType.NO_DISCOUNT;
        this.discountValueSnapshot = discountValueSnapshot != null ? discountValueSnapshot.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.finalPriceSnapshot = finalPriceSnapshot != null ? finalPriceSnapshot.setScale(2, RoundingMode.HALF_UP) : this.basePriceSnapshot;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.discountTypeSnapshot == null) {
            this.discountTypeSnapshot = DiscountType.NO_DISCOUNT;
        }
        if (this.discountValueSnapshot == null) {
            this.discountValueSnapshot = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.finalPriceSnapshot == null && this.basePriceSnapshot != null) {
            this.finalPriceSnapshot = this.basePriceSnapshot.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public ServiceCatalog getServiceCatalog() {
        return serviceCatalog;
    }

    public void setServiceCatalog(ServiceCatalog serviceCatalog) {
        this.serviceCatalog = serviceCatalog;
    }

    public String getServiceNameSnapshot() {
        return serviceNameSnapshot;
    }

    public void setServiceNameSnapshot(String serviceNameSnapshot) {
        this.serviceNameSnapshot = serviceNameSnapshot;
    }

    public BigDecimal getBasePriceSnapshot() {
        return basePriceSnapshot;
    }

    public void setBasePriceSnapshot(BigDecimal basePriceSnapshot) {
        this.basePriceSnapshot = basePriceSnapshot != null ? basePriceSnapshot.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public DiscountType getDiscountTypeSnapshot() {
        return discountTypeSnapshot;
    }

    public void setDiscountTypeSnapshot(DiscountType discountTypeSnapshot) {
        this.discountTypeSnapshot = discountTypeSnapshot != null ? discountTypeSnapshot : DiscountType.NO_DISCOUNT;
    }

    public BigDecimal getDiscountValueSnapshot() {
        return discountValueSnapshot;
    }

    public void setDiscountValueSnapshot(BigDecimal discountValueSnapshot) {
        this.discountValueSnapshot = discountValueSnapshot != null ? discountValueSnapshot.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getFinalPriceSnapshot() {
        return finalPriceSnapshot;
    }

    public void setFinalPriceSnapshot(BigDecimal finalPriceSnapshot) {
        this.finalPriceSnapshot = finalPriceSnapshot != null ? finalPriceSnapshot.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

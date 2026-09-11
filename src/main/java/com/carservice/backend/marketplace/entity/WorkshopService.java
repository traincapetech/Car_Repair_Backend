package com.carservice.backend.marketplace.entity;

import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "workshop_services",
        indexes = {
                @Index(name = "idx_ws_workshop_id", columnList = "workshop_id"),
                @Index(name = "idx_ws_service_id", columnList = "service_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_workshop_service", columnNames = {"workshop_id", "service_id"})
        }
)
public class WorkshopService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceCatalog serviceCatalog;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WorkshopService() {
    }

    public WorkshopService(Workshop workshop, ServiceCatalog serviceCatalog, Boolean isActive) {
        this.workshop = workshop;
        this.serviceCatalog = serviceCatalog;
        this.isActive = isActive != null ? isActive : true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public ServiceCatalog getServiceCatalog() {
        return serviceCatalog;
    }

    public void setServiceCatalog(ServiceCatalog serviceCatalog) {
        this.serviceCatalog = serviceCatalog;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

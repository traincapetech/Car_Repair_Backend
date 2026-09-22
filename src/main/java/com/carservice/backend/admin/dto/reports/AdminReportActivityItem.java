package com.carservice.backend.admin.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminReportActivityItem {

    private Long id;
    private LocalDateTime date;
    private String type; // "BOOKING", "SERVICE_REQUEST", "WORKSHOP"
    private String reference;
    private String customerName;
    private String workshopName;
    private String serviceName;
    private String status;
    private BigDecimal amount;

    public AdminReportActivityItem() {}

    public AdminReportActivityItem(
            Long id,
            LocalDateTime date,
            String type,
            String reference,
            String customerName,
            String workshopName,
            String serviceName,
            String status,
            BigDecimal amount
    ) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.reference = reference;
        this.customerName = customerName;
        this.workshopName = workshopName;
        this.serviceName = serviceName;
        this.status = status;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getWorkshopName() {
        return workshopName;
    }

    public void setWorkshopName(String workshopName) {
        this.workshopName = workshopName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

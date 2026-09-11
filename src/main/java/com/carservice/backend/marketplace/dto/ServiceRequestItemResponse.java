package com.carservice.backend.marketplace.dto;

import com.carservice.backend.servicecatalog.enums.DiscountType;

import java.math.BigDecimal;

public class ServiceRequestItemResponse {

    private Long id;
    private Long serviceId;
    private String serviceNameSnapshot;
    private BigDecimal basePriceSnapshot;
    private DiscountType discountTypeSnapshot;
    private BigDecimal discountValueSnapshot;
    private BigDecimal finalPriceSnapshot;

    public ServiceRequestItemResponse() {
    }

    public ServiceRequestItemResponse(
            Long id,
            Long serviceId,
            String serviceNameSnapshot,
            BigDecimal basePriceSnapshot,
            DiscountType discountTypeSnapshot,
            BigDecimal discountValueSnapshot,
            BigDecimal finalPriceSnapshot
    ) {
        this.id = id;
        this.serviceId = serviceId;
        this.serviceNameSnapshot = serviceNameSnapshot;
        this.basePriceSnapshot = basePriceSnapshot;
        this.discountTypeSnapshot = discountTypeSnapshot;
        this.discountValueSnapshot = discountValueSnapshot;
        this.finalPriceSnapshot = finalPriceSnapshot;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
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
        this.basePriceSnapshot = basePriceSnapshot;
    }

    public DiscountType getDiscountTypeSnapshot() {
        return discountTypeSnapshot;
    }

    public void setDiscountTypeSnapshot(DiscountType discountTypeSnapshot) {
        this.discountTypeSnapshot = discountTypeSnapshot;
    }

    public BigDecimal getDiscountValueSnapshot() {
        return discountValueSnapshot;
    }

    public void setDiscountValueSnapshot(BigDecimal discountValueSnapshot) {
        this.discountValueSnapshot = discountValueSnapshot;
    }

    public BigDecimal getFinalPriceSnapshot() {
        return finalPriceSnapshot;
    }

    public void setFinalPriceSnapshot(BigDecimal finalPriceSnapshot) {
        this.finalPriceSnapshot = finalPriceSnapshot;
    }
}

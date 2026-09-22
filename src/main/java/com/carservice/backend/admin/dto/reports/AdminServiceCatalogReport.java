package com.carservice.backend.admin.dto.reports;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AdminServiceCatalogReport {

    private long totalServiceCatalogItems;
    private long activeServices;
    private long inactiveServices;
    private List<TopServiceItem> mostRequestedServices = new ArrayList<>();
    private List<TopServiceItem> mostBookedServices = new ArrayList<>();

    public AdminServiceCatalogReport() {}

    public static class TopServiceItem {
        private String serviceName;
        private String category;
        private long count;
        private BigDecimal totalRevenue;

        public TopServiceItem() {}

        public TopServiceItem(String serviceName, String category, long count, BigDecimal totalRevenue) {
            this.serviceName = serviceName;
            this.category = category;
            this.count = count;
            this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }
    }

    public long getTotalServiceCatalogItems() {
        return totalServiceCatalogItems;
    }

    public void setTotalServiceCatalogItems(long totalServiceCatalogItems) {
        this.totalServiceCatalogItems = totalServiceCatalogItems;
    }

    public long getActiveServices() {
        return activeServices;
    }

    public void setActiveServices(long activeServices) {
        this.activeServices = activeServices;
    }

    public long getInactiveServices() {
        return inactiveServices;
    }

    public void setInactiveServices(long inactiveServices) {
        this.inactiveServices = inactiveServices;
    }

    public List<TopServiceItem> getMostRequestedServices() {
        return mostRequestedServices;
    }

    public void setMostRequestedServices(List<TopServiceItem> mostRequestedServices) {
        this.mostRequestedServices = mostRequestedServices;
    }

    public List<TopServiceItem> getMostBookedServices() {
        return mostBookedServices;
    }

    public void setMostBookedServices(List<TopServiceItem> mostBookedServices) {
        this.mostBookedServices = mostBookedServices;
    }
}

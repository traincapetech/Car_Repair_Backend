package com.carservice.backend.admin.dto.reports;

import java.util.ArrayList;
import java.util.List;

public class AdminWorkshopReport {

    private long totalWorkshops;
    private long pendingApproval;
    private long verifiedWorkshops;
    private long rejectedWorkshops;
    private long activeWorkshops;
    private long inactiveWorkshops;
    private List<AdminReportTimelinePoint> timeline = new ArrayList<>();
    private boolean hasGeoCoordinates;
    private String geoNotice;
    private List<CityDistributionItem> cityDistribution = new ArrayList<>();

    public AdminWorkshopReport() {
        this.geoNotice = "Workshop location analytics will be available after workshop geo-registration is enabled.";
    }

    public static class CityDistributionItem {
        private String city;
        private String state;
        private long count;

        public CityDistributionItem() {}

        public CityDistributionItem(String city, String state, long count) {
            this.city = city;
            this.state = state;
            this.count = count;
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

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public long getTotalWorkshops() {
        return totalWorkshops;
    }

    public void setTotalWorkshops(long totalWorkshops) {
        this.totalWorkshops = totalWorkshops;
    }

    public long getPendingApproval() {
        return pendingApproval;
    }

    public void setPendingApproval(long pendingApproval) {
        this.pendingApproval = pendingApproval;
    }

    public long getVerifiedWorkshops() {
        return verifiedWorkshops;
    }

    public void setVerifiedWorkshops(long verifiedWorkshops) {
        this.verifiedWorkshops = verifiedWorkshops;
    }

    public long getRejectedWorkshops() {
        return rejectedWorkshops;
    }

    public void setRejectedWorkshops(long rejectedWorkshops) {
        this.rejectedWorkshops = rejectedWorkshops;
    }

    public long getActiveWorkshops() {
        return activeWorkshops;
    }

    public void setActiveWorkshops(long activeWorkshops) {
        this.activeWorkshops = activeWorkshops;
    }

    public long getInactiveWorkshops() {
        return inactiveWorkshops;
    }

    public void setInactiveWorkshops(long inactiveWorkshops) {
        this.inactiveWorkshops = inactiveWorkshops;
    }

    public List<AdminReportTimelinePoint> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<AdminReportTimelinePoint> timeline) {
        this.timeline = timeline;
    }

    public boolean isHasGeoCoordinates() {
        return hasGeoCoordinates;
    }

    public void setHasGeoCoordinates(boolean hasGeoCoordinates) {
        this.hasGeoCoordinates = hasGeoCoordinates;
    }

    public String getGeoNotice() {
        return geoNotice;
    }

    public void setGeoNotice(String geoNotice) {
        this.geoNotice = geoNotice;
    }

    public List<CityDistributionItem> getCityDistribution() {
        return cityDistribution;
    }

    public void setCityDistribution(List<CityDistributionItem> cityDistribution) {
        this.cityDistribution = cityDistribution;
    }
}

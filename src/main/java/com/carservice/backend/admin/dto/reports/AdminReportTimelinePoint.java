package com.carservice.backend.admin.dto.reports;

import java.math.BigDecimal;

public class AdminReportTimelinePoint {
    private String date; // YYYY-MM-DD
    private long count;
    private BigDecimal amount;

    public AdminReportTimelinePoint() {}

    public AdminReportTimelinePoint(String date, long count) {
        this.date = date;
        this.count = count;
        this.amount = BigDecimal.ZERO;
    }

    public AdminReportTimelinePoint(String date, long count, BigDecimal amount) {
        this.date = date;
        this.count = count;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

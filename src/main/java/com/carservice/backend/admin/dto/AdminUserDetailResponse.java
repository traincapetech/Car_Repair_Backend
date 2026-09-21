package com.carservice.backend.admin.dto;

import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AdminUserDetailResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> permissions;

    private WorkshopSummary workshop;
    private CustomerSummary customer;

    public AdminUserDetailResponse() {
    }

    public static class WorkshopSummary {
        private Long id;
        private String businessName;
        private WorkshopVerificationStatus verificationStatus;
        private Boolean isActive;
        private String city;
        private String state;
        private BigDecimal rating;
        private Integer totalReviews;

        public WorkshopSummary() {
        }

        public WorkshopSummary(Long id, String businessName, WorkshopVerificationStatus verificationStatus, Boolean isActive, String city, String state, BigDecimal rating, Integer totalReviews) {
            this.id = id;
            this.businessName = businessName;
            this.verificationStatus = verificationStatus;
            this.isActive = isActive;
            this.city = city;
            this.state = state;
            this.rating = rating;
            this.totalReviews = totalReviews;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getBusinessName() {
            return businessName;
        }

        public void setBusinessName(String businessName) {
            this.businessName = businessName;
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

        public BigDecimal getRating() {
            return rating;
        }

        public void setRating(BigDecimal rating) {
            this.rating = rating;
        }

        public Integer getTotalReviews() {
            return totalReviews;
        }

        public void setTotalReviews(Integer totalReviews) {
            this.totalReviews = totalReviews;
        }
    }

    public static class CustomerSummary {
        private int vehiclesCount;
        private int bookingsCount;
        private int requestsCount;

        public CustomerSummary() {
        }

        public CustomerSummary(int vehiclesCount, int bookingsCount, int requestsCount) {
            this.vehiclesCount = vehiclesCount;
            this.bookingsCount = bookingsCount;
            this.requestsCount = requestsCount;
        }

        public int getVehiclesCount() {
            return vehiclesCount;
        }

        public void setVehiclesCount(int vehiclesCount) {
            this.vehiclesCount = vehiclesCount;
        }

        public int getBookingsCount() {
            return bookingsCount;
        }

        public void setBookingsCount(int bookingsCount) {
            this.bookingsCount = bookingsCount;
        }

        public int getRequestsCount() {
            return requestsCount;
        }

        public void setRequestsCount(int requestsCount) {
            this.requestsCount = requestsCount;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public WorkshopSummary getWorkshop() {
        return workshop;
    }

    public void setWorkshop(WorkshopSummary workshop) {
        this.workshop = workshop;
    }

    public CustomerSummary getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerSummary customer) {
        this.customer = customer;
    }
}

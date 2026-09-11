package com.carservice.backend.marketplace.dto;

public class CustomerProfileDto {

    private String name;
    private String phone;
    private String email;
    private String address;
    private boolean masked;

    public CustomerProfileDto() {
    }

    public CustomerProfileDto(String name, String phone, String email, String address, boolean masked) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.masked = masked;
    }

    public static CustomerProfileDto masked(String city, String pincode) {
        return new CustomerProfileDto(
                "********",
                "**********",
                "********",
                "********, " + (city != null ? city : "") + " " + (pincode != null ? pincode : ""),
                true
        );
    }

    public static CustomerProfileDto full(String name, String phone, String email, String address, String city, String pincode) {
        String fullAddress = (address != null ? address : "") + ", " + (city != null ? city : "") + " - " + (pincode != null ? pincode : "");
        return new CustomerProfileDto(
                name != null ? name : "N/A",
                phone != null ? phone : "N/A",
                email != null ? email : "N/A",
                fullAddress.trim(),
                false
        );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isMasked() {
        return masked;
    }

    public void setMasked(boolean masked) {
        this.masked = masked;
    }
}

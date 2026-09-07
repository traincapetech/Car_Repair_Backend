package com.carservice.backend.user.dto;

public class DeactivateAccountRequest {

    private String password;

    public DeactivateAccountRequest() {
    }

    public DeactivateAccountRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

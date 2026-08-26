package com.carservice.backend.user.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.dto.CustomerRegistrationRequest;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.service.UserService;
import com.carservice.backend.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.carservice.backend.user.dto.LoginRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<UserResponse>> registerCustomer(
            @Valid @RequestBody CustomerRegistrationRequest request) {

        UserResponse user = userService.registerCustomer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(
                                true,
                                "Customer registered successfully",
                                user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        UserResponse user = userService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful",
                        user));
    }
}
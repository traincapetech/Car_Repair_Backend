package com.carservice.backend.user.controller;

import com.carservice.backend.common.exception.InvalidCredentialsException;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.dto.CustomerRegistrationRequest;
import com.carservice.backend.user.service.UserService;
import com.carservice.backend.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.carservice.backend.user.dto.LoginRequest;
import com.carservice.backend.user.dto.LoginResponse;
import com.carservice.backend.user.dto.RefreshTokenRequest;
import com.carservice.backend.user.dto.TokenResponse;
import jakarta.validation.Valid;
import com.carservice.backend.user.entity.User;
import org.springframework.security.core.Authentication;
import com.carservice.backend.user.dto.UpdateProfileRequest;


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
        public ResponseEntity<ApiResponse<LoginResponse>> login(
                        @Valid @RequestBody LoginRequest request) {

                LoginResponse loginResponse = userService.login(request);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Login successful",
                                                loginResponse));
        }

        @PostMapping("/refresh")
        public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
                        @Valid @RequestBody RefreshTokenRequest request) {

                TokenResponse response = userService.refreshToken(request);

                return ResponseEntity.ok(
                                new ApiResponse<>(
                                                true,
                                                "Token refreshed successfully",
                                                response));
        }

        @GetMapping("/me")
public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
        Authentication authentication) {

    User currentUser = (User) authentication.getPrincipal();

    UserResponse response = new UserResponse(
            currentUser.getId(),
            currentUser.getName(),
            currentUser.getEmail(),
            currentUser.getPhone(),
            currentUser.getRole(),
            currentUser.getIsActive(),
            currentUser.getCreatedAt()
    );

    return ResponseEntity.ok(
            ApiResponse.success(
                    "Authenticated user",
                    response
            )
    );
}
       @PutMapping("/me")
public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
        Authentication authentication,
        @Valid @RequestBody UpdateProfileRequest request) {

    if (authentication == null || !authentication.isAuthenticated()) {
        throw new InvalidCredentialsException("Authentication required");
    }

    User currentUser = (User) authentication.getPrincipal();

    UserResponse response = userService.updateProfile(
            currentUser,
            request
    );

    return ResponseEntity.ok(
            ApiResponse.success(
                    "Profile updated successfully",
                    response
            )
    );
}
}
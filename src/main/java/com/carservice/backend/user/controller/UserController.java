package com.carservice.backend.user.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.dto.CustomerRegistrationRequest;
import com.carservice.backend.user.dto.LoginRequest;
import com.carservice.backend.user.dto.LoginResponse;
import com.carservice.backend.user.dto.LogoutRequest;
import com.carservice.backend.user.dto.RefreshTokenRequest;
import com.carservice.backend.user.dto.ResetPasswordRequest;
import com.carservice.backend.user.dto.TokenResponse;
import com.carservice.backend.user.dto.UpdateProfileRequest;
import com.carservice.backend.user.dto.UserResponse;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.carservice.backend.user.dto.ChangePasswordRequest;
import com.carservice.backend.user.dto.DeactivateAccountRequest;
import com.carservice.backend.user.dto.ForgotPasswordRequest;
import com.carservice.backend.user.dto.ResetPasswordRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class UserController {

        private final UserService userService;

        public UserController(
                        UserService userService) {
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

        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(
                        @Valid @RequestBody LogoutRequest request) {

                userService.logout(request);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Logout successful",
                                                null));
        }

        @GetMapping("/me")
        public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
                        Authentication authentication) {

                User user = (User) authentication.getPrincipal();

                UserResponse response = new UserResponse(
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getPhone(),
                                user.getRole(),
                                user.getIsActive(),
                                user.getCreatedAt());

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Authenticated user",
                                                response));
        }

        @PutMapping("/me")
        public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
                        Authentication authentication,
                        @Valid @RequestBody UpdateProfileRequest request) {

                User currentUser = (User) authentication.getPrincipal();

                UserResponse response = userService.updateProfile(
                                currentUser,
                                request);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Profile updated successfully",
                                                response));
        }

        @PutMapping("/change-password")
        public ResponseEntity<ApiResponse<Void>> changePassword(
                        Authentication authentication,
                        @Valid @RequestBody ChangePasswordRequest request) {

                User currentUser = (User) authentication.getPrincipal();

                userService.changePassword(
                                currentUser,
                                request);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Password changed successfully",
                                                null));
        }

        @PostMapping("/forgot-password")
        public ResponseEntity<ApiResponse<String>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequest request) {

                String resetToken = userService.forgotPassword(
                                request.getEmail());

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "If the account exists, a password reset request has been created.",
                                                resetToken));
        }

        @PostMapping("/reset-password")
        public ResponseEntity<ApiResponse<String>> resetPassword(
                        @Valid @RequestBody ResetPasswordRequest request) {

                userService.resetPassword(
                                request.getResetToken(),
                                request.getNewPassword());

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Password reset successfully",
                                                null));
        }

        @PutMapping("/deactivate")
        public ResponseEntity<ApiResponse<Void>> deactivateAccount(
                        Authentication authentication,
                        @RequestBody(required = false) DeactivateAccountRequest request) {

                User currentUser = (User) authentication.getPrincipal();

                String password = request != null ? request.getPassword() : null;
                userService.deactivateAccount(currentUser, password);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Account deactivated successfully",
                                                null));
        }

        @PostMapping("/deactivate")
        public ResponseEntity<ApiResponse<Void>> deactivateAccountPost(
                        Authentication authentication,
                        @RequestBody(required = false) DeactivateAccountRequest request) {

                return deactivateAccount(authentication, request);
        }
}
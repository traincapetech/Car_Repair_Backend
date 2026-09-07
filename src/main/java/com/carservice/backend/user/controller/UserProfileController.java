package com.carservice.backend.user.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.dto.DeactivateAccountRequest;
import com.carservice.backend.user.dto.UserResponse;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        UserResponse response = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Authenticated user",
                        response
                )
        );
    }

    @PutMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            Authentication authentication,
            @RequestBody(required = false) DeactivateAccountRequest request
    ) {

        User currentUser = (User) authentication.getPrincipal();
        String password = request != null ? request.getPassword() : null;
        userService.deactivateAccount(currentUser, password);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Account deactivated successfully",
                        null
                )
        );
    }
}
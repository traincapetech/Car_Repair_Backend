package com.carservice.backend.user.service;

import com.carservice.backend.common.exception.InvalidCredentialsException;
import com.carservice.backend.common.exception.UserAlreadyExistsException;
import com.carservice.backend.security.entity.PasswordResetToken;
import com.carservice.backend.security.entity.RefreshToken;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.security.service.PasswordResetTokenService;
import com.carservice.backend.security.service.RefreshTokenService;
import com.carservice.backend.user.dto.ChangePasswordRequest;
import com.carservice.backend.user.dto.CustomerRegistrationRequest;
import com.carservice.backend.user.dto.LoginRequest;
import com.carservice.backend.user.dto.LoginResponse;
import com.carservice.backend.user.dto.LogoutRequest;
import com.carservice.backend.user.dto.RefreshTokenRequest;
import com.carservice.backend.user.dto.TokenResponse;
import com.carservice.backend.user.dto.UpdateProfileRequest;
import com.carservice.backend.user.dto.UserResponse;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final RefreshTokenService refreshTokenService;
        private final PasswordResetTokenService passwordResetTokenService;

        public UserService(
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        RefreshTokenService refreshTokenService,
                        PasswordResetTokenService passwordResetTokenService) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtService = jwtService;
                this.refreshTokenService = refreshTokenService;
                this.passwordResetTokenService = passwordResetTokenService;
        }

        public UserResponse registerCustomer(
                        CustomerRegistrationRequest request) {

                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new UserAlreadyExistsException(
                                        "Email already registered");
                }

                if (userRepository.existsByPhone(request.getPhone())) {
                        throw new UserAlreadyExistsException(
                                        "Phone number already registered");
                }

                User user = new User();

                user.setName(request.getName());
                user.setEmail(request.getEmail());
                user.setPhone(request.getPhone());

                user.setPassword(
                                passwordEncoder.encode(request.getPassword()));

                user.setRole(UserRole.CUSTOMER);

                User savedUser = userRepository.save(user);

                return new UserResponse(
                                savedUser.getId(),
                                savedUser.getName(),
                                savedUser.getEmail(),
                                savedUser.getPhone(),
                                savedUser.getRole(),
                                savedUser.getIsActive(),
                                savedUser.getCreatedAt());
        }

        public LoginResponse login(
                        LoginRequest request) {

                User user = userRepository
                                .findByEmail(request.getEmail())
                                .orElseThrow(() -> new InvalidCredentialsException(
                                                "Invalid email or password"));

                if (!passwordEncoder.matches(
                                request.getPassword(),
                                user.getPassword())) {
                        throw new InvalidCredentialsException(
                                        "Invalid email or password");
                }

                if (!user.getIsActive()) {
                        throw new InvalidCredentialsException(
                                        "User account is inactive");
                }

                String accessToken = jwtService.generateAccessToken(user);

                String refreshToken = jwtService.generateRefreshToken(user);

                refreshTokenService.saveRefreshToken(
                                refreshToken,
                                user,
                                jwtService.getRefreshTokenExpiryDate());

                UserResponse userResponse = new UserResponse(
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getPhone(),
                                user.getRole(),
                                user.getIsActive(),
                                user.getCreatedAt());

                return new LoginResponse(
                                accessToken,
                                refreshToken,
                                userResponse);
        }

        @Transactional
        public TokenResponse refreshToken(
                        RefreshTokenRequest request) {

                // 1. Validate that the supplied JWT is valid and explicitly a REFRESH token
                if (request.refreshToken() == null || !jwtService.isRefreshToken(request.refreshToken())) {
                        throw new InvalidCredentialsException(
                                        "Invalid refresh token");
                }

                // 2. Find refresh token in DB using its hash
                RefreshToken storedToken = refreshTokenService.findByToken(
                                request.refreshToken());

                // 3. Validate refresh token state (not revoked, not expired)
                refreshTokenService.validateRefreshToken(
                                storedToken);

                // 4. Get user and verify account is still active
                User user = storedToken.getUser();

                if (!Boolean.TRUE.equals(user.getIsActive())) {
                        throw new InvalidCredentialsException(
                                        "User account is inactive");
                }

                // 5. Revoke old refresh token (rotation)
                refreshTokenService.revokeToken(
                                storedToken);

                // 6. Generate new access token
                String newAccessToken = jwtService.generateAccessToken(user);

                // 7. Generate new refresh token
                String newRefreshToken = jwtService.generateRefreshToken(user);

                // 8. Save new refresh token
                refreshTokenService.saveRefreshToken(
                                newRefreshToken,
                                user,
                                jwtService.getRefreshTokenExpiryDate());

                // 9. Return new token pair
                return new TokenResponse(
                                newAccessToken,
                                newRefreshToken);
        }

        public UserResponse updateProfile(
                        User currentUser,
                        UpdateProfileRequest request) {

                // Check whether another user already owns this phone number
                userRepository.findByPhone(request.getPhone())
                                .ifPresent(existingUser -> {

                                        if (!existingUser.getId()
                                                        .equals(currentUser.getId())) {

                                                throw new UserAlreadyExistsException(
                                                                "Phone number already registered");
                                        }
                                });

                currentUser.setName(
                                request.getName());

                currentUser.setPhone(
                                request.getPhone());

                User updatedUser = userRepository.save(currentUser);

                return new UserResponse(
                                updatedUser.getId(),
                                updatedUser.getName(),
                                updatedUser.getEmail(),
                                updatedUser.getPhone(),
                                updatedUser.getRole(),
                                updatedUser.getIsActive(),
                                updatedUser.getCreatedAt());
        }

        @Transactional
        public void changePassword(
                        User currentUser,
                        ChangePasswordRequest request) {

                // 1. Verify current password
                if (!passwordEncoder.matches(
                                request.getCurrentPassword(),
                                currentUser.getPassword())) {

                        throw new InvalidCredentialsException(
                                        "Current password is incorrect");
                }

                // 2. Make sure new password and confirmation match
                if (!request.getNewPassword()
                                .equals(request.getConfirmPassword())) {

                        throw new InvalidCredentialsException(
                                        "New password and confirm password do not match");
                }

                // 3. Prevent using the same password
                if (passwordEncoder.matches(
                                request.getNewPassword(),
                                currentUser.getPassword())) {

                        throw new InvalidCredentialsException(
                                        "New password must be different from current password");
                }

                // 4. Encode the new password
                currentUser.setPassword(
                                passwordEncoder.encode(
                                                request.getNewPassword()));

                // 5. Save updated user
                userRepository.save(currentUser);

                // 6. Invalidate existing sessions across devices
                refreshTokenService.revokeAllUserTokens(currentUser.getId());
        }

        /*
         * Logout
         *
         * Revokes the supplied refresh token.
         */
        public void logout(
                        LogoutRequest request) {

                refreshTokenService.logout(
                                request.getRefreshToken());
        }

        @Transactional
        public String forgotPassword(String email) {

                java.util.Optional<User> userOptional = userRepository.findByEmail(email);

                // Avoid user enumeration: do not reveal whether account exists or is inactive
                if (userOptional.isEmpty() || !Boolean.TRUE.equals(userOptional.get().getIsActive())) {
                        return null;
                }

                return passwordResetTokenService
                                .createPasswordResetToken(userOptional.get());
        }

        @Transactional
        public void resetPassword(
                        String resetToken,
                        String newPassword) {

                // 1. Validate reset token
                PasswordResetToken passwordResetToken = passwordResetTokenService.validateToken(resetToken);

                // 2. Get the user
                User user = passwordResetToken.getUser();

                // 3. Make sure account is still active
                if (!Boolean.TRUE.equals(user.getIsActive())) {
                        throw new InvalidCredentialsException(
                                        "User account is inactive");
                }

                // 4. Prevent reusing the same password
                if (passwordEncoder.matches(newPassword, user.getPassword())) {
                        throw new InvalidCredentialsException(
                                        "New password must be different from current password");
                }

                // 5. Update password
                user.setPassword(
                                passwordEncoder.encode(newPassword));

                userRepository.save(user);

                // 6. Mark reset token as used
                passwordResetTokenService.markTokenAsUsed(
                                passwordResetToken);

                /*
                 * Invalidate all existing refresh tokens for this user
                 * so that resetting a password terminates existing sessions.
                 */
                refreshTokenService.revokeAllUserTokens(user.getId());
        }

        /*
         * Account Deactivation
         *
         * Deactivates the user account (sets isActive to false) and revokes all active refresh tokens.
         * If a confirmation password is provided, verifies it first.
         */
        @Transactional
        public void deactivateAccount(
                        User currentUser,
                        String password) {

                if (password != null && !password.isBlank()) {
                        if (!passwordEncoder.matches(
                                        password,
                                        currentUser.getPassword())) {
                                throw new InvalidCredentialsException(
                                                "Current password is incorrect");
                        }
                }

                currentUser.setIsActive(false);
                userRepository.save(currentUser);

                refreshTokenService.revokeAllUserTokens(currentUser.getId());
        }

        @Transactional
        public void deactivateAccount(User currentUser) {
                deactivateAccount(currentUser, null);
        }
}
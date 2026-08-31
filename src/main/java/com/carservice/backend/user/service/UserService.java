package com.carservice.backend.user.service;

import com.carservice.backend.common.exception.UserAlreadyExistsException;
import com.carservice.backend.user.dto.CustomerRegistrationRequest;
import com.carservice.backend.user.dto.LoginRequest;
import com.carservice.backend.user.dto.UserResponse;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carservice.backend.common.exception.InvalidCredentialsException;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.security.service.RefreshTokenService;
import com.carservice.backend.security.entity.RefreshToken;
import com.carservice.backend.user.dto.AuthResponse;
import com.carservice.backend.user.dto.LoginResponse;
import com.carservice.backend.user.dto.RefreshTokenRequest;
import com.carservice.backend.user.dto.TokenResponse;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public UserResponse registerCustomer(
            CustomerRegistrationRequest request
    ) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Email already registered"
            );
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new UserAlreadyExistsException(
                    "Phone number already registered"
            );
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(UserRole.CUSTOMER);

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole(),
                savedUser.getIsActive(),
                savedUser.getCreatedAt()
        );
    }


    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        if (!user.getIsActive()) {
            throw new InvalidCredentialsException(
                    "User account is inactive"
            );
        }

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                jwtService.generateRefreshToken(user);

        refreshTokenService.saveRefreshToken(
                refreshToken,
                user,
                jwtService.getRefreshTokenExpiryDate()
        );

        UserResponse userResponse =
                new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getRole(),
                        user.getIsActive(),
                        user.getCreatedAt()
                );

        return new LoginResponse(
                accessToken,
                refreshToken,
                userResponse
        );
    }

    @Transactional
    public TokenResponse refreshToken(
            RefreshTokenRequest request
    ) {

        // 1. Find the refresh token using its hash
        RefreshToken storedToken =
                refreshTokenService.findByToken(
                        request.refreshToken()
                );

        // 2. Validate token
        refreshTokenService.validateRefreshToken(
                storedToken
        );

        // 3. Get the user
        User user = storedToken.getUser();

        // 4. Check user is still active
        if (!user.getIsActive()) {
            throw new InvalidCredentialsException(
                    "User account is inactive"
            );
        }

        // Validate that JWT itself is a refresh token
        if (!"REFRESH".equals(
                jwtService.extractTokenType(
                        request.refreshToken()
                )
        )) {
            throw new InvalidCredentialsException(
                    "Invalid token type"
            );
        }

        // 5. Revoke the old refresh token
        refreshTokenService.revokeToken(
                storedToken
        );

        // 6. Generate new access token
        String newAccessToken =
                jwtService.generateAccessToken(user);

        // 7. Generate new refresh token
        String newRefreshToken =
                jwtService.generateRefreshToken(user);

        // 8. Save the new refresh token hash
        refreshTokenService.saveRefreshToken(
                newRefreshToken,
                user,
                jwtService.getRefreshTokenExpiryDate()
        );

        // 9. Return the new token pair
        return new TokenResponse(
                newAccessToken,
                newRefreshToken
        );
    }
}
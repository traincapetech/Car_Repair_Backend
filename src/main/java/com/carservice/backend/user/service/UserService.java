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
import com.carservice.backend.common.exception.InvalidCredentialsException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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


    public UserResponse login(LoginRequest request) {

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

    return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole(),
            user.getIsActive(),
            user.getCreatedAt()
    );
}
}
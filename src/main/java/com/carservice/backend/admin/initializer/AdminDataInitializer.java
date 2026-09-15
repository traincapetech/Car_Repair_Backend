package com.carservice.backend.admin.initializer;

import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Environment-safe Admin Data Initializer.
 * Seeds a default administrator only if no user exists with the configured admin email.
 * Never mutates or overwrites existing records.
 */
@Component
@Order(20)
public class AdminDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.enabled:true}")
    private boolean adminEnabled;

    @Value("${admin.default.email:admin@carservice.com}")
    private String adminEmail;

    @Value("${admin.default.password:Admin@12345}")
    private String adminPassword;

    @Value("${admin.default.name:System Administrator}")
    private String adminName;

    @Value("${admin.default.phone:9999999999}")
    private String adminPhone;

    public AdminDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminEnabled) {
            log.info("Default admin provisioning is disabled by configuration");
            return;
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.debug("Admin user '{}' already exists. Skipping provisioning.", adminEmail);
            return;
        }

        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(adminEmail);
        admin.setPhone(adminPhone);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setIsActive(true);

        userRepository.save(admin);
        log.info("Successfully provisioned default administrative user '{}' with role {}", adminEmail, UserRole.ADMIN);
    }
}

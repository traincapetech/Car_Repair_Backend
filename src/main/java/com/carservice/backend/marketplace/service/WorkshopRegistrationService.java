package com.carservice.backend.marketplace.service;

import com.carservice.backend.common.exception.UserAlreadyExistsException;
import com.carservice.backend.admin.service.AuditService;
import com.carservice.backend.marketplace.dto.WorkshopRegistrationRequest;
import com.carservice.backend.marketplace.dto.WorkshopRegistrationResponse;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.entity.WorkshopService;
import com.carservice.backend.marketplace.entity.WorkshopWallet;
import com.carservice.backend.marketplace.enums.MarketplaceEventType;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.marketplace.repository.WorkshopServiceRepository;
import com.carservice.backend.marketplace.repository.WorkshopWalletRepository;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkshopRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(WorkshopRegistrationService.class);

    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;
    private final WorkshopServiceRepository workshopServiceRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final WorkshopWalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final MarketplaceAuditService marketplaceAuditService;
    private final PlatformConfigService platformConfigService;

    public WorkshopRegistrationService(
            UserRepository userRepository,
            WorkshopRepository workshopRepository,
            WorkshopServiceRepository workshopServiceRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            WorkshopWalletRepository walletRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            MarketplaceAuditService marketplaceAuditService,
            PlatformConfigService platformConfigService
    ) {
        this.userRepository = userRepository;
        this.workshopRepository = workshopRepository;
        this.workshopServiceRepository = workshopServiceRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.marketplaceAuditService = marketplaceAuditService;
        this.platformConfigService = platformConfigService;
    }

    @Transactional
    public WorkshopRegistrationResponse registerWorkshop(WorkshopRegistrationRequest request) {
        log.info("Processing self-registration for workshop: [{}] with email [{}]",
                request.getBusinessName(), request.getEmail());

        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

        // 1. Uniqueness validation
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email is already registered");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new UserAlreadyExistsException("Phone number is already registered");
        }
        if (workshopRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Workshop email is already registered");
        }
        if (workshopRepository.existsByPhone(phone)) {
            throw new UserAlreadyExistsException("Workshop phone number is already registered");
        }

        // 2. Coordinate range validation
        BigDecimal lat = request.getLatitude();
        BigDecimal lng = request.getLongitude();
        if (lat == null || lat.compareTo(new BigDecimal("-90.0")) < 0 || lat.compareTo(new BigDecimal("90.0")) > 0) {
            throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0 degrees");
        }
        if (lng == null || lng.compareTo(new BigDecimal("-180.0")) < 0 || lng.compareTo(new BigDecimal("180.0")) > 0) {
            throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0 degrees");
        }

        // 3. Create user account with PARTNER role
        User user = new User();
        user.setName(request.getOwnerName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.PARTNER);
        user.setIsActive(true); // User can login, but workshop verification restricts access
        User savedUser = userRepository.save(user);

        // 4. Create Workshop entity in PENDING and INACTIVE status
        double defaultRadius = platformConfigService != null
                ? platformConfigService.getMatchingDefaultRadiusKm()
                : 25.0;

        Workshop workshop = new Workshop();
        workshop.setUser(savedUser);
        workshop.setBusinessName(request.getBusinessName().trim());
        workshop.setOwnerName(request.getOwnerName().trim());
        workshop.setEmail(email);
        workshop.setPhone(phone);
        workshop.setAddress(request.getAddress().trim());
        workshop.setCity(request.getCity().trim());
        workshop.setState(request.getState().trim());
        workshop.setPincode(request.getPincode().trim());
        workshop.setLatitude(lat.setScale(7, RoundingMode.HALF_UP));
        workshop.setLongitude(lng.setScale(7, RoundingMode.HALF_UP));
        BigDecimal radius = request.getServiceRadiusKm() != null && request.getServiceRadiusKm().compareTo(BigDecimal.ZERO) > 0
                ? request.getServiceRadiusKm()
                : BigDecimal.valueOf(defaultRadius);
        workshop.setServiceRadiusKm(radius.setScale(2, RoundingMode.HALF_UP));
        workshop.setOpeningTime(request.getOpeningTime() != null && !request.getOpeningTime().isBlank() ? request.getOpeningTime().trim() : "09:00 AM");
        workshop.setClosingTime(request.getClosingTime() != null && !request.getClosingTime().isBlank() ? request.getClosingTime().trim() : "08:00 PM");
        workshop.setWorkingDays(request.getWorkingDays() != null && !request.getWorkingDays().isBlank() ? request.getWorkingDays().trim() : "Monday - Saturday");

        // CRITICAL: New self-registered workshops start as PENDING and INACTIVE
        workshop.setVerificationStatus(WorkshopVerificationStatus.PENDING);
        workshop.setIsActive(false);
        workshop.setStatusReason("Awaiting administrative review and verification approval");

        Workshop savedWorkshop = workshopRepository.save(workshop);

        // 5. Attach selected active services from catalog
        int serviceCount = 0;
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ServiceCatalog> catalogs = serviceCatalogRepository.findAllById(request.getServiceIds());
            for (ServiceCatalog cat : catalogs) {
                if (Boolean.TRUE.equals(cat.getIsActive())) {
                    WorkshopService ws = new WorkshopService(savedWorkshop, cat, true);
                    workshopServiceRepository.save(ws);
                    serviceCount++;
                }
            }
        }

        // 6. Initialize Workshop Wallet
        WorkshopWallet wallet = new WorkshopWallet(savedWorkshop, BigDecimal.ZERO);
        walletRepository.save(wallet);

        // 7. System & Marketplace Audit Logging
        Map<String, Object> afterState = new HashMap<>();
        afterState.put("id", savedWorkshop.getId());
        afterState.put("businessName", savedWorkshop.getBusinessName());
        afterState.put("ownerName", savedWorkshop.getOwnerName());
        afterState.put("city", savedWorkshop.getCity());
        afterState.put("verificationStatus", "PENDING");
        afterState.put("isActive", false);
        afterState.put("latitude", savedWorkshop.getLatitude());
        afterState.put("longitude", savedWorkshop.getLongitude());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("email", savedWorkshop.getEmail());
        metadata.put("phone", savedWorkshop.getPhone());
        metadata.put("serviceCount", serviceCount);

        try {
            auditService.record(
                    savedUser,
                    "WORKSHOP_REGISTERED",
                    "WORKSHOP",
                    String.valueOf(savedWorkshop.getId()),
                    "Workshop [" + savedWorkshop.getBusinessName() + "] self-registered. Awaiting admin approval.",
                    "SUCCESS",
                    null,
                    afterState,
                    metadata
            );
        } catch (Exception e) {
            log.warn("Failed to write audit log for workshop registration: {}", e.getMessage(), e);
        }

        try {
            marketplaceAuditService.recordEvent(
                    MarketplaceEventType.WORKSHOP_STATUS_CHANGED,
                    null,
                    null,
                    savedWorkshop.getId(),
                    savedUser.getId(),
                    "Workshop [" + savedWorkshop.getBusinessName() + "] registered. Status: PENDING / INACTIVE",
                    "{\"verificationStatus\":\"PENDING\",\"isActive\":false,\"serviceCount\":" + serviceCount + "}"
            );
        } catch (Exception e) {
            log.warn("Failed to write marketplace audit event for workshop registration: {}", e.getMessage(), e);
        }

        log.info("Workshop self-registration successful for ID [{}] - {}. Status: PENDING",
                savedWorkshop.getId(), savedWorkshop.getBusinessName());

        return new WorkshopRegistrationResponse(
                savedWorkshop.getId(),
                savedUser.getId(),
                savedWorkshop.getBusinessName(),
                savedWorkshop.getOwnerName(),
                savedWorkshop.getEmail(),
                savedWorkshop.getPhone(),
                savedWorkshop.getAddress(),
                savedWorkshop.getCity(),
                savedWorkshop.getState(),
                savedWorkshop.getPincode(),
                savedWorkshop.getLatitude(),
                savedWorkshop.getLongitude(),
                savedWorkshop.getVerificationStatus(),
                savedWorkshop.getIsActive(),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                serviceCount,
                "Workshop registered successfully. Your account is currently pending administrative review and approval.",
                savedWorkshop.getCreatedAt()
        );
    }
}

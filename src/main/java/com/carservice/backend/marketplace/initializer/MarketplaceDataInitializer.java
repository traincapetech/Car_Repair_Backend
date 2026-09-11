package com.carservice.backend.marketplace.initializer;

import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.WalletTransactionType;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.marketplace.repository.*;
import com.carservice.backend.marketplace.service.PlatformConfigService;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@Order(30)
public class MarketplaceDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceDataInitializer.class);

    private final PlatformConfigRepository platformConfigRepository;
    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;
    private final WorkshopServiceRepository workshopServiceRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final WorkshopWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public MarketplaceDataInitializer(
            PlatformConfigRepository platformConfigRepository,
            UserRepository userRepository,
            WorkshopRepository workshopRepository,
            WorkshopServiceRepository workshopServiceRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            WorkshopWalletRepository walletRepository,
            WalletTransactionRepository transactionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.platformConfigRepository = platformConfigRepository;
        this.userRepository = userRepository;
        this.workshopRepository = workshopRepository;
        this.workshopServiceRepository = workshopServiceRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            seedPlatformConfig();
            seedWorkshops();
            log.info("Marketplace data initialization completed successfully.");
        } catch (Exception e) {
            log.error("Error during marketplace data initialization: {}", e.getMessage(), e);
        }
    }

    private void seedPlatformConfig() {
        Optional<PlatformConfig> feeConfig = platformConfigRepository.findByConfigKey(PlatformConfigService.KEY_LEAD_FEE);
        if (feeConfig.isEmpty()) {
            PlatformConfig config = new PlatformConfig(
                    PlatformConfigService.KEY_LEAD_FEE,
                    PlatformConfigService.DEFAULT_LEAD_FEE.toPlainString(),
                    "Default workshop lead acceptance fee in INR"
            );
            platformConfigRepository.save(config);
            log.info("Seeded default lead acceptance fee: ₹{}", PlatformConfigService.DEFAULT_LEAD_FEE);
        }
    }

    private void seedWorkshops() {
        List<ServiceCatalog> allActiveServices = serviceCatalogRepository.findAllByIsActiveTrueOrderByNameAsc();
        if (allActiveServices.isEmpty()) {
            log.warn("No active services available in catalog to bind to workshops");
            return;
        }

        // Workshop 1: Delhi (Apex Auto Care)
        seedSingleWorkshop(
                "Apex Auto Care Central Delhi",
                "partner.delhi@carservice.com",
                "9811000001",
                "Shop 14, Barakhamba Road, Connaught Place",
                "Delhi",
                "Delhi",
                "110001",
                new BigDecimal("28.6315"),
                new BigDecimal("77.2167"),
                new BigDecimal("30.00"),
                new BigDecimal("1000.00"),
                allActiveServices
        );

        // Workshop 2: Noida (Speedy Motors)
        seedSingleWorkshop(
                "Speedy Motors Noida Sector 62",
                "partner.noida@carservice.com",
                "9811000002",
                "Plot B-9, Electronic City, Sector 62",
                "Noida",
                "Uttar Pradesh",
                "201301",
                new BigDecimal("28.6270"),
                new BigDecimal("77.3725"),
                new BigDecimal("25.00"),
                new BigDecimal("500.00"),
                allActiveServices
        );

        // Workshop 3: Gurgaon (Elite Precision Garage)
        seedSingleWorkshop(
                "Elite Precision Garage CyberHub",
                "partner.gurgaon@carservice.com",
                "9811000003",
                "Building 8B, DLF Cyber City, Phase 2",
                "Gurgaon",
                "Haryana",
                "122002",
                new BigDecimal("28.4900"),
                new BigDecimal("77.0900"),
                new BigDecimal("25.00"),
                new BigDecimal("800.00"),
                allActiveServices
        );

        // Workshop 4: Mumbai (Mumbai Express Auto Worli)
        seedSingleWorkshop(
                "Mumbai Express Auto Worli",
                "partner.mumbai@carservice.com",
                "9811000004",
                "102 Lotus Colony, Dr. E Moses Road, Worli",
                "Mumbai",
                "Maharashtra",
                "400018",
                new BigDecimal("19.0178"),
                new BigDecimal("72.8478"),
                new BigDecimal("20.00"),
                new BigDecimal("500.00"),
                allActiveServices
        );
    }

    private void seedSingleWorkshop(
            String businessName,
            String email,
            String phone,
            String address,
            String city,
            String state,
            String pincode,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal radiusKm,
            BigDecimal initialWalletBalance,
            List<ServiceCatalog> servicesToAttach
    ) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setName(businessName + " Owner");
            newUser.setEmail(email);
            newUser.setPhone(phone);
            newUser.setPassword(passwordEncoder.encode("Partner@1234"));
            newUser.setRole(UserRole.PARTNER);
            newUser.setIsActive(true);
            return userRepository.save(newUser);
        });

        Workshop workshop = workshopRepository.findByUserId(user.getId()).orElseGet(() -> {
            Workshop newW = new Workshop(
                    user,
                    businessName,
                    phone,
                    email,
                    address,
                    city,
                    state,
                    pincode,
                    latitude,
                    longitude,
                    radiusKm,
                    WorkshopVerificationStatus.VERIFIED,
                    true
            );
            return workshopRepository.save(newW);
        });

        // Attach services
        for (ServiceCatalog catalog : servicesToAttach) {
            if (!workshopServiceRepository.existsByWorkshopIdAndServiceCatalogIdAndIsActiveTrue(workshop.getId(), catalog.getId())) {
                WorkshopService ws = new WorkshopService(workshop, catalog, true);
                workshopServiceRepository.save(ws);
            }
        }

        // Initialize wallet
        Optional<WorkshopWallet> existingWallet = walletRepository.findByWorkshopId(workshop.getId());
        if (existingWallet.isEmpty()) {
            WorkshopWallet wallet = new WorkshopWallet(workshop, initialWalletBalance);
            WorkshopWallet savedWallet = walletRepository.save(wallet);

            WalletTransaction tx = new WalletTransaction(
                    savedWallet,
                    WalletTransactionType.CREDIT,
                    initialWalletBalance,
                    initialWalletBalance,
                    "INIT-" + workshop.getId(),
                    "Initial promotional partner wallet balance"
            );
            transactionRepository.save(tx);
        }
    }
}

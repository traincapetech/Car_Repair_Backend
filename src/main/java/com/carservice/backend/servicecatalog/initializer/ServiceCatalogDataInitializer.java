package com.carservice.backend.servicecatalog.initializer;

import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class ServiceCatalogDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ServiceCatalogDataInitializer.class);

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public ServiceCatalogDataInitializer(
            ServiceCatalogRepository serviceCatalogRepository,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper
    ) {
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1. Deactivate old test artifact services so customer catalog is clean
            int deactivated = serviceCatalogRepository.deactivateTestArtifacts();
            if (deactivated > 0) {
                log.info("Deactivated {} test artifact services from customer catalog.", deactivated);
            }

            // 2. Load authentic Indian car services from JSON
            Resource resource = resourceLoader.getResource("classpath:data/indian_service_catalog.json");
            if (!resource.exists()) {
                log.warn("Indian service catalog JSON resource not found at classpath:data/indian_service_catalog.json");
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                List<ServiceSeedData> seedList = objectMapper.readValue(
                        is,
                        new TypeReference<List<ServiceSeedData>>() {}
                );

                int seededCount = 0;
                for (ServiceSeedData seed : seedList) {
                    Optional<ServiceCatalog> existingOpt = serviceCatalogRepository.findByNameIgnoreCase(seed.getName());
                    ServiceCatalog service;
                    if (existingOpt.isPresent()) {
                        service = existingOpt.get();
                        service.setDescription(seed.getDescription());
                        service.setCategory(ServiceCategory.valueOf(seed.getCategory()));
                        service.setBasePrice(seed.getBasePrice());
                        service.setEstimatedDurationMinutes(seed.getEstimatedDurationMinutes());
                        service.setIsActive(true);
                    } else {
                        service = new ServiceCatalog(
                                seed.getName(),
                                seed.getDescription(),
                                ServiceCategory.valueOf(seed.getCategory()),
                                seed.getBasePrice(),
                                seed.getEstimatedDurationMinutes(),
                                true
                        );
                    }
                    serviceCatalogRepository.save(service);
                    seededCount++;
                }

                log.info("Successfully synced {} authentic Indian car services into catalog.", seededCount);
            }
        } catch (Exception e) {
            log.error("Failed to seed Indian service catalog: {}", e.getMessage(), e);
        }
    }

    public static class ServiceSeedData {
        private String name;
        private String category;
        private BigDecimal basePrice;
        private Integer estimatedDurationMinutes;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
        }

        public Integer getEstimatedDurationMinutes() {
            return estimatedDurationMinutes;
        }

        public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
            this.estimatedDurationMinutes = estimatedDurationMinutes;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}

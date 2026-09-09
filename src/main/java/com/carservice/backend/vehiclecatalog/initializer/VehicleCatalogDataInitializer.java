package com.carservice.backend.vehiclecatalog.initializer;

import com.carservice.backend.vehiclecatalog.entity.VehicleBrand;
import com.carservice.backend.vehiclecatalog.service.VehicleCatalogService;
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
import java.util.List;

@Component
public class VehicleCatalogDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VehicleCatalogDataInitializer.class);

    private final VehicleCatalogService vehicleCatalogService;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public VehicleCatalogDataInitializer(
            VehicleCatalogService vehicleCatalogService,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper
    ) {
        this.vehicleCatalogService = vehicleCatalogService;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Resource resource = resourceLoader.getResource("classpath:data/indian_vehicle_catalog.json");
            if (!resource.exists()) {
                log.warn("Indian vehicle catalog JSON resource not found at classpath:data/indian_vehicle_catalog.json");
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                List<BrandSeedData> seedData = objectMapper.readValue(
                        is,
                        new TypeReference<List<BrandSeedData>>() {}
                );

                int seededBrands = 0;
                int seededModels = 0;

                for (BrandSeedData brandData : seedData) {
                    VehicleBrand brand = vehicleCatalogService.getOrCreateBrand(
                            brandData.getName(),
                            brandData.getCountryOfOrigin(),
                            brandData.getDisplayOrder()
                    );
                    seededBrands++;

                    if (brandData.getModels() != null) {
                        for (ModelSeedData modelData : brandData.getModels()) {
                            vehicleCatalogService.getOrCreateModel(
                                    brand,
                                    modelData.getName(),
                                    modelData.getBodyType(),
                                    modelData.getYearIntroduced(),
                                    modelData.getYearDiscontinued()
                            );
                            seededModels++;
                        }
                    }
                }

                log.info("Successfully synced Indian Vehicle Catalog: {} brands and {} models.", seededBrands, seededModels);
            }
        } catch (Exception e) {
            log.error("Failed to seed Indian vehicle catalog from JSON: {}", e.getMessage(), e);
        }
    }

    public static class BrandSeedData {
        private String name;
        private String countryOfOrigin;
        private Integer displayOrder;
        private List<ModelSeedData> models;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCountryOfOrigin() {
            return countryOfOrigin;
        }

        public void setCountryOfOrigin(String countryOfOrigin) {
            this.countryOfOrigin = countryOfOrigin;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }

        public List<ModelSeedData> getModels() {
            return models;
        }

        public void setModels(List<ModelSeedData> models) {
            this.models = models;
        }
    }

    public static class ModelSeedData {
        private String name;
        private String bodyType;
        private Integer yearIntroduced;
        private Integer yearDiscontinued;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBodyType() {
            return bodyType;
        }

        public void setBodyType(String bodyType) {
            this.bodyType = bodyType;
        }

        public Integer getYearIntroduced() {
            return yearIntroduced;
        }

        public void setYearIntroduced(Integer yearIntroduced) {
            this.yearIntroduced = yearIntroduced;
        }

        public Integer getYearDiscontinued() {
            return yearDiscontinued;
        }

        public void setYearDiscontinued(Integer yearDiscontinued) {
            this.yearDiscontinued = yearDiscontinued;
        }
    }
}

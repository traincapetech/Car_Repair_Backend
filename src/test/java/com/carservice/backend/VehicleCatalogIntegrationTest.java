package com.carservice.backend;

import com.carservice.backend.vehiclecatalog.entity.VehicleBrand;
import com.carservice.backend.vehiclecatalog.repository.VehicleBrandRepository;
import com.carservice.backend.vehiclecatalog.repository.VehicleModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class VehicleCatalogIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private VehicleBrandRepository brandRepository;

    @Autowired
    private VehicleModelRepository modelRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Should seed and fetch all active passenger vehicle brands in India")
    void shouldFetchAllActiveBrands() throws Exception {
        mockMvc.perform(get("/api/v1/vehicle-catalog/brands")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(40))))
                .andExpect(jsonPath("$.data[0].name").value("Maruti Suzuki"))
                .andExpect(jsonPath("$.data[0].modelCount", greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data[1].name").value("Hyundai"))
                .andExpect(jsonPath("$.data[2].name").value("Tata Motors"))
                .andExpect(jsonPath("$.data[3].name").value("Mahindra"));
    }

    @Test
    @DisplayName("Should fetch models by brand ID")
    void shouldFetchModelsByBrandId() throws Exception {
        VehicleBrand maruti = brandRepository.findByNormalizedName("maruti suzuki")
                .orElseThrow(() -> new AssertionError("Maruti Suzuki brand should exist"));

        mockMvc.perform(get("/api/v1/vehicle-catalog/brands/" + maruti.getId() + "/models")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(30))))
                .andExpect(jsonPath("$.data[*].name", hasItems("Swift", "Baleno", "Alto", "800", "Gypsy", "Dzire", "Brezza")));
    }

    @Test
    @DisplayName("Should fetch models by brand name (case-insensitive query)")
    void shouldFetchModelsByBrandName() throws Exception {
        mockMvc.perform(get("/api/v1/vehicle-catalog/models")
                        .param("brand", "Hyundai")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(25))))
                .andExpect(jsonPath("$.data[*].name", hasItems("Santro", "Creta", "i20", "Verna", "Venue")));
    }

    @Test
    @DisplayName("Should return 404 when querying models for nonexistent brand ID")
    void shouldReturn404ForNonexistentBrandId() throws Exception {
        mockMvc.perform(get("/api/v1/vehicle-catalog/brands/9999999/models")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should verify discontinued brands and historical models are properly cataloged")
    void shouldVerifyDiscontinuedBrandsAndHistoricalModels() throws Exception {
        VehicleBrand ford = brandRepository.findByNormalizedName("ford")
                .orElseThrow(() -> new AssertionError("Ford brand should exist"));
        VehicleBrand chevrolet = brandRepository.findByNormalizedName("chevrolet")
                .orElseThrow(() -> new AssertionError("Chevrolet brand should exist"));
        VehicleBrand fiat = brandRepository.findByNormalizedName("fiat")
                .orElseThrow(() -> new AssertionError("Fiat brand should exist"));

        mockMvc.perform(get("/api/v1/vehicle-catalog/brands/" + ford.getId() + "/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", hasItems("Ikon", "Figo (Gen 1)", "EcoSport", "Endeavour (Gen 1)")));

        mockMvc.perform(get("/api/v1/vehicle-catalog/brands/" + chevrolet.getId() + "/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", hasItems("Tavera", "Cruze", "Beat", "Spark")));

        mockMvc.perform(get("/api/v1/vehicle-catalog/brands/" + fiat.getId() + "/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", hasItems("Palio", "Linea", "Grande Punto", "Uno")));
    }
}

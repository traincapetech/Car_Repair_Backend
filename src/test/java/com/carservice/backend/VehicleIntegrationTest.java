package com.carservice.backend;

import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import com.carservice.backend.vehicle.entity.Vehicle;
import com.carservice.backend.vehicle.enums.FuelType;
import com.carservice.backend.vehicle.enums.Transmission;
import com.carservice.backend.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class VehicleIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        userA = new User();
        userA.setName("Customer A");
        userA.setEmail("customer.a." + suffixA + "@test.com");
        userA.setPhone("9" + (System.currentTimeMillis() % 1000000000L));
        userA.setPassword(passwordEncoder.encode("Password@123"));
        userA.setRole(UserRole.CUSTOMER);
        userA.setIsActive(true);
        userA = userRepository.save(userA);
        tokenA = jwtService.generateAccessToken(userA);

        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        userB = new User();
        userB.setName("Customer B");
        userB.setEmail("customer.b." + suffixB + "@test.com");
        userB.setPhone("8" + ((System.currentTimeMillis() + 1) % 1000000000L));
        userB.setPassword(passwordEncoder.encode("Password@123"));
        userB.setRole(UserRole.CUSTOMER);
        userB.setIsActive(true);
        userB = userRepository.save(userB);
        tokenB = jwtService.generateAccessToken(userB);
    }

    @Test
    @DisplayName("1. Create vehicle successfully (201 Created) with normalized registration number")
    void testCreateVehicle_Success() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String jsonPayload = String.format("""
                {
                    "make": "Hyundai",
                    "model": "i20",
                    "year": 2023,
                    "registrationNumber": "DL 01 AB %s",
                    "fuelType": "PETROL",
                    "transmission": "MANUAL"
                }
                """, uniqueSuffix);

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vehicle added successfully"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.make").value("Hyundai"))
                .andExpect(jsonPath("$.data.model").value("i20"))
                .andExpect(jsonPath("$.data.year").value(2023))
                .andExpect(jsonPath("$.data.registrationNumber").value("DL01AB" + uniqueSuffix))
                .andExpect(jsonPath("$.data.fuelType").value("PETROL"))
                .andExpect(jsonPath("$.data.transmission").value("MANUAL"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("2. Unauthenticated request to /api/v1/vehicles is rejected (401 Unauthorized)")
    void testCreateVehicle_Unauthenticated() throws Exception {
        String jsonPayload = """
                {
                    "make": "Honda",
                    "model": "City",
                    "year": 2022,
                    "registrationNumber": "MH12AB1234",
                    "fuelType": "PETROL",
                    "transmission": "AUTOMATIC"
                }
                """;

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("3. Duplicate registration number is rejected (409 Conflict)")
    void testCreateVehicle_DuplicateRegistrationNumber() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String regNum = "KA05XY" + uniqueSuffix;

        String jsonPayload1 = String.format("""
                {
                    "make": "Tata",
                    "model": "Nexon",
                    "year": 2022,
                    "registrationNumber": "%s",
                    "fuelType": "DIESEL",
                    "transmission": "AUTOMATIC"
                }
                """, regNum);

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload1))
                .andExpect(status().isCreated());

        // Attempt to create second vehicle with same registration number (even with different casing/spacing)
        String jsonPayload2 = String.format("""
                {
                    "make": "Mahindra",
                    "model": "XUV700",
                    "year": 2023,
                    "registrationNumber": "%s",
                    "fuelType": "PETROL",
                    "transmission": "AUTOMATIC"
                }
                """, "ka-05 xy-" + uniqueSuffix.toLowerCase());

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("4. Invalid vehicle input validation fails (400 Bad Request)")
    void testCreateVehicle_ValidationFailure() throws Exception {
        String jsonPayload = """
                {
                    "make": "",
                    "model": "",
                    "year": 1800,
                    "registrationNumber": "??",
                    "fuelType": null,
                    "transmission": null
                }
                """;

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.make").isNotEmpty())
                .andExpect(jsonPath("$.data.model").isNotEmpty())
                .andExpect(jsonPath("$.data.year").isNotEmpty())
                .andExpect(jsonPath("$.data.registrationNumber").isNotEmpty());
    }

    @Test
    @DisplayName("5. Malformed payload / invalid enum value rejected (400 Bad Request)")
    void testCreateVehicle_InvalidEnumValue() throws Exception {
        String jsonPayload = """
                {
                    "make": "Tesla",
                    "model": "Model 3",
                    "year": 2024,
                    "registrationNumber": "DL02XY9999",
                    "fuelType": "HYDROGEN_NOT_REAL",
                    "transmission": "AUTOMATIC"
                }
                """;

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("6. View all vehicles returns only the authenticated user's vehicles")
    void testGetMyVehicles_OwnershipSeparation() throws Exception {
        String suffixA1 = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String suffixA2 = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String suffixB1 = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Vehicle vA1 = new Vehicle(userA, "Toyota", "Innova", 2021, "TN01" + suffixA1, FuelType.DIESEL, Transmission.MANUAL);
        Vehicle vA2 = new Vehicle(userA, "Maruti", "Swift", 2022, "TN02" + suffixA2, FuelType.PETROL, Transmission.AUTOMATIC);
        Vehicle vB1 = new Vehicle(userB, "Hyundai", "Creta", 2023, "TN03" + suffixB1, FuelType.PETROL, Transmission.DCT);

        vehicleRepository.save(vA1);
        vehicleRepository.save(vA2);
        vehicleRepository.save(vB1);

        // Customer A should see exactly 2 vehicles
        mockMvc.perform(get("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].registrationNumber", hasItems("TN01" + suffixA1, "TN02" + suffixA2)))
                .andExpect(jsonPath("$.data[*].registrationNumber", not(hasItem("TN03" + suffixB1))));

        // Customer B should see exactly 1 vehicle
        mockMvc.perform(get("/api/v1/vehicles")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].registrationNumber").value("TN03" + suffixB1));
    }

    @Test
    @DisplayName("7. View one vehicle returns vehicle if owned by caller (200 OK)")
    void testGetMyVehicle_Success() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Vehicle v = new Vehicle(userA, "Kia", "Seltos", 2024, "HR26" + suffix, FuelType.PETROL, Transmission.CVT);
        v = vehicleRepository.save(v);

        mockMvc.perform(get("/api/v1/vehicles/" + v.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(v.getId()))
                .andExpect(jsonPath("$.data.make").value("Kia"))
                .andExpect(jsonPath("$.data.model").value("Seltos"))
                .andExpect(jsonPath("$.data.registrationNumber").value("HR26" + suffix));
    }

    @Test
    @DisplayName("8. Cross-user view: User B cannot view User A's vehicle (404 Not Found - no enumeration)")
    void testGetMyVehicle_CrossUserAccessForbidden() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Vehicle vA = new Vehicle(userA, "BMW", "3 Series", 2023, "MH01" + suffix, FuelType.PETROL, Transmission.AUTOMATIC);
        vA = vehicleRepository.save(vA);

        mockMvc.perform(get("/api/v1/vehicles/" + vA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    @DisplayName("9. Update vehicle successfully (200 OK)")
    void testUpdateVehicle_Success() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Vehicle v = new Vehicle(userA, "Skoda", "Slavia", 2022, "UP16" + suffix, FuelType.PETROL, Transmission.MANUAL);
        v = vehicleRepository.save(v);

        String updatePayload = String.format("""
                {
                    "make": "Skoda",
                    "model": "Slavia 1.5 TSI",
                    "year": 2023,
                    "registrationNumber": "UP 16 %s",
                    "fuelType": "PETROL",
                    "transmission": "DCT"
                }
                """, suffix);

        mockMvc.perform(put("/api/v1/vehicles/" + v.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vehicle updated successfully"))
                .andExpect(jsonPath("$.data.model").value("Slavia 1.5 TSI"))
                .andExpect(jsonPath("$.data.year").value(2023))
                .andExpect(jsonPath("$.data.transmission").value("DCT"));
    }

    @Test
    @DisplayName("10. Cross-user update: User B cannot update User A's vehicle (404 Not Found)")
    void testUpdateVehicle_CrossUserAccessForbidden() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Vehicle vA = new Vehicle(userA, "Audi", "A4", 2021, "KA01" + suffix, FuelType.PETROL, Transmission.AUTOMATIC);
        vA = vehicleRepository.save(vA);

        String updatePayload = String.format("""
                {
                    "make": "Audi",
                    "model": "A6",
                    "year": 2022,
                    "registrationNumber": "KA01%s",
                    "fuelType": "PETROL",
                    "transmission": "AUTOMATIC"
                }
                """, suffix);

        mockMvc.perform(put("/api/v1/vehicles/" + vA.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        // Verify vehicle in DB was unchanged
        Vehicle unchanged = vehicleRepository.findById(vA.getId()).orElseThrow();
        assertEquals("A4", unchanged.getModel());
    }

    @Test
    @DisplayName("11. Update vehicle to an already existing registration number rejected (409 Conflict)")
    void testUpdateVehicle_DuplicateRegistrationNumber() throws Exception {
        String suffix1 = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String suffix2 = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Vehicle v1 = new Vehicle(userA, "Ford", "EcoSport", 2020, "DL05" + suffix1, FuelType.DIESEL, Transmission.MANUAL);
        Vehicle v2 = new Vehicle(userA, "Ford", "Endeavour", 2021, "DL05" + suffix2, FuelType.DIESEL, Transmission.AUTOMATIC);
        v1 = vehicleRepository.save(v1);
        v2 = vehicleRepository.save(v2);

        // Try to update v2 to have v1's registration number
        String updatePayload = String.format("""
                {
                    "make": "Ford",
                    "model": "Endeavour Sport",
                    "year": 2021,
                    "registrationNumber": "DL05%s",
                    "fuelType": "DIESEL",
                    "transmission": "AUTOMATIC"
                }
                """, suffix1);

        mockMvc.perform(put("/api/v1/vehicles/" + v2.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("12. Delete vehicle successfully (200 OK)")
    void testDeleteVehicle_Success() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Vehicle v = new Vehicle(userA, "MG", "Hector", 2021, "GJ01" + suffix, FuelType.PETROL, Transmission.AUTOMATIC);
        v = vehicleRepository.save(v);

        mockMvc.perform(delete("/api/v1/vehicles/" + v.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Vehicle deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        // Verify vehicle is gone
        assertTrue(vehicleRepository.findById(v.getId()).isEmpty());

        // Verify fetching deleted vehicle returns 404
        mockMvc.perform(get("/api/v1/vehicles/" + v.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("13. Cross-user delete: User B cannot delete User A's vehicle (404 Not Found)")
    void testDeleteVehicle_CrossUserAccessForbidden() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Vehicle vA = new Vehicle(userA, "Volvo", "XC60", 2022, "WB01" + suffix, FuelType.DIESEL, Transmission.AUTOMATIC);
        vA = vehicleRepository.save(vA);

        mockMvc.perform(delete("/api/v1/vehicles/" + vA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        // Verify vehicle still exists in database
        assertTrue(vehicleRepository.findById(vA.getId()).isPresent());
    }
}

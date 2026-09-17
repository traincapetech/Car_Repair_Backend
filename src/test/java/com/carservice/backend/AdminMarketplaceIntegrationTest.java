package com.carservice.backend;

import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.enums.BookingTimeSlot;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.repository.*;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import com.carservice.backend.servicecatalog.enums.DiscountType;
import com.carservice.backend.servicecatalog.enums.ServiceCategory;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
public class AdminMarketplaceIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ServiceRequestItemRepository serviceRequestItemRepository;

    @Autowired
    private LeadOpportunityRepository leadOpportunityRepository;

    @Autowired
    private WorkshopPaymentRepository workshopPaymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private MarketplaceAuditEventRepository auditEventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User customerUser;
    private User partnerUser;

    private String adminToken;
    private String customerToken;
    private String partnerToken;

    private Workshop workshopA;
    private Workshop workshopB;
    private Vehicle vehicle;
    private ServiceCatalog serviceCatalog;
    private ServiceRequest serviceRequest;
    private LeadOpportunity opportunityA;
    private LeadOpportunity opportunityB;
    private WorkshopPayment paymentA;
    private Refund refundB;
    private Booking booking;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Admin
        adminUser = new User();
        adminUser.setName("Admin MP " + runId);
        adminUser.setEmail("adm_mp_" + runId + "@carservice.com");
        adminUser.setPhone("91" + Math.abs(runId.hashCode() % 100000000));
        if (adminUser.getPhone().length() < 10) adminUser.setPhone("9188" + runId.substring(0, 6));
        adminUser.setPassword(passwordEncoder.encode("Admin@12345"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);

        // 2. Create Customer
        customerUser = new User();
        customerUser.setName("Customer MP " + runId);
        customerUser.setEmail("cust_mp_" + runId + "@carservice.com");
        customerUser.setPhone("92" + Math.abs(runId.hashCode() % 100000000));
        if (customerUser.getPhone().length() < 10) customerUser.setPhone("9288" + runId.substring(0, 6));
        customerUser.setPassword(passwordEncoder.encode("Cust@12345"));
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setIsActive(true);
        customerUser = userRepository.save(customerUser);
        customerToken = jwtService.generateAccessToken(customerUser);

        // 3. Create Partner User A
        partnerUser = new User();
        partnerUser.setName("Partner MP A " + runId);
        partnerUser.setEmail("part_a_" + runId + "@carservice.com");
        partnerUser.setPhone("93" + Math.abs(runId.hashCode() % 100000000));
        if (partnerUser.getPhone().length() < 10) partnerUser.setPhone("9388" + runId.substring(0, 6));
        partnerUser.setPassword(passwordEncoder.encode("Part@12345"));
        partnerUser.setRole(UserRole.PARTNER);
        partnerUser.setIsActive(true);
        partnerUser = userRepository.save(partnerUser);
        partnerToken = jwtService.generateAccessToken(partnerUser);

        // Create Partner User B
        User partnerUserB = new User();
        partnerUserB.setName("Partner MP B " + runId);
        partnerUserB.setEmail("part_b_" + runId + "@carservice.com");
        partnerUserB.setPhone("94" + Math.abs(runId.hashCode() % 100000000));
        if (partnerUserB.getPhone().length() < 10) partnerUserB.setPhone("9488" + runId.substring(0, 6));
        partnerUserB.setPassword(passwordEncoder.encode("Part@12345"));
        partnerUserB.setRole(UserRole.PARTNER);
        partnerUserB.setIsActive(true);
        partnerUserB = userRepository.save(partnerUserB);

        // 4. Create Workshops
        workshopA = new Workshop();
        workshopA.setUser(partnerUser);
        workshopA.setBusinessName("Apex Motors " + runId);
        workshopA.setPhone("9811" + runId.substring(0, 6));
        workshopA.setEmail("apex_" + runId + "@workshop.com");
        workshopA.setAddress("Plot 42, Okhla Phase 3");
        workshopA.setCity("New Delhi");
        workshopA.setState("Delhi");
        workshopA.setPincode("110020");
        workshopA.setIsActive(true);
        workshopA.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        workshopA.setServiceRadiusKm(new BigDecimal("15.00"));
        workshopA.setLatitude(new BigDecimal("28.5355000"));
        workshopA.setLongitude(new BigDecimal("77.2680000"));
        workshopA = workshopRepository.save(workshopA);

        workshopB = new Workshop();
        workshopB.setUser(partnerUserB);
        workshopB.setBusinessName("Zenith Auto " + runId);
        workshopB.setPhone("9822" + runId.substring(0, 6));
        workshopB.setEmail("zenith_" + runId + "@workshop.com");
        workshopB.setAddress("Plot 88, Mayapuri Industrial Area");
        workshopB.setCity("New Delhi");
        workshopB.setState("Delhi");
        workshopB.setPincode("110064");
        workshopB.setIsActive(true);
        workshopB.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        workshopB.setServiceRadiusKm(new BigDecimal("20.00"));
        workshopB.setLatitude(new BigDecimal("28.6280000"));
        workshopB.setLongitude(new BigDecimal("77.1120000"));
        workshopB = workshopRepository.save(workshopB);

        // 5. Create Vehicle
        vehicle = new Vehicle();
        vehicle.setUser(customerUser);
        vehicle.setMake("Hyundai");
        vehicle.setModel("Creta SX");
        vehicle.setYear(2023);
        vehicle.setRegistrationNumber("DL-01-" + runId.toUpperCase());
        vehicle.setFuelType(FuelType.PETROL);
        vehicle.setTransmission(Transmission.AUTOMATIC);
        vehicle = vehicleRepository.save(vehicle);

        // 6. Create Service Catalog
        serviceCatalog = new ServiceCatalog(
                "Comprehensive Periodic Service " + runId,
                "Full 40-point inspection and filter replacement",
                ServiceCategory.PERIODIC_SERVICE,
                new BigDecimal("2999.00"),
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("200.00"),
                180,
                true
        );
        serviceCatalog = serviceCatalogRepository.save(serviceCatalog);

        // 7. Create Service Request
        serviceRequest = new ServiceRequest();
        serviceRequest.setRequestReference("SR-" + runId.toUpperCase());
        serviceRequest.setUser(customerUser);
        serviceRequest.setVehicle(vehicle);
        serviceRequest.setCity("New Delhi");
        serviceRequest.setAddress("742 Evergreen Terrace, Sector 15");
        serviceRequest.setPincode("110001");
        serviceRequest.setLatitude(new BigDecimal("28.6139000"));
        serviceRequest.setLongitude(new BigDecimal("77.2090000"));
        serviceRequest.setPreferredDate(LocalDate.now().plusDays(2));
        serviceRequest.setPreferredTimeSlot("10:00 - 11:00");
        serviceRequest.setCustomerNotes("Please check brake fluid and AC filter thoroughly.");
        serviceRequest.setTotalAmount(new BigDecimal("2799.00"));
        serviceRequest.setStatus(ServiceRequestStatus.ACCEPTED);
        serviceRequest.setAssignedWorkshop(workshopA);
        serviceRequest.setBookingReference("BK-" + runId.toUpperCase());
        serviceRequest = serviceRequestRepository.save(serviceRequest);

        // 8. Create Service Request Item
        ServiceRequestItem item = new ServiceRequestItem();
        item.setServiceRequest(serviceRequest);
        item.setServiceCatalog(serviceCatalog);
        item.setServiceNameSnapshot(serviceCatalog.getName());
        item.setBasePriceSnapshot(new BigDecimal("2999.00"));
        item.setDiscountTypeSnapshot(DiscountType.FIXED_AMOUNT);
        item.setDiscountValueSnapshot(new BigDecimal("200.00"));
        item.setFinalPriceSnapshot(new BigDecimal("2799.00"));
        serviceRequestItemRepository.save(item);

        // 9. Create Booking
        booking = new Booking();
        booking.setBookingReference("BK-" + runId.toUpperCase());
        booking.setUser(customerUser);
        booking.setVehicle(vehicle);
        booking.setService(serviceCatalog);
        booking.setServiceRequest(serviceRequest);
        booking.setBookingDate(LocalDate.now().plusDays(2));
        booking.setTimeSlot("10:00 - 11:00");
        booking.setBookingTime(LocalTime.of(10, 0));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("2799.00"));
        booking.setEstimatedPrice(new BigDecimal("2799.00"));
        booking = bookingRepository.save(booking);

        serviceRequest.setBooking(booking);
        serviceRequest = serviceRequestRepository.save(serviceRequest);

        // 10. Create Lead Opportunity A (Assigned / Won)
        opportunityA = new LeadOpportunity();
        opportunityA.setServiceRequest(serviceRequest);
        opportunityA.setWorkshop(workshopA);
        opportunityA.setFeeSnapshot(new BigDecimal("99.00"));
        opportunityA.setStatus(OpportunityStatus.CUSTOMER_DETAILS_UNLOCKED);
        opportunityA.setViewedAt(LocalDateTime.now().minusMinutes(50));
        opportunityA.setAcceptedAt(LocalDateTime.now().minusMinutes(45));
        opportunityA.setPaidAt(LocalDateTime.now().minusMinutes(40));
        opportunityA.setUnlockedAt(LocalDateTime.now().minusMinutes(40));
        opportunityA = leadOpportunityRepository.save(opportunityA);

        // 11. Create Workshop Payment A
        paymentA = new WorkshopPayment();
        paymentA.setOpportunity(opportunityA);
        paymentA.setWorkshop(workshopA);
        paymentA.setAmount(new BigDecimal("99.00"));
        paymentA.setCurrency("INR");
        paymentA.setPaymentMethod(PaymentMethod.WALLET);
        paymentA.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentA.setPaidAt(LocalDateTime.now().minusMinutes(40));
        paymentA.setIdempotencyKey("idemp_a_" + runId);
        paymentA = workshopPaymentRepository.save(paymentA);

        // 12. Create Lead Opportunity B (Lost in race condition)
        opportunityB = new LeadOpportunity();
        opportunityB.setServiceRequest(serviceRequest);
        opportunityB.setWorkshop(workshopB);
        opportunityB.setFeeSnapshot(new BigDecimal("99.00"));
        opportunityB.setStatus(OpportunityStatus.LOST);
        opportunityB.setViewedAt(LocalDateTime.now().minusMinutes(48));
        opportunityB.setAcceptedAt(LocalDateTime.now().minusMinutes(42));
        opportunityB = leadOpportunityRepository.save(opportunityB);

        // 13. Create Payment & Refund B
        WorkshopPayment paymentB = new WorkshopPayment();
        paymentB.setOpportunity(opportunityB);
        paymentB.setWorkshop(workshopB);
        paymentB.setAmount(new BigDecimal("99.00"));
        paymentB.setCurrency("INR");
        paymentB.setPaymentMethod(PaymentMethod.WALLET);
        paymentB.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentB.setPaidAt(LocalDateTime.now().minusMinutes(39));
        paymentB.setIdempotencyKey("idemp_b_" + runId);
        paymentB = workshopPaymentRepository.save(paymentB);

        refundB = new Refund();
        refundB.setPayment(paymentB);
        refundB.setOpportunity(opportunityB);
        refundB.setWorkshop(workshopB);
        refundB.setRefundAmount(new BigDecimal("99.00"));
        refundB.setRefundStatus(RefundStatus.SUCCESS);
        refundB.setRefundReason(RefundReason.OPPORTUNITY_ALREADY_ASSIGNED);
        refundB.setInitiatedAt(LocalDateTime.now().minusMinutes(38));
        refundB.setProcessedAt(LocalDateTime.now().minusMinutes(37));
        refundB = refundRepository.save(refundB);

        // 14. Create Audit Events for Service Request
        MarketplaceAuditEvent event1 = new MarketplaceAuditEvent(
                MarketplaceEventType.REQUEST_CREATED,
                serviceRequest.getId(),
                null,
                null,
                customerUser.getId(),
                "Customer submitted service request " + serviceRequest.getRequestReference(),
                "{\"totalAmount\": 2799.00}"
        );
        auditEventRepository.save(event1);

        MarketplaceAuditEvent event2 = new MarketplaceAuditEvent(
                MarketplaceEventType.WORKSHOPS_MATCHED,
                serviceRequest.getId(),
                null,
                null,
                null,
                "Matched 2 eligible workshops within 15km radius",
                "{\"matchedCount\": 2}"
        );
        auditEventRepository.save(event2);

        MarketplaceAuditEvent event3 = new MarketplaceAuditEvent(
                MarketplaceEventType.OPPORTUNITY_ACCEPTED,
                serviceRequest.getId(),
                opportunityA.getId(),
                workshopA.getId(),
                partnerUser.getId(),
                "Apex Motors accepted opportunity #" + opportunityA.getId(),
                "{\"fee\": 99.00}"
        );
        auditEventRepository.save(event3);

        MarketplaceAuditEvent event4 = new MarketplaceAuditEvent(
                MarketplaceEventType.DETAILS_UNLOCKED,
                serviceRequest.getId(),
                opportunityA.getId(),
                workshopA.getId(),
                partnerUser.getId(),
                "Customer contact details unlocked for Apex Motors following successful fee payment",
                null
        );
        auditEventRepository.save(event4);
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/summary - Returns valid operational KPIs")
    void getMarketplaceSummary_AsAdmin_ReturnsValidMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalServiceRequests", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.activeRequests", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalOpportunities", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.totalAcceptanceRevenue", notNullValue()))
                .andExpect(jsonPath("$.data.totalRefunds", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests - As Admin returns paged list")
    void getServiceRequests_AsAdmin_ReturnsPagedList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[?(@.requestReference == '" + serviceRequest.getRequestReference() + "')].customerName",
                        hasItem(customerUser.getName())))
                .andExpect(jsonPath("$.data.content[?(@.requestReference == '" + serviceRequest.getRequestReference() + "')].assignedWorkshopName",
                        hasItem(workshopA.getBusinessName())));
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests - With search filter matches reference")
    void getServiceRequests_WithSearchFilter_ReturnsFilteredResults() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", serviceRequest.getRequestReference()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].requestReference").value(serviceRequest.getRequestReference()))
                .andExpect(jsonPath("$.data.content[0].city").value("New Delhi"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests - With status filter")
    void getServiceRequests_WithStatusFilter_ReturnsFilteredResults() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests - With city filter")
    void getServiceRequests_WithCityFilter_ReturnsFilteredResults() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("city", "New Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests - As Customer returns 403 Forbidden")
    void getServiceRequests_AsCustomer_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests - As Partner returns 403 Forbidden")
    void getServiceRequests_AsPartner_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests")
                        .header("Authorization", "Bearer " + partnerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests - Unauthenticated returns 401 Unauthorized")
    void getServiceRequests_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests/{id} - Returns complete 360 dossier")
    void getServiceRequestDetail_AsAdmin_ReturnsComplete360View() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests/" + serviceRequest.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(serviceRequest.getId()))
                .andExpect(jsonPath("$.data.requestReference").value(serviceRequest.getRequestReference()))
                .andExpect(jsonPath("$.data.customerName").value(customerUser.getName()))
                .andExpect(jsonPath("$.data.customerPhone").value(customerUser.getPhone()))
                .andExpect(jsonPath("$.data.customerEmail").value(customerUser.getEmail()))
                .andExpect(jsonPath("$.data.vehicleMake").value(vehicle.getMake()))
                .andExpect(jsonPath("$.data.vehicleModel").value(vehicle.getModel()))
                .andExpect(jsonPath("$.data.vehicleRegistrationNumber").value(vehicle.getRegistrationNumber()))
                .andExpect(jsonPath("$.data.assignedWorkshopName").value(workshopA.getBusinessName()))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].serviceNameSnapshot").value(serviceCatalog.getName()))
                .andExpect(jsonPath("$.data.totalOpportunities").value(2))
                .andExpect(jsonPath("$.data.totalPaidAmount").value(99.00));
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests/{id} - Non-existent ID returns 404")
    void getServiceRequestDetail_NonExistent_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests/9999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests/{id}/opportunities - Returns all attempts with status")
    void getServiceRequestOpportunities_ReturnsAllAttemptsWithStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests/" + serviceRequest.getId() + "/opportunities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[?(@.workshopName == '" + workshopA.getBusinessName() + "')].assigned", hasItem(true)))
                .andExpect(jsonPath("$.data[?(@.workshopName == '" + workshopA.getBusinessName() + "')].customerDetailsUnlocked", hasItem(true)))
                .andExpect(jsonPath("$.data[?(@.workshopName == '" + workshopB.getBusinessName() + "')].status", hasItem("LOST")));
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests/{id}/payments - Returns payments with masked secrets")
    void getServiceRequestPayments_ReturnsPaymentsWithMaskedSecrets() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests/" + serviceRequest.getId() + "/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].amount").value(99.00))
                .andExpect(jsonPath("$.data[0].paymentMethod").value("WALLET"))
                .andExpect(jsonPath("$.data[0].razorpaySignature").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests/{id}/refunds - Returns refund history")
    void getServiceRequestRefunds_ReturnsRefundHistory() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests/" + serviceRequest.getId() + "/refunds")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].workshopName").value(workshopB.getBusinessName()))
                .andExpect(jsonPath("$.data[0].refundAmount").value(99.00))
                .andExpect(jsonPath("$.data[0].refundReason").value("OPPORTUNITY_ALREADY_ASSIGNED"))
                .andExpect(jsonPath("$.data[0].refundStatus").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/marketplace/service-requests/{id}/timeline - Returns chronological audit trail")
    void getServiceRequestTimeline_ReturnsChronologicalAuditTrail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests/" + serviceRequest.getId() + "/timeline")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].eventType").value("REQUEST_CREATED"))
                .andExpect(jsonPath("$.data[1].eventType").value("WORKSHOPS_MATCHED"))
                .andExpect(jsonPath("$.data[2].eventType").value("OPPORTUNITY_ACCEPTED"))
                .andExpect(jsonPath("$.data[2].workshopName").value(workshopA.getBusinessName()))
                .andExpect(jsonPath("$.data[3].eventType").value("DETAILS_UNLOCKED"));
    }

    @Test
    @DisplayName("Transferred opportunity is properly represented as separate lifecycle")
    void transferredOpportunity_IsRepresentedAsSeparateLifecycle() throws Exception {
        // Create an opportunity in transferred status
        LeadOpportunity transferOpp = new LeadOpportunity();
        transferOpp.setServiceRequest(serviceRequest);
        transferOpp.setWorkshop(workshopB);
        transferOpp.setFeeSnapshot(new BigDecimal("99.00"));
        transferOpp.setStatus(OpportunityStatus.TRANSFERRED);
        transferOpp.setTransferredAt(LocalDateTime.now());
        transferOpp.setTransferReason("PARTS_UNAVAILABLE: Brake rotor out of stock");
        leadOpportunityRepository.save(transferOpp);

        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests/" + serviceRequest.getId() + "/opportunities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.status == 'TRANSFERRED')].transferReason",
                        hasItem(containsString("PARTS_UNAVAILABLE"))));
    }

    @Test
    @DisplayName("Race condition loss and refund is visible in admin audit")
    void raceConditionLoss_DisplaysLostOpportunityAndRefundAudit() throws Exception {
        mockMvc.perform(get("/api/v1/admin/marketplace/service-requests/" + serviceRequest.getId() + "/opportunities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.status == 'LOST')].refundReason",
                        hasItem("OPPORTUNITY_ALREADY_ASSIGNED")))
                .andExpect(jsonPath("$.data[?(@.status == 'LOST')].refundStatus",
                        hasItem("SUCCESS")));
    }
}

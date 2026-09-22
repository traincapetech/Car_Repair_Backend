package com.carservice.backend;

import com.carservice.backend.common.notification.dto.NotificationResponse;
import com.carservice.backend.common.notification.entity.Notification;
import com.carservice.backend.common.notification.enums.NotificationType;
import com.carservice.backend.common.notification.repository.NotificationRepository;
import com.carservice.backend.common.notification.NotificationService;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class NotificationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private User customerA;
    private User customerB;
    private String tokenCustomerA;
    private String tokenCustomerB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        customerA = new User();
        customerA.setName("Customer Alpha");
        customerA.setEmail("alpha_" + uniqueSuffix + "@test.com");
        customerA.setPhone("910000" + uniqueSuffix.substring(0, 4));
        customerA.setPassword(passwordEncoder.encode("Pass@12345"));
        customerA.setRole(UserRole.CUSTOMER);
        customerA.setIsActive(true);
        customerA = userRepository.save(customerA);

        customerB = new User();
        customerB.setName("Customer Beta");
        customerB.setEmail("beta_" + uniqueSuffix + "@test.com");
        customerB.setPhone("920000" + uniqueSuffix.substring(0, 4));
        customerB.setPassword(passwordEncoder.encode("Pass@12345"));
        customerB.setRole(UserRole.CUSTOMER);
        customerB.setIsActive(true);
        customerB = userRepository.save(customerB);

        tokenCustomerA = jwtService.generateAccessToken(customerA);
        tokenCustomerB = jwtService.generateAccessToken(customerB);
    }

    @Test
    @DisplayName("Notification creation and persistence via NotificationService")
    void testNotificationCreationAndPersistence() {
        NotificationResponse response = notificationService.sendNotification(
                customerA.getId(),
                "CUSTOMER",
                NotificationType.BOOKING_CREATED,
                "Booking Confirmed",
                "Your booking #BK-100 has been confirmed.",
                "BOOKING",
                100L,
                Map.of("bookingRef", "BK-100")
        );

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Booking Confirmed", response.getTitle());
        assertEquals("BOOKING_CREATED", response.getType());
        assertFalse(response.getIsRead());

        Notification inDb = notificationRepository.findById(response.getId()).orElse(null);
        assertNotNull(inDb);
        assertEquals(customerA.getId(), inDb.getRecipientId());
        assertEquals("BOOKING", inDb.getEntityType());
        assertEquals(100L, inDb.getEntityId());
        assertFalse(inDb.getIsRead());
    }

    @Test
    @DisplayName("User notification isolation: Customer A only sees Customer A's notifications")
    void testUserNotificationIsolation() throws Exception {
        // Create 2 notifications for Customer A
        notificationService.sendNotification(
                customerA.getId(), "CUSTOMER", NotificationType.BOOKING_CREATED,
                "Alpha Notif 1", "Message 1", "BOOKING", 1L, null);
        notificationService.sendNotification(
                customerA.getId(), "CUSTOMER", NotificationType.BOOKING_ACCEPTED,
                "Alpha Notif 2", "Message 2", "BOOKING", 2L, null);

        // Create 1 notification for Customer B
        notificationService.sendNotification(
                customerB.getId(), "CUSTOMER", NotificationType.PAYMENT_SUCCESS,
                "Beta Notif 1", "Beta Message", "PAYMENT", 3L, null);

        // Fetch Customer A's notifications
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenCustomerA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].title", containsInAnyOrder("Alpha Notif 1", "Alpha Notif 2")))
                .andExpect(jsonPath("$.data.content[*].title", not(hasItem("Beta Notif 1"))));

        // Fetch Customer B's notifications
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenCustomerB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Beta Notif 1"))
                .andExpect(jsonPath("$.data.content[*].title", not(hasItem("Alpha Notif 1"))));
    }

    @Test
    @DisplayName("Unread count API returns accurate count of unread items")
    void testUnreadCountApi() throws Exception {
        notificationService.sendNotification(customerA.getId(), "CUSTOMER", NotificationType.BOOKING_CREATED, "N1", "M1", null, null, null);
        notificationService.sendNotification(customerA.getId(), "CUSTOMER", NotificationType.BOOKING_ACCEPTED, "N2", "M2", null, null, null);
        notificationService.sendNotification(customerA.getId(), "CUSTOMER", NotificationType.BOOKING_COMPLETED, "N3", "M3", null, null, null);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenCustomerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    @DisplayName("Mark single notification as read updates read status and timestamp")
    void testMarkNotificationAsRead() throws Exception {
        NotificationResponse created = notificationService.sendNotification(
                customerA.getId(), "CUSTOMER", NotificationType.BOOKING_CREATED,
                "Test Unread", "Unread message", null, null, null);

        mockMvc.perform(put("/api/v1/notifications/" + created.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenCustomerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        Notification updated = notificationRepository.findById(created.getId()).orElseThrow();
        assertTrue(updated.getIsRead());
        assertNotNull(updated.getReadAt());
    }

    @Test
    @DisplayName("IDOR Prevention: User B cannot mark User A's notification as read (returns 403 Forbidden)")
    void testIdorPreventionMarkAsRead() throws Exception {
        NotificationResponse createdForAlpha = notificationService.sendNotification(
                customerA.getId(), "CUSTOMER", NotificationType.BOOKING_CREATED,
                "Alpha Secret", "Confidential message", null, null, null);

        // Beta attempts to mark Alpha's notification as read
        mockMvc.perform(put("/api/v1/notifications/" + createdForAlpha.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenCustomerB))
                .andExpect(status().isForbidden());

        // Verify Alpha's notification is still unread
        Notification notif = notificationRepository.findById(createdForAlpha.getId()).orElseThrow();
        assertFalse(notif.getIsRead(), "Notification should remain unread after unauthorized attempt");
    }

    @Test
    @DisplayName("Mark all as read marks only the current user's notifications as read")
    void testMarkAllAsRead() throws Exception {
        notificationService.sendNotification(customerA.getId(), "CUSTOMER", NotificationType.BOOKING_CREATED, "A1", "M", null, null, null);
        notificationService.sendNotification(customerA.getId(), "CUSTOMER", NotificationType.BOOKING_ACCEPTED, "A2", "M", null, null, null);

        // Customer B also has an unread notification
        NotificationResponse betaNotif = notificationService.sendNotification(
                customerB.getId(), "CUSTOMER", NotificationType.PAYMENT_SUCCESS, "B1", "M", null, null, null);

        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenCustomerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.updatedCount").value(2));

        // Customer A unread count should be 0
        assertEquals(0, notificationRepository.countByRecipientIdAndIsReadFalse(customerA.getId()));

        // Customer B unread notification should NOT be affected
        Notification bInDb = notificationRepository.findById(betaNotif.getId()).orElseThrow();
        assertFalse(bInDb.getIsRead(), "Customer B's notification must not be marked as read by Customer A");
    }

    @Test
    @DisplayName("Unauthenticated request to notifications endpoint returns 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/notifications/1/read"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }
}

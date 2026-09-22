package com.carservice.backend;

import com.carservice.backend.admin.service.AdminWorkshopService;
import com.carservice.backend.common.notification.entity.Notification;
import com.carservice.backend.common.notification.enums.NotificationType;
import com.carservice.backend.common.notification.repository.NotificationRepository;
import com.carservice.backend.common.notification.NotificationService;
import com.carservice.backend.marketplace.dto.WorkshopRegistrationRequest;
import com.carservice.backend.marketplace.dto.WorkshopRegistrationResponse;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.marketplace.service.WorkshopRegistrationService;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BusinessEventNotificationIntegrationTest {

    @Autowired
    private WorkshopRegistrationService registrationService;

    @Autowired
    private AdminWorkshopService adminWorkshopService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminUser = new User();
        adminUser.setName("Super Admin");
        adminUser.setEmail("admin_" + suffix + "@carservice.com");
        adminUser.setPhone("989900" + suffix.substring(0, 4));
        adminUser.setPassword(passwordEncoder.encode("Admin@12345"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
    }

    @Test
    @DisplayName("Workshop registration triggers WORKSHOP_REGISTERED notification for Admin")
    void testWorkshopRegistrationAdminNotification() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        WorkshopRegistrationRequest req = new WorkshopRegistrationRequest();
        req.setBusinessName("Apex Motors " + suffix);
        req.setOwnerName("John Apex");
        req.setEmail("apex_" + suffix + "@test.com");
        req.setPhone("998811" + suffix.substring(0, 4));
        req.setPassword("Apex@12345");
        req.setAddress("Industrial Area Phase 2");
        req.setCity("New Delhi");
        req.setState("Delhi");
        req.setPincode("110020");
        req.setLatitude(new BigDecimal("28.5355"));
        req.setLongitude(new BigDecimal("77.2631"));
        req.setServiceRadiusKm(new BigDecimal("15.0"));

        long adminNotifsBefore = notificationRepository.countByRecipientIdAndIsReadFalse(adminUser.getId());

        WorkshopRegistrationResponse registered = registrationService.registerWorkshop(req);
        assertNotNull(registered);

        long adminNotifsAfter = notificationRepository.countByRecipientIdAndIsReadFalse(adminUser.getId());
        assertTrue(adminNotifsAfter > adminNotifsBefore, "Admin must receive in-app notification when a new workshop registers");

        List<Notification> adminNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(adminUser.getId(), null).getContent();
        boolean found = adminNotifs.stream().anyMatch(n -> n.getType() == NotificationType.WORKSHOP_REGISTERED
                && n.getMessage().contains("Apex Motors"));
        assertTrue(found, "Admin notification must match WORKSHOP_REGISTERED type and contain workshop business name");
    }

    @Test
    @DisplayName("Admin workshop approval triggers WORKSHOP_APPROVED notification for Workshop User")
    void testAdminWorkshopApprovalNotification() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        WorkshopRegistrationRequest req = new WorkshopRegistrationRequest();
        req.setBusinessName("Speedy Care " + suffix);
        req.setOwnerName("Bob Speedy");
        req.setEmail("speedy_" + suffix + "@test.com");
        req.setPhone("998822" + suffix.substring(0, 4));
        req.setPassword("Speedy@12345");
        req.setAddress("Sector 18");
        req.setCity("Noida");
        req.setState("Uttar Pradesh");
        req.setPincode("201301");
        req.setLatitude(new BigDecimal("28.5700"));
        req.setLongitude(new BigDecimal("77.3200"));
        req.setServiceRadiusKm(new BigDecimal("10.0"));

        WorkshopRegistrationResponse reg = registrationService.registerWorkshop(req);
        Long workshopId = reg.getWorkshopId();
        Long workshopUserId = reg.getUserId();

        // Approve workshop
        adminWorkshopService.approveWorkshop(workshopId, adminUser);

        // Verify Workshop user received WORKSHOP_APPROVED notification
        List<Notification> workshopNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(workshopUserId, null).getContent();
        boolean foundApproval = workshopNotifs.stream().anyMatch(n -> n.getType() == NotificationType.WORKSHOP_APPROVED
                && n.getTitle().contains("Approved"));
        assertTrue(foundApproval, "Workshop partner user must receive WORKSHOP_APPROVED notification on admin verification");
    }

    @Test
    @DisplayName("Admin workshop rejection triggers WORKSHOP_REJECTED notification with reason for Workshop User")
    void testAdminWorkshopRejectionNotification() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        WorkshopRegistrationRequest req = new WorkshopRegistrationRequest();
        req.setBusinessName("Shady Garage " + suffix);
        req.setOwnerName("Sam Shady");
        req.setEmail("shady_" + suffix + "@test.com");
        req.setPhone("998833" + suffix.substring(0, 4));
        req.setPassword("Shady@12345");
        req.setAddress("Back Alley 5");
        req.setCity("Delhi");
        req.setState("Delhi");
        req.setPincode("110001");
        req.setLatitude(new BigDecimal("28.6000"));
        req.setLongitude(new BigDecimal("77.2000"));

        WorkshopRegistrationResponse reg = registrationService.registerWorkshop(req);
        Long workshopId = reg.getWorkshopId();
        Long workshopUserId = reg.getUserId();

        // Reject workshop
        String rejectReason = "Incomplete fire safety and trade license documentation";
        adminWorkshopService.rejectWorkshop(workshopId, rejectReason, adminUser);

        // Verify Workshop user received WORKSHOP_REJECTED notification
        List<Notification> workshopNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(workshopUserId, null).getContent();
        boolean foundRejection = workshopNotifs.stream().anyMatch(n -> n.getType() == NotificationType.WORKSHOP_REJECTED
                && n.getMessage().contains(rejectReason));
        assertTrue(foundRejection, "Workshop partner user must receive WORKSHOP_REJECTED notification containing rejection reason");
    }

    @Test
    @DisplayName("Nearby Workshop Requirement: Only matched nearby workshops receive booking notification (no global broadcast)")
    void testNearbyWorkshopIsolationNotification() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Create Workshop A (Nearby in Delhi - eligible)
        User userA = createUser("Workshop User A", "wa_" + suffix + "@test.com", "9111" + suffix.substring(0, 6));
        Workshop workshopA = createWorkshop(userA, "Delhi Workshop A", "New Delhi", "28.6139", "77.2090");

        // Create Workshop B (Nearby in Gurgaon - eligible)
        User userB = createUser("Workshop User B", "wb_" + suffix + "@test.com", "9222" + suffix.substring(0, 6));
        Workshop workshopB = createWorkshop(userB, "Gurgaon Workshop B", "Gurgaon", "28.4595", "77.0266");

        // Create Workshop C (Far away in Lucknow - NOT eligible)
        User userC = createUser("Workshop User C", "wc_" + suffix + "@test.com", "9333" + suffix.substring(0, 6));
        Workshop workshopC = createWorkshop(userC, "Lucknow Workshop C", "Lucknow", "26.8467", "80.9462");

        List<Workshop> matchedNearby = List.of(workshopA, workshopB);

        // Dispatch notification strictly to matched nearby workshops
        notificationService.notifyNearbyWorkshopsOfBooking(
                matchedNearby,
                555L,
                "REQ-DELHI-555",
                "New Delhi",
                new BigDecimal("50.00")
        );

        // Assert Workshop A received notification
        List<Notification> notifsA = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userA.getId(), null).getContent();
        assertTrue(notifsA.stream().anyMatch(n -> n.getType() == NotificationType.BOOKING_CREATED && n.getMessage().contains("REQ-DELHI-555")));

        // Assert Workshop B received notification
        List<Notification> notifsB = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userB.getId(), null).getContent();
        assertTrue(notifsB.stream().anyMatch(n -> n.getType() == NotificationType.BOOKING_CREATED && n.getMessage().contains("REQ-DELHI-555")));

        // Assert Workshop C (unmatched / far away) received NOTHING (NO global broadcast)
        List<Notification> notifsC = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userC.getId(), null).getContent();
        assertTrue(notifsC.isEmpty(), "Unmatched far-away workshop MUST NOT receive any notification for this booking");
    }

    private User createUser(String name, String email, String phone) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("Pass@12345"));
        user.setRole(UserRole.PARTNER);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    private Workshop createWorkshop(User user, String businessName, String city, String lat, String lng) {
        Workshop w = new Workshop();
        w.setUser(user);
        w.setBusinessName(businessName);
        w.setOwnerName(user.getName());
        w.setEmail(user.getEmail());
        w.setPhone(user.getPhone());
        w.setAddress("Sample Street");
        w.setCity(city);
        w.setState("State");
        w.setPincode("110001");
        w.setLatitude(new BigDecimal(lat));
        w.setLongitude(new BigDecimal(lng));
        w.setServiceRadiusKm(new BigDecimal("25.0"));
        w.setVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        w.setIsActive(true);
        return workshopRepository.save(w);
    }
}

package com.carservice.backend.common.notification;

import com.carservice.backend.common.email.BrevoEmailService;
import com.carservice.backend.common.notification.dto.NotificationResponse;
import com.carservice.backend.common.notification.entity.Notification;
import com.carservice.backend.common.notification.enums.NotificationType;
import com.carservice.backend.common.notification.repository.NotificationRepository;
import com.carservice.backend.common.realtime.RealtimeEvents;
import com.carservice.backend.common.realtime.SocketIOService;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Production-ready notification and communication service.
 * Handles persistent in-app notifications, Socket.IO realtime emissions,
 * and Brevo transactional email delivery.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SocketIOService socketIOService;
    private final BrevoEmailService emailService;
    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            SocketIOService socketIOService,
            BrevoEmailService emailService,
            UserRepository userRepository,
            WorkshopRepository workshopRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.socketIOService = socketIOService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.workshopRepository = workshopRepository;
    }

    /**
     * Core notification dispatcher:
     * 1. Persists Notification to database
     * 2. Emits 'notification:new' event to user's private Socket.IO room
     * 3. Emits domain-specific realtime event (e.g. booking:accepted)
     * 4. Optionally dispatches transactional email (non-blocking)
     */
    @Transactional
    public NotificationResponse sendNotification(
            Long recipientUserId,
            String recipientRole,
            NotificationType type,
            String title,
            String message,
            String entityType,
            Long entityId,
            Map<String, Object> metadata
    ) {
        log.info("[NotificationService] Dispatching to User #{}: type={}, title=\"{}\"", recipientUserId, type, title);

        // 1. Persist notification to database
        Notification notification = new Notification(
                recipientUserId,
                recipientRole != null ? recipientRole : "CUSTOMER",
                type,
                title,
                message,
                entityType,
                entityId
        );
        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = NotificationResponse.fromEntity(saved);

        // 2. Emit realtime notification event to recipient private room
        try {
            socketIOService.sendToUser(recipientUserId, RealtimeEvents.NOTIFICATION_NEW, response);

            // Also emit domain event if applicable
            String domainEvent = mapToDomainEvent(type);
            if (domainEvent != null) {
                Map<String, Object> eventPayload = new HashMap<>();
                if (metadata != null) {
                    eventPayload.putAll(metadata);
                }
                eventPayload.put("notification", response);
                eventPayload.put("entityId", entityId);
                eventPayload.put("entityType", entityType);
                socketIOService.sendToUser(recipientUserId, domainEvent, eventPayload);
            }
        } catch (Exception ex) {
            log.error("[NotificationService] Socket emission error for user #{}: {}", recipientUserId, ex.getMessage());
        }

        return response;
    }

    /**
     * Backward-compatible helper for customer notifications.
     */
    @Transactional
    public void notifyCustomer(Long customerUserId, String eventType, String title, String message, Map<String, Object> metadata) {
        NotificationType type = resolveNotificationType(eventType);
        Long entityId = extractLong(metadata, "requestId", "bookingId", "entityId");
        String entityType = metadata != null && metadata.containsKey("entityType") ? (String) metadata.get("entityType") : "BOOKING";

        NotificationResponse response = sendNotification(
                customerUserId,
                "CUSTOMER",
                type,
                title,
                message,
                entityType,
                entityId,
                metadata
        );

        // Send customer email if applicable
        try {
            userRepository.findById(customerUserId).ifPresent(user -> {
                sendCustomerEmailForEvent(user, type, title, message, metadata);
            });
        } catch (Exception ex) {
            log.warn("[NotificationService] Customer email dispatch exception: {}", ex.getMessage());
        }
    }

    /**
     * Backward-compatible helper for workshop notifications.
     */
    @Transactional
    public void notifyWorkshop(Long workshopId, String eventType, String title, String message, Map<String, Object> metadata) {
        Workshop workshop = workshopRepository.findById(workshopId).orElse(null);
        if (workshop == null || workshop.getUser() == null) {
            log.warn("[NotificationService] Cannot notify workshop #{}: workshop or workshop user not found", workshopId);
            return;
        }

        Long workshopUserId = workshop.getUser().getId();
        NotificationType type = resolveNotificationType(eventType);
        Long entityId = extractLong(metadata, "requestId", "opportunityId", "jobId", "entityId");

        sendNotification(
                workshopUserId,
                "PARTNER",
                type,
                title,
                message,
                "WORKSHOP_JOB",
                entityId,
                metadata
        );

        // Send workshop email if applicable
        try {
            String toEmail = workshop.getEmail() != null ? workshop.getEmail() : workshop.getUser().getEmail();
            if (toEmail != null && !toEmail.isBlank()) {
                if (type == NotificationType.BOOKING_CANCELLED) {
                    emailService.sendBookingStatusEmail(toEmail, workshop.getBusinessName(), String.valueOf(entityId), "CANCELLED", message);
                }
            }
        } catch (Exception ex) {
            log.warn("[NotificationService] Workshop email dispatch exception: {}", ex.getMessage());
        }
    }

    /**
     * Backward-compatible sendNotification by userId.
     */
    @Transactional
    public void sendNotification(Long recipientUserId, String eventType, String message, Map<String, Object> metadata) {
        NotificationType type = resolveNotificationType(eventType);
        String title = formatTitleFromEventType(eventType);
        Long entityId = extractLong(metadata, "requestId", "bookingId", "id", "entityId");
        String role = "CUSTOMER";

        Optional<User> userOpt = userRepository.findById(recipientUserId);
        if (userOpt.isPresent()) {
            role = userOpt.get().getRole().name();
        }

        sendNotification(
                recipientUserId,
                role,
                type,
                title,
                message,
                "SYSTEM",
                entityId,
                metadata
        );
    }

    /**
     * Notify Admin users (via role:ADMIN Socket.IO room, persisted admin notification, and email).
     */
    @Transactional
    public void notifyAdmin(NotificationType type, String title, String message, String entityType, Long entityId, Map<String, Object> metadata) {
        log.info("[NotificationService] Dispatching administrative alert: type={}, title=\"{}\"", type, title);

        // Find primary admin users to persist notification records for
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        if (admins.isEmpty()) {
            admins = userRepository.findByRole(UserRole.SUPER_ADMIN);
        }

        for (User admin : admins) {
            Notification notification = new Notification(
                    admin.getId(),
                    admin.getRole().name(),
                    type,
                    title,
                    message,
                    entityType,
                    entityId
            );
            Notification saved = notificationRepository.save(notification);
            NotificationResponse response = NotificationResponse.fromEntity(saved);

            try {
                socketIOService.sendToUser(admin.getId(), RealtimeEvents.NOTIFICATION_NEW, response);
            } catch (Exception ignored) {}
        }

        // Also emit to role:ADMIN room
        try {
            Map<String, Object> adminPayload = new HashMap<>();
            adminPayload.put("type", type.name());
            adminPayload.put("title", title);
            adminPayload.put("message", message);
            adminPayload.put("entityType", entityType);
            adminPayload.put("entityId", entityId);
            if (metadata != null) {
                adminPayload.putAll(metadata);
            }
            socketIOService.sendToRole("ADMIN", RealtimeEvents.NOTIFICATION_NEW, adminPayload);
        } catch (Exception ex) {
            log.warn("[NotificationService] Socket emission to role:ADMIN failed: {}", ex.getMessage());
        }

        // Email admin notification
        try {
            emailService.sendAdminNotificationEmail(title, message);
        } catch (Exception ex) {
            log.warn("[NotificationService] Admin email alert failed: {}", ex.getMessage());
        }
    }

    /**
     * Restrictive dispatch to matched nearby workshops only.
     * Guaranteed: NEVER broadcasts to all workshops or public rooms.
     */
    @Transactional
    public void notifyNearbyWorkshopsOfBooking(List<Workshop> nearbyWorkshops, Long serviceRequestId, String requestReference, String city, BigDecimal fee) {
        if (nearbyWorkshops == null || nearbyWorkshops.isEmpty()) {
            log.info("[NotificationService] No nearby workshops to notify for service request #{}", serviceRequestId);
            return;
        }

        log.info("[NotificationService] Dispatching nearby booking #{} to {} eligible workshops in city: {}",
                requestReference, nearbyWorkshops.size(), city);

        for (Workshop workshop : nearbyWorkshops) {
            if (workshop.getUser() == null) continue;
            Long workshopUserId = workshop.getUser().getId();

            String title = "New Nearby Service Opportunity";
            String msg = "A new service request (" + requestReference + ") in " + city + " matches your workshop. Claim fee: ₹" + fee;

            // Persist individual workshop notification
            Notification notification = new Notification(
                    workshopUserId,
                    "PARTNER",
                    NotificationType.BOOKING_CREATED,
                    title,
                    msg,
                    "SERVICE_REQUEST",
                    serviceRequestId
            );
            Notification saved = notificationRepository.save(notification);
            NotificationResponse response = NotificationResponse.fromEntity(saved);

            // Emit strictly to this specific workshop's private room
            try {
                socketIOService.sendToUser(workshopUserId, RealtimeEvents.NOTIFICATION_NEW, response);
                socketIOService.sendToUser(workshopUserId, RealtimeEvents.BOOKING_CREATED, Map.of(
                        "serviceRequestId", serviceRequestId,
                        "requestReference", requestReference,
                        "city", city,
                        "fee", fee
                ));
            } catch (Exception ex) {
                log.warn("[NotificationService] Socket delivery failed for workshop user #{}: {}", workshopUserId, ex.getMessage());
            }
        }
    }

    private void sendCustomerEmailForEvent(User user, NotificationType type, String title, String message, Map<String, Object> metadata) {
        String email = user.getEmail();
        String name = user.getName();
        if (email == null || email.isBlank()) return;

        switch (type) {
            case BOOKING_CREATED -> {
                String bookingRef = metadata != null && metadata.containsKey("bookingReference")
                        ? (String) metadata.get("bookingReference") : "CS-REQ";
                String date = metadata != null && metadata.containsKey("date")
                        ? String.valueOf(metadata.get("date")) : "Upcoming";
                String slot = metadata != null && metadata.containsKey("slot")
                        ? String.valueOf(metadata.get("slot")) : "Standard";
                BigDecimal amount = BigDecimal.ZERO;
                if (metadata != null && metadata.get("amount") instanceof BigDecimal bd) {
                    amount = bd;
                }
                emailService.sendBookingConfirmationEmail(email, name, bookingRef, date, slot, amount);
            }
            case BOOKING_ACCEPTED -> {
                String bookingRef = metadata != null && metadata.containsKey("requestReference")
                        ? (String) metadata.get("requestReference") : "Booking";
                emailService.sendBookingStatusEmail(email, name, bookingRef, "CONFIRMED", message);
            }
            case BOOKING_COMPLETED -> {
                String bookingRef = metadata != null && metadata.containsKey("requestReference")
                        ? (String) metadata.get("requestReference") : "Booking";
                emailService.sendBookingStatusEmail(email, name, bookingRef, "COMPLETED", message);
            }
            case BOOKING_CANCELLED -> {
                String bookingRef = metadata != null && metadata.containsKey("requestReference")
                        ? (String) metadata.get("requestReference") : "Booking";
                emailService.sendBookingStatusEmail(email, name, bookingRef, "CANCELLED", message);
            }
            default -> {}
        }
    }

    private NotificationType resolveNotificationType(String eventType) {
        if (eventType == null) return NotificationType.SYSTEM_ALERT;
        return switch (eventType.toUpperCase()) {
            case "BOOKING_CREATED", "REQUEST_CREATED", "NEW_OPPORTUNITY" -> NotificationType.BOOKING_CREATED;
            case "BOOKING_ACCEPTED", "WORKSHOP_ASSIGNED", "JOB_ASSIGNED" -> NotificationType.BOOKING_ACCEPTED;
            case "BOOKING_REJECTED", "DECLINED" -> NotificationType.BOOKING_REJECTED;
            case "BOOKING_CANCELLED", "SERVICE_CANCELLED", "JOB_CANCELLED" -> NotificationType.BOOKING_CANCELLED;
            case "BOOKING_COMPLETED", "JOB_COMPLETED" -> NotificationType.BOOKING_COMPLETED;
            case "WORKSHOP_REGISTERED" -> NotificationType.WORKSHOP_REGISTERED;
            case "WORKSHOP_APPROVED" -> NotificationType.WORKSHOP_APPROVED;
            case "WORKSHOP_REJECTED" -> NotificationType.WORKSHOP_REJECTED;
            case "PAYMENT_SUCCESS", "WALLET_TOPUP" -> NotificationType.PAYMENT_SUCCESS;
            case "PAYMENT_FAILED" -> NotificationType.PAYMENT_FAILED;
            default -> NotificationType.SYSTEM_ALERT;
        };
    }

    private String mapToDomainEvent(NotificationType type) {
        return switch (type) {
            case BOOKING_CREATED -> RealtimeEvents.BOOKING_CREATED;
            case BOOKING_ACCEPTED -> RealtimeEvents.BOOKING_ACCEPTED;
            case BOOKING_REJECTED -> RealtimeEvents.BOOKING_REJECTED;
            case BOOKING_CANCELLED -> RealtimeEvents.BOOKING_CANCELLED;
            case BOOKING_COMPLETED -> RealtimeEvents.BOOKING_COMPLETED;
            case WORKSHOP_APPROVED -> RealtimeEvents.WORKSHOP_APPROVED;
            case WORKSHOP_REJECTED -> RealtimeEvents.WORKSHOP_REJECTED;
            case PAYMENT_SUCCESS -> RealtimeEvents.PAYMENT_SUCCESS;
            case PAYMENT_FAILED -> RealtimeEvents.PAYMENT_FAILED;
            default -> null;
        };
    }

    private String formatTitleFromEventType(String eventType) {
        if (eventType == null) return "System Notification";
        String clean = eventType.replace("_", " ").toLowerCase();
        return Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }

    private Long extractLong(Map<String, Object> metadata, String... keys) {
        if (metadata == null) return null;
        for (String key : keys) {
            Object val = metadata.get(key);
            if (val instanceof Number num) {
                return num.longValue();
            } else if (val instanceof String str) {
                try {
                    return Long.parseLong(str);
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}

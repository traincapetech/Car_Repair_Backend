package com.carservice.backend.common.realtime;

/**
 * Centralized registry of all Socket.IO realtime event types.
 * Defines standard channels across Notification, Booking, Workshop, Payment,
 * and future Live Chat (ADMIN 10) domain models.
 */
public final class RealtimeEvents {

    private RealtimeEvents() {
        // Utility class
    }

    // ================= NOTIFICATION EVENTS =================
    public static final String NOTIFICATION_NEW = "notification:new";
    public static final String NOTIFICATION_READ = "notification:read";

    // ================= BOOKING EVENTS =================
    public static final String BOOKING_CREATED = "booking:created";
    public static final String BOOKING_ACCEPTED = "booking:accepted";
    public static final String BOOKING_REJECTED = "booking:rejected";
    public static final String BOOKING_CANCELLED = "booking:cancelled";
    public static final String BOOKING_COMPLETED = "booking:completed";

    // ================= WORKSHOP LIFECYCLE EVENTS =================
    public static final String WORKSHOP_APPROVED = "workshop:approved";
    public static final String WORKSHOP_REJECTED = "workshop:rejected";

    // ================= PAYMENT EVENTS =================
    public static final String PAYMENT_SUCCESS = "payment:success";
    public static final String PAYMENT_FAILED = "payment:failed";

    // ================= LIVE CHAT FOUNDATION (ADMIN 10) =================
    public static final String CHAT_QUEUE_JOIN = "chat:queue:join";
    public static final String CHAT_QUEUE_LEAVE = "chat:queue:leave";
    public static final String CHAT_ASSIGNED = "chat:assigned";
    public static final String CHAT_MESSAGE = "chat:message";
    public static final String CHAT_TYPING = "chat:typing";
    public static final String CHAT_READ = "chat:read";
    public static final String CHAT_CLOSED = "chat:closed";

    // ================= ROOM HELPERS =================
    public static String userRoom(Long userId) {
        return "user:" + userId;
    }

    public static String roleRoom(String role) {
        return "role:" + role;
    }
}

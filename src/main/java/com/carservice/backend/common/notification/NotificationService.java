package com.carservice.backend.common.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Clean notification abstraction for customer and workshop lifecycle events.
 * Ready for future WebSocket / Push / SMS / Email driver integrations.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyCustomer(Long customerUserId, String eventType, String title, String message, Map<String, Object> metadata) {
        log.info("[Notification -> Customer #{}] event={}, title=\"{}\", message=\"{}\", meta={}",
                customerUserId, eventType, title, message, metadata);
    }

    public void notifyWorkshop(Long workshopId, String eventType, String title, String message, Map<String, Object> metadata) {
        log.info("[Notification -> Workshop #{}] event={}, title=\"{}\", message=\"{}\", meta={}",
                workshopId, eventType, title, message, metadata);
    }

    public void sendNotification(Long recipientUserId, String eventType, String message, Map<String, Object> metadata) {
        log.info("[Notification -> User #{}] event={}, message=\"{}\", meta={}",
                recipientUserId, eventType, message, metadata);
    }
}

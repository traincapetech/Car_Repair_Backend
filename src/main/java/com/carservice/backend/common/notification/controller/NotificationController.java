package com.carservice.backend.common.notification.controller;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.common.notification.dto.NotificationResponse;
import com.carservice.backend.common.notification.entity.Notification;
import com.carservice.backend.common.notification.repository.NotificationRepository;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));

        Page<Notification> notificationPage;
        if (unreadOnly) {
            notificationPage = notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(currentUser.getId(), pageable);
        } else {
            notificationPage = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        }

        long unreadCount = notificationRepository.countByRecipientIdAndIsReadFalse(currentUser.getId());
        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(NotificationResponse::fromEntity)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("page", notificationPage.getNumber());
        result.put("size", notificationPage.getSize());
        result.put("totalElements", notificationPage.getTotalElements());
        result.put("totalPages", notificationPage.getTotalPages());
        result.put("unreadCount", unreadCount);

        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", result));
    }

    @GetMapping("/unread-count")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        long unreadCount = notificationRepository.countByRecipientIdAndIsReadFalse(currentUser.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", unreadCount);
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved successfully", result));
    }

    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        // IDOR protection: only recipient can mark notification as read
        if (!notification.getRecipientId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to modify this notification");
        }

        notification.markAsRead();
        Notification saved = notificationRepository.save(notification);

        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", NotificationResponse.fromEntity(saved)));
    }

    @PutMapping("/read-all")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead(
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        int updatedCount = notificationRepository.markAllAsRead(currentUser.getId(), LocalDateTime.now());

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", updatedCount);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", result));
    }
}

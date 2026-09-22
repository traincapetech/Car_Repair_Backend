package com.carservice.backend.common.realtime;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class SocketIOService {

    private static final Logger log = LoggerFactory.getLogger(SocketIOService.class);

    private final SocketIOServer server;

    @Value("${socketio.enabled:true}")
    private boolean socketIoEnabled;

    public SocketIOService(SocketIOServer server) {
        this.server = server;
    }

    @PostConstruct
    public void start() {
        if (!socketIoEnabled) {
            log.info("[SocketIOService] Socket.IO server is disabled by configuration");
            return;
        }

        // Connection Handler
        server.addConnectListener(client -> {
            try {
                Long userId = client.get("userId");
                String role = client.get("role");
                String email = client.get("email");

                if (userId != null) {
                    String userRoom = RealtimeEvents.userRoom(userId);
                    client.joinRoom(userRoom);
                    log.info("[SocketIO] Client {} joined private room '{}'", client.getSessionId(), userRoom);

                    // Join Role Rooms
                    if ("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role) || "OPERATIONS_ADMIN".equalsIgnoreCase(role)) {
                        client.joinRoom(RealtimeEvents.roleRoom("ADMIN"));
                        log.info("[SocketIO] Client {} joined role room '{}'", client.getSessionId(), RealtimeEvents.roleRoom("ADMIN"));
                    } else if ("PARTNER".equalsIgnoreCase(role) || "WORKSHOP_OWNER".equalsIgnoreCase(role) || "WORKSHOP_STAFF".equalsIgnoreCase(role)) {
                        client.joinRoom(RealtimeEvents.roleRoom("WORKSHOP"));
                        log.info("[SocketIO] Client {} joined role room '{}'", client.getSessionId(), RealtimeEvents.roleRoom("WORKSHOP"));
                    }
                } else {
                    log.warn("[SocketIO] Connected client {} lacks userId session store, disconnecting", client.getSessionId());
                    client.disconnect();
                }
            } catch (Exception ex) {
                log.error("[SocketIO] Error in connection listener: {}", ex.getMessage());
            }
        });

        // Disconnection Handler
        server.addDisconnectListener(client -> {
            Long userId = client.get("userId");
            log.info("[SocketIO] Client disconnected: sessionId={}, userId={}", client.getSessionId(), userId);
        });

        // ================= ADMIN 10 LIVE CHAT FOUNDATION LISTENERS =================
        server.addEventListener(RealtimeEvents.CHAT_QUEUE_JOIN, Map.class, (client, data, ackSender) -> {
            Long userId = client.get("userId");
            log.info("[SocketIO:Chat] User #{} requested to join support queue with meta: {}", userId, data);
            client.joinRoom("chat:queue");
            if (ackSender.isAckRequested()) {
                ackSender.sendAckData(Map.of("status", "QUEUED", "message", "Waiting for available support agent"));
            }
        });

        server.addEventListener(RealtimeEvents.CHAT_QUEUE_LEAVE, Map.class, (client, data, ackSender) -> {
            Long userId = client.get("userId");
            log.info("[SocketIO:Chat] User #{} left support queue", userId);
            client.leaveRoom("chat:queue");
            if (ackSender.isAckRequested()) {
                ackSender.sendAckData(Map.of("status", "LEFT"));
            }
        });

        server.addEventListener(RealtimeEvents.CHAT_TYPING, Map.class, (client, data, ackSender) -> {
            String roomId = (String) data.get("roomId");
            if (roomId != null && !roomId.isBlank()) {
                server.getRoomOperations(roomId).sendEvent(RealtimeEvents.CHAT_TYPING, data);
            }
        });

        try {
            server.start();
            log.info("[SocketIOService] Netty-SocketIO server started successfully on port {}", server.getConfiguration().getPort());
        } catch (Exception ex) {
            log.error("[SocketIOService] Failed to start Socket.IO server: {}", ex.getMessage(), ex);
        }
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            try {
                log.info("[SocketIOService] Stopping Socket.IO server...");
                server.stop();
                log.info("[SocketIOService] Socket.IO server stopped cleanly");
            } catch (Exception ex) {
                log.warn("[SocketIOService] Exception while stopping Socket.IO server: {}", ex.getMessage());
            }
        }
    }

    /**
     * Send event strictly to a specific user's private room.
     */
    public void sendToUser(Long userId, String event, Object data) {
        if (userId == null) return;
        String room = RealtimeEvents.userRoom(userId);
        log.debug("[SocketIO] Emitting event '{}' to room '{}'", event, room);
        server.getRoomOperations(room).sendEvent(event, data);
    }

    /**
     * Send event to multiple specific users (e.g. matched nearby workshops).
     */
    public void sendToUsers(Collection<Long> userIds, String event, Object data) {
        if (userIds == null || userIds.isEmpty()) return;
        for (Long userId : userIds) {
            sendToUser(userId, event, data);
        }
    }

    /**
     * Send event to a role room (e.g. role:ADMIN, role:WORKSHOP).
     */
    public void sendToRole(String role, String event, Object data) {
        if (role == null) return;
        String room = RealtimeEvents.roleRoom(role);
        log.debug("[SocketIO] Emitting event '{}' to role room '{}'", event, room);
        server.getRoomOperations(room).sendEvent(event, data);
    }

    /**
     * Helper to send event to all administrators.
     */
    public void sendToAllAdmins(String event, Object data) {
        sendToRole("ADMIN", event, data);
    }

    /**
     * Broadcast to all connected sockets.
     */
    public void broadcast(String event, Object data) {
        log.debug("[SocketIO] Broadcasting event '{}'", event);
        server.getBroadcastOperations().sendEvent(event, data);
    }

    public SocketIOServer getServer() {
        return server;
    }
}

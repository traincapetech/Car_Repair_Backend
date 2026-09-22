package com.carservice.backend;

import com.carservice.backend.common.realtime.RealtimeEvents;
import com.carservice.backend.common.realtime.SocketIOConfig;
import com.carservice.backend.common.realtime.SocketIOService;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.HandshakeData;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class SocketIORealtimeIntegrationTest {

    @Autowired
    private SocketIOService socketIOService;

    @Autowired
    private SocketIOConfig socketIOConfig;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("SocketIOService and SocketIOServer are initialized and running")
    void testSocketIOServiceRunning() {
        assertNotNull(socketIOService);
        assertNotNull(socketIOService.getServer());
    }

    @Test
    @DisplayName("RealtimeEvents registry contains all required production channels")
    void testRealtimeEventsRegistry() {
        assertEquals("notification:new", RealtimeEvents.NOTIFICATION_NEW);
        assertEquals("notification:read", RealtimeEvents.NOTIFICATION_READ);
        assertEquals("booking:created", RealtimeEvents.BOOKING_CREATED);
        assertEquals("booking:accepted", RealtimeEvents.BOOKING_ACCEPTED);
        assertEquals("booking:rejected", RealtimeEvents.BOOKING_REJECTED);
        assertEquals("booking:cancelled", RealtimeEvents.BOOKING_CANCELLED);
        assertEquals("booking:completed", RealtimeEvents.BOOKING_COMPLETED);
        assertEquals("workshop:approved", RealtimeEvents.WORKSHOP_APPROVED);
        assertEquals("workshop:rejected", RealtimeEvents.WORKSHOP_REJECTED);
        assertEquals("payment:success", RealtimeEvents.PAYMENT_SUCCESS);
        assertEquals("payment:failed", RealtimeEvents.PAYMENT_FAILED);

        // Live Chat Foundation (ADMIN 10)
        assertEquals("chat:queue:join", RealtimeEvents.CHAT_QUEUE_JOIN);
        assertEquals("chat:queue:leave", RealtimeEvents.CHAT_QUEUE_LEAVE);
        assertEquals("chat:assigned", RealtimeEvents.CHAT_ASSIGNED);
        assertEquals("chat:message", RealtimeEvents.CHAT_MESSAGE);
        assertEquals("chat:typing", RealtimeEvents.CHAT_TYPING);
        assertEquals("chat:read", RealtimeEvents.CHAT_READ);
        assertEquals("chat:closed", RealtimeEvents.CHAT_CLOSED);

        assertEquals("user:42", RealtimeEvents.userRoom(42L));
        assertEquals("role:ADMIN", RealtimeEvents.roleRoom("ADMIN"));
        assertEquals("role:WORKSHOP", RealtimeEvents.roleRoom("WORKSHOP"));
    }

    @Test
    @DisplayName("Handshake authorization listener approves valid JWT token and extracts session params")
    void testHandshakeWithValidToken() {
        User user = new User();
        user.setId(888L);
        user.setEmail("socket_user@test.com");
        user.setRole(UserRole.CUSTOMER);

        String token = jwtService.generateAccessToken(user);

        HandshakeData mockHandshake = mock(HandshakeData.class);
        when(mockHandshake.getSingleUrlParam("token")).thenReturn(token);
        when(mockHandshake.getHttpHeaders()).thenReturn(new io.netty.handler.codec.http.DefaultHttpHeaders());

        AuthorizationResult result = socketIOConfig.socketIOServer()
                .getConfiguration()
                .getAuthorizationListener()
                .getAuthorizationResult(mockHandshake);

        assertTrue(result.isAuthorized(), "Valid JWT token must be authorized");
        assertNotNull(result.getStoreParams());
        assertEquals(888L, result.getStoreParams().get("userId"));
        assertEquals("CUSTOMER", result.getStoreParams().get("role"));
        assertEquals("socket_user@test.com", result.getStoreParams().get("email"));
    }

    @Test
    @DisplayName("Handshake authorization listener rejects connection without token")
    void testHandshakeWithoutTokenRejected() {
        HandshakeData mockHandshake = mock(HandshakeData.class);
        when(mockHandshake.getSingleUrlParam("token")).thenReturn(null);
        when(mockHandshake.getHttpHeaders()).thenReturn(new io.netty.handler.codec.http.DefaultHttpHeaders());

        AuthorizationResult result = socketIOConfig.socketIOServer()
                .getConfiguration()
                .getAuthorizationListener()
                .getAuthorizationResult(mockHandshake);

        assertFalse(result.isAuthorized(), "Connection without token must be rejected");
    }

    @Test
    @DisplayName("Handshake authorization listener rejects invalid/tampered token")
    void testHandshakeWithInvalidTokenRejected() {
        HandshakeData mockHandshake = mock(HandshakeData.class);
        when(mockHandshake.getSingleUrlParam("token")).thenReturn("invalid.tampered.token");
        when(mockHandshake.getHttpHeaders()).thenReturn(new io.netty.handler.codec.http.DefaultHttpHeaders());

        AuthorizationResult result = socketIOConfig.socketIOServer()
                .getConfiguration()
                .getAuthorizationListener()
                .getAuthorizationResult(mockHandshake);

        assertFalse(result.isAuthorized(), "Tampered token must be rejected");
    }

    @Test
    @DisplayName("sendToUser and sendToRole emit events without throwing exceptions")
    void testEventEmissionSafety() {
        assertDoesNotThrow(() -> {
            socketIOService.sendToUser(123L, RealtimeEvents.NOTIFICATION_NEW, Map.of("title", "Test"));
            socketIOService.sendToUsers(List.of(1L, 2L, 3L), RealtimeEvents.BOOKING_CREATED, Map.of("ref", "BK-1"));
            socketIOService.sendToRole("ADMIN", RealtimeEvents.NOTIFICATION_NEW, Map.of("alert", "High"));
            socketIOService.sendToAllAdmins(RealtimeEvents.NOTIFICATION_NEW, Map.of("alert", "High"));
            socketIOService.broadcast(RealtimeEvents.NOTIFICATION_NEW, Map.of("announcement", "Scheduled maintenance"));
        });
    }
}

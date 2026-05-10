package com.mutuelle.config;

import com.mutuelle.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@Slf4j
public class WebSocketEventListener {

    private final ChatService chatService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(@Lazy ChatService chatService, 
                                 org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    private static final java.util.Map<String, Long> sessionUserMap = new java.util.concurrent.ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        org.springframework.messaging.Message<?> connectMsg = (org.springframework.messaging.Message<?>) headerAccessor.getHeader("simpConnectMessage");
        if (connectMsg != null) {
            StompHeaderAccessor connectAccessor = StompHeaderAccessor.wrap(connectMsg);
            String userIdStr = connectAccessor.getFirstNativeHeader("userId");
            if (userIdStr != null) {
                Long userId = Long.parseLong(userIdStr);
                String sessionId = headerAccessor.getSessionId();
                if (sessionId != null) {
                    sessionUserMap.put(sessionId, userId);
                }
                log.info("Utilisateur {} connecté (Session: {})", userId, sessionId);
                
                // Broadcast status
                java.util.Map<String, Object> update = new java.util.HashMap<>();
                update.put("userId", userId);
                update.put("status", "ONLINE");
                update.put("lastSeen", java.time.LocalDateTime.now());
                messagingTemplate.convertAndSend("/topic/user.status", update);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Long userId = sessionUserMap.remove(sessionId);
        
        if (userId != null) {
            log.info("Utilisateur {} déconnecté (Session: {})", userId, sessionId);
            
            // Broadcast status
            java.util.Map<String, Object> update = new java.util.HashMap<>();
            update.put("userId", userId);
            update.put("status", "OFFLINE");
            // Set lastSeen to 5 minutes ago so the UI immediately treats it as offline
            update.put("lastSeen", java.time.LocalDateTime.now().minusMinutes(5));
            messagingTemplate.convertAndSend("/topic/user.status", update);
        }
    }
}

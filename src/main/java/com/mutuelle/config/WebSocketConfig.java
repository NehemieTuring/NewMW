package com.mutuelle.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final com.mutuelle.service.ChatService chatService;

    public WebSocketConfig(@org.springframework.context.annotation.Lazy com.mutuelle.service.ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(org.springframework.messaging.Message<?> message, org.springframework.messaging.MessageChannel channel) {
                org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor = 
                    org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(message, org.springframework.messaging.simp.stomp.StompHeaderAccessor.class);
                
                if (accessor != null && org.springframework.messaging.simp.stomp.StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String userId = accessor.getFirstNativeHeader("userId");
                    if (userId != null) {
                        // Set the user principal so convertAndSendToUser works
                        accessor.setUser(() -> userId);
                        chatService.updateLastSeen(Long.parseLong(userId));
                    }
                }
                return message;
            }
        });
    }
}

package com.vulntrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Same origin list as the REST CORS config (CORS_ALLOWED_ORIGINS env
    // var, comma-separated); defaults to the local dev origins.
    @Value("${cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private List<String> allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Clients will subscribe to /topic/issues/{issueId}
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint for handshake
        registry.addEndpoint("/ws")
            .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
            .withSockJS();
    }
}
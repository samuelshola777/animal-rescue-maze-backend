package com.samuel.animalrescue.config;

import com.samuel.animalrescue.websocket.GameWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final GameWebSocketHandler gameWebSocketHandler;
    private final CorsProperties corsProperties;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler, CorsProperties corsProperties) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/games/*")
                .setAllowedOriginPatterns(corsProperties.allowedOriginPatterns().toArray(String[]::new));
    }
}

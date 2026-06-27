package dev.taskflow.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatHandler;
    private final MeetWebSocketHandler meetHandler;

    public WebSocketConfig(ChatWebSocketHandler chatHandler, MeetWebSocketHandler meetHandler) {
        this.chatHandler = chatHandler;
        this.meetHandler = meetHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/ws/chat").setAllowedOriginPatterns("*");
        registry.addHandler(meetHandler, "/ws/meet").setAllowedOriginPatterns("*");
    }
}

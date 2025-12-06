package com.stroke.omok.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.stroke.omok.websocket.EchoHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final EchoHandler echoHandler;

    public WebSocketConfig() {
        this.echoHandler = new EchoHandler(); // 직접 생성(초기 테스트용)
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(echoHandler, "/ws/echo")
                .setAllowedOrigins("*");  // CORS 허용
    }
}

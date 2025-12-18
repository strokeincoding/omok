package com.stroke.omok.config;

import com.stroke.omok.websocket.lobby.LobbyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    // ---------------------------------------------------------
    // 로비 전용 WebSocket 핸들러
    //
    // Day 3의 목적:
    // - 접속 / 퇴장 관리
    // - 로비 상태 브로드캐스트
    // - "모든 유저가 보는 공용 채널"
    // ---------------------------------------------------------
    private final LobbyWebSocketHandler lobbyWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        // ---------------------------------------------------------
        // /ws/lobby
        //
        // ✔ 로그인(Session) 완료된 사용자만 접속
        // ✔ 로비 진입 시 WebSocket 연결
        // ✔ 접속 / 퇴장 / 메시지를 서버가 관리
        //
        // Echo 테스트용 /ws/echo 는 Day 3부터 제거
        // ---------------------------------------------------------
        registry.addHandler(lobbyWebSocketHandler, "/ws/lobby")
                .setAllowedOrigins("*"); // 개발 단계이므로 전체 허용
    }
}

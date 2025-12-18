package com.stroke.omok.websocket.lobby;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stroke.omok.user.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 로그인(Session 인증)이 완료된 사용자만 로비 WebSocket 접속
// 로비 공용 정보를 모든 로비 사용자에게 실시간으로 브로드캐스트

@Component
@Slf4j
public class LobbyWebSocketHandler extends TextWebSocketHandler {

    // 현재 접속 중인 로비 사용자
    // WebSocket은 멀티 스레드 환경
    // 동시에 접속 / 종료 발생 가능
    // ConcurrentHashMap → thread-safe
    // userId → WebSocketSession
    private final Map<Long, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ----------------------------------------------------
    // WebSocket 연결 성공 (로비 입장)
    // ----------------------------------------------------
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        Authentication authentication =
                (Authentication) session.getPrincipal();

        // 인증 안 된 사용자는 즉시 차단
        // (서버 자원 보호 + 보안)
        if (authentication == null || !authentication.isAuthenticated()) {
            session.close();
            return;
        }

        CustomUserDetails user =
                (CustomUserDetails) authentication.getPrincipal();

        Long userId = user.getUserId();

        // 로비 접속자 등록
        sessions.put(userId, session);

        log.info("[LOBBY] JOIN - userId={}, username={}",
                userId, user.getUsername());

        // 로비 전체 브로드캐스트
        broadcastUserCount();
        broadcastUserJoin(user);
    }

    // ----------------------------------------------------
    // WebSocket 연결 종료 (로비 퇴장)
    // - 브라우저 종료
    // - 새로고침
    // - 네트워크 끊김
    // ----------------------------------------------------
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        Long disconnectedUserId = null;

        // 세션 ID 기준으로 userId 역추적
        for (Map.Entry<Long, WebSocketSession> entry : sessions.entrySet()) {
            if (entry.getValue().getId().equals(session.getId())) {
                disconnectedUserId = entry.getKey();
                break;
            }
        }

        if (disconnectedUserId != null) {
            sessions.remove(disconnectedUserId);

            log.info("[LOBBY] LEAVE - userId={}", disconnectedUserId);

            broadcastUserCount();
            broadcastUserLeave(disconnectedUserId);
        }
    }

    // ----------------------------------------------------
    // 현재 접속자 수 전체 브로드캐스트
    // ----------------------------------------------------
    private void broadcastUserCount() {

        LobbyMessage message = new LobbyMessage(
                LobbyMessageType.USER_COUNT,
                sessions.size()
        );

        broadcast(message);
    }

    // ----------------------------------------------------
    // 유저 입장 이벤트 브로드캐스트
    // ----------------------------------------------------
    private void broadcastUserJoin(CustomUserDetails user) {

        LobbyMessage message = new LobbyMessage(
                LobbyMessageType.USER_JOIN,
                Map.of(
                        "userId", user.getUserId(),
                        "username", user.getUsername()
                )
        );

        broadcast(message);
    }

    // ----------------------------------------------------
    // 유저 퇴장 이벤트 브로드캐스트
    // ----------------------------------------------------
    private void broadcastUserLeave(Long userId) {

        LobbyMessage message = new LobbyMessage(
                LobbyMessageType.USER_LEAVE,
                Map.of("userId", userId)
        );

        broadcast(message);
    }

    // ----------------------------------------------------
    // 공통 브로드캐스트 메서드
    // ----------------------------------------------------
    private void broadcast(LobbyMessage message) {

        try {
            String json = objectMapper.writeValueAsString(message);

            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }

        } catch (Exception e) {
            log.error("[LOBBY] broadcast error", e);
        }
    }
}

package com.stroke.omok.websocket.lobby;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stroke.omok.user.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Comment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 로그인(Session 인증)이 완료된 사용자만 로비 WebSocket 접속
// 로비방 공용 정보를 모든 로비 사용자에게 실시간으로 브로드캐스트

@Component
@Slf4j
public class LobbyWebSocketHandler extends TextWebSocketHandler {

    // 현재 접속 중인 세션들
    // WebSocket은 멀티 스레트 환경
    // 동시에 접속 / 종료 발생 가능
    // ConcurrentHashMap.newKeySet(); -> thread-safe, 성능 좋은
    private final Set<WebSocketSession> sessions =
            ConcurrentHashMap.newKeySet();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ----------------------------------------------------
    // WebSocket 연결 성공
    // ----------------------------------------------------
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        Authentication authentication =
                (Authentication) session.getPrincipal();

        //인증 점검 단계(서버 메모리 점유, 브로드캐스트 오염, 보안 이슈 등) 지속 연결을 위해 초기 진입 시 차단
        if (authentication == null || !authentication.isAuthenticated()) {
            session.close();
            return;
        }

        CustomUserDetails user =
                (CustomUserDetails) authentication.getPrincipal();

        //접속 성공 시 로비 목록에 추가(브로드 캐스트 대상 및 로비 사용자 취급)
        sessions.add(session);

        log.info("[LOBBY] JOIN - userId={}, username={}",
                user.getUserId(), user.getUsername());

        broadcastUserCount();
    }

    // ----------------------------------------------------
    // WebSocket 연결 종료
    // 브라우저 종료, 새로고침, 네트워크 끊김, 서버 종료
    // ----------------------------------------------------
    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) throws Exception {

        //세션 제거
        sessions.remove(session);

        log.info("[LOBBY] LEAVE - sessionId={}", session.getId());

        // 브로드캐스트 호출
        broadcastUserCount();
    }

    // ----------------------------------------------------
    // 현재 접속자 수 전체 브로드캐스트
    // 사용자 접속, 종료마다 호출
    // ----------------------------------------------------
    private void broadcastUserCount() throws Exception {

        LobbyMessage message = new LobbyMessage(
                LobbyMessageType.USER_COUNT,
                sessions.size()
        );

        String json = objectMapper.writeValueAsString(message);

        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }
    }
}

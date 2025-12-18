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

/**
 * ----------------------------------------------------
 * LobbyWebSocketHandler
 * ----------------------------------------------------
 *
 * 역할:
 *  - 로그인(Session 인증)이 완료된 사용자만 로비 WebSocket 접속 허용
 *  - 로비에 접속한 사용자들을 서버 메모리에서 관리
 *  - 로비 공용 정보(접속자 수, 유저 입장/퇴장, 유저 목록)를
 *    모든 로비 사용자에게 실시간으로 브로드캐스트
 *
 *  - 접속 / 퇴장 이벤트 처리
 *  - USER_COUNT 브로드캐스트
 *  - USER_JOIN / USER_LEAVE 이벤트
 *  - USER_LIST (로비 유저 스냅샷)
 */
@Component
@Slf4j
public class LobbyWebSocketHandler extends TextWebSocketHandler {

    /**
     * 현재 로비에 접속 중인 사용자 세션 목록
     *
     * - WebSocket은 멀티 스레드 환경
     * - 동시 접속 / 종료 발생 가능
     * - ConcurrentHashMap → thread-safe
     *
     * key   : userId
     * value : WebSocketSession
     */
    private final Map<Long, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    /**
     * 현재 로비에 접속 중인 사용자 정보
     *
     * - USER_LIST 스냅샷 용도
     * - 프론트에서 로비 화면 구성 시 사용
     */
    private final Map<Long, LobbyUserDto> users =
            new ConcurrentHashMap<>();

    /**
     * WebSocket 메시지를 JSON 문자열로 직렬화하기 위한 ObjectMapper
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ----------------------------------------------------
    // WebSocket 연결 성공 (로비 입장)
    // ----------------------------------------------------
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        /**
         * WebSocket은 HTTP 요청 이후 handshake로 연결되며,
         * Security 인증 정보는 Principal로 전달됨
         */
        Authentication authentication =
                (Authentication) session.getPrincipal();

        /**
         * 인증되지 않은 사용자는 즉시 차단
         *
         * 이유:
         *  - 서버 메모리 점유 방지
         *  - 브로드캐스트 오염 방지
         *  - 보안 이슈 예방
         */
        if (authentication == null || !authentication.isAuthenticated()) {
            session.close();
            return;
        }

        /**
         * 인증된 사용자 정보 추출
         */
        CustomUserDetails user =
                (CustomUserDetails) authentication.getPrincipal();

        Long userId = user.getUserId();

        /**
         * 로비 사용자 상태 등록
         * - USER_LIST 스냅샷에 사용됨
         */
        users.put(
                userId,
                new LobbyUserDto(
                        userId,
                        user.getUsername(),
                        LobbyUserStatus.WAITING
                )
        );

        /**
         * WebSocket 세션 등록
         * - 브로드캐스트 대상
         */
        sessions.put(userId, session);

        log.info("[LOBBY] JOIN - userId={}, username={}",
                userId, user.getUsername());

        /**
         * 신규 접속자에게 현재 로비 상태 스냅샷 전달
         *    (USER_LIST)
         */
        sendUserList(session);

        /**
         * 전체 사용자에게 이벤트 브로드캐스트
         */
        broadcastUserCount();
        broadcastUserJoin(user);
    }

    // ----------------------------------------------------
    // WebSocket 연결 종료 (로비 퇴장)
    // ----------------------------------------------------
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        Long disconnectedUserId = null;

        /**
         * WebSocketSession → userId 역추적
         */
        for (Map.Entry<Long, WebSocketSession> entry : sessions.entrySet()) {
            if (entry.getValue().getId().equals(session.getId())) {
                disconnectedUserId = entry.getKey();
                break;
            }
        }

        if (disconnectedUserId != null) {

            sessions.remove(disconnectedUserId);
            users.remove(disconnectedUserId);

            log.info("[LOBBY] LEAVE - userId={}", disconnectedUserId);

            /**
             * 퇴장 이벤트 발생 시
             *  - 접속자 수 갱신
             *  - USER_LEAVE 이벤트 브로드캐스트
             */
            broadcastUserCount();
            broadcastUserLeave(disconnectedUserId);
        }
    }

    // ----------------------------------------------------
    // 현재 접속자 수 브로드캐스트
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
    // USER_LIST (로비 유저 스냅샷) - 단일 세션 전송
    // ----------------------------------------------------
    private void sendUserList(WebSocketSession session) {

        try {
            LobbyMessage message = new LobbyMessage(
                    LobbyMessageType.USER_LIST,
                    users.values()
            );

            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));

        } catch (Exception e) {
            log.error("[LOBBY] send USER_LIST error", e);
        }
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

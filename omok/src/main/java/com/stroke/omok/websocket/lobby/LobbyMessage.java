package com.stroke.omok.websocket.lobby;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LobbyMessage {

    // payload를 Object를 둔 이유
    // -> 추후 "유저목록", "방 목록" , "매칭 상태"
    // 추가 가능성이 있는 lobby에 이벤트를 다 같은 메세지 구조로 보내기 위해서
    // DTO + Protocol 정의
    // WebSocket 메세지 포맷 정의 전용

    private LobbyMessageType type;
    private Object payload;
}
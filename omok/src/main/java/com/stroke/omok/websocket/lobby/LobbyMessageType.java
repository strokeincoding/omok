package com.stroke.omok.websocket.lobby;


// ⚠️ WebSocket 메시지를 문자열(String)로 막 보내면 구조가 무너진다.
//
// 이유 1)
//  - 클라이언트/서버가 메시지 의미를 문자열 비교(if, equals)로 판단하게 됨
//  - 이벤트가 늘어날수록 분기 지옥 발생
//
// 이유 2)
//  - 메시지 포맷이 암묵적이라 유지보수 시 의도를 파악하기 어려움
//  - 프론트/백엔드 간 규약이 깨지기 쉬움
//
// 이유 3)
//  - 추후 확장(유저 목록, 방 목록, 매칭 상태 등)이 매우 힘들어짐
//
// 해결 방법)
//  - 반드시 "type + payload" 구조를 사용
//  - enum(MessageType) 기반으로 이벤트를 명확히 정의
//
// → WebSocket은 '이벤트 기반 프로토콜'처럼 다뤄야 안정적으로 확장 가능

//로비 이벤트 정의
public enum LobbyMessageType {

    USER_COUNT,     // 현재 접속자 수
    USER_JOIN,      // 유저 입장 (확장용)
    USER_LEAVE,     // 유저 퇴장 (확장용)

    ROOM_LIST,      // 방 목록 전달 (Day 3 후반)
    MATCHING_STATE  // 매칭 상태 (Day 4)
}

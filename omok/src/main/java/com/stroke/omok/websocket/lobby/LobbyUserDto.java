package com.stroke.omok.websocket.lobby;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LobbyUserDto {

    private Long userId;
    private String username;
    private LobbyUserStatus status;
}

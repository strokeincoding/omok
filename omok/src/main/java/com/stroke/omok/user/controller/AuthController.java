package com.stroke.omok.user.controller;

import com.stroke.omok.user.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// @Controller 사용시 MVC 구조(HTML View)로 반환됨
// @RestController 사용 시 Json 응답
@RestController
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("message", "UNAUTHORIZED"));
        }

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(
                Map.of(
                        "id", user.getUserId(),
                        "username", user.getUsername(),
                        "role", user.getAuthorities()
                )
        );
    }
}

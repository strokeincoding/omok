package com.stroke.omok.user.controller;

import com.stroke.omok.user.DTO.UserRegisterRequest;
import com.stroke.omok.user.security.CustomUserDetails;
import com.stroke.omok.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// @Controller 사용시 MVC 구조(HTML View)로 반환됨
// @RestController 사용 시 Json 응답
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of("message", "UNAUTHORIZED"));
        }

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        String role = user.getAuthorities()
                .stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("ROLE_USER");

        return ResponseEntity.ok(
                Map.of(
                        "id", user.getUserId(),
                        "username", user.getUsername(),
                        "role", role
                )
        );
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterRequest req) {
        userService.register(req);
        return ResponseEntity.ok(Map.of("message", "REGISTER_SUCCESS"));
    }
}
package com.stroke.omok.user.service;

import com.stroke.omok.user.DTO.UserRegisterRequest;
import com.stroke.omok.user.entity.User;
import com.stroke.omok.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void register(UserRegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("이미 존재하는 사용자입니다.");
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))  // 🔥 암호화 저장
                .role("USER")
                .build();

        userRepository.save(user);
    }
}

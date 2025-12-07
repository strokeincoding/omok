package com.stroke.omok.user.security;

import com.stroke.omok.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor //final 필드를 생성자 주입으로 자동 생성하는 Lombok
public class CustomUserDetails implements UserDetails {

    // ---------------------------------------------------------------------
    // 로그인한 사용자(User 엔티티)를 보안 객체(UserDetails)로 감싸는 필드
    // - final 사용 이유:
    //   1) UserDetails는 인증된 사용자를 나타내는 불변 객체여야 하기 때문
    //   2) 생성 시 반드시 값이 주입되도록 강제하여 null 방지
    //   3) 보안 객체 특성상 중간에 변경되면 안 되므로 읽기 전용에 적합
    // ---------------------------------------------------------------------
    private final User user;  // 반드시 final

    // ---------------------------------------------------------------------
    // 생성자에서 User를 주입받는 이유:
    // - Spring DI가 아니라, DB에서 조회된 User 엔티티를 기반으로
    //   CustomUserDetails 객체를 직접 생성하는 구조이기 때문
    // - 즉, "new CustomUserDetails(user)" 형태로 사용되며,
    //   User 객체를 보안용 UserDetails 형태로 변환하는 역할
    // @RequiredArgsConstructor 사용시 선언 X
    // ---------------------------------------------------------------------
//    public CustomUserDetails(User user) {
//        this.user = user;
//    }

    public Long getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getPassword() { return user.getPassword(); }

    @Override
    public String getUsername() { return user.getUsername(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}

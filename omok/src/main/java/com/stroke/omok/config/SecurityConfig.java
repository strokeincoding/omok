package com.stroke.omok.config;

import com.stroke.omok.user.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // ---------------------------------------------------------------------
    // Spring Security가 로그인 시 사용할 사용자 조회 서비스
    // Boot 3.x 에서는 자동 주입 X → 반드시 명시적 Provider 등록 필요
    // ---------------------------------------------------------------------
    private final CustomUserDetailsService customUserDetailsService;

    // ---------------------------------------------------------------------
    // BCryptPasswordEncoder: 비밀번호 암호화 용도
    // Boot 2.5.4 시절에는 WebSecurityConfigurerAdapter 안에서 선언하는 경우가 많았음
    // Boot 3.x에서는 Bean으로 명시적 등록 필요
    // ---------------------------------------------------------------------
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ---------------------------------------------------------------------
    // DaoAuthenticationProvider:
    //
    // 🔥 Boot 2.x 시절
    // - AuthenticationManagerBuilder 내부가 자동으로 DaoAuthenticationProvider 생성
    // - 개발자가 직접 Provider Bean을 만들 필요가 없었음
    //
    // 🔥 Boot 3.x / Security 6.x
    // - WebSecurityConfigurerAdapter 삭제됨
    // - configure() 메서드도 삭제됨
    // - AuthenticationManagerBuilder가 자동 구성되지 않음
    // - 따라서 인증 Provider를 개발자가 직접 Bean으로 등록해야 함
    //
    // 이 Bean이 실제 로그인 인증을 수행하는 핵심 객체임
    // ---------------------------------------------------------------------
    @Bean
    public DaoAuthenticationProvider authProvider() {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        // 로그인 처리 시 사용할 사용자 조회 전략 설정
        provider.setUserDetailsService(customUserDetailsService);

        // 비밀번호 비교 시 BCrypt 알고리즘 사용
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    // ---------------------------------------------------------------------
    // Spring Security의 핵심: SecurityFilterChain Bean
    //
    // Boot 2.5.4:
    //   WebSecurityConfigurerAdapter + override configure(HttpSecurity) 사용
    //
    // Boot 3.x:
    //   반드시 SecurityFilterChain Bean 방식으로 설정해야 함
    //
    // Boot 3.x에서 WebSecurityConfigurerAdapter가 deprecated → 삭제됨
    //
    // 아래 설정은 Boot 3.x 표준 구조
    // ---------------------------------------------------------------------
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // -----------------------------------------------------------------
                // Boot 3.x에서는 기본적으로 CSRF가 활성화되어 있으나,
                // 우리가 개발하는 REST API & WebSocket 기반 게임 서비스에서는 CSRF 비활성화가 일반적
                // CSRF 보호는 브라우저의 form 전송 기반 공격을 방지하는 기능.
                // React + REST API + WebSocket 구조에서는 form 기반 요청을 사용하지 않으며
                // WebSocket handshake에도 CSRF 토큰을 포함할 수 없음.
                // 따라서 SPA(React) + JSON API + WebSocket 기반 서비스에서는
                // CSRF를 비활성화하는 것이 일반적.
                // -----------------------------------------------------------------
                .csrf(csrf -> csrf.disable())

                // -----------------------------------------------------------------
                // 인증 Provider 등록 (로그인 시 어떤 방식으로 인증할 것인지)
                // Boot 2.x에서는 configure(AuthenticationManagerBuilder)에서 처리하던 부분
                // Boot 3.x에서는 Bean 등록한 Provider를 여기서 사용
                // -----------------------------------------------------------------
                .authenticationProvider(authProvider())

                // -----------------------------------------------------------------
                // URL 권한 설정: 어떤 URL에 인증이 필요한지
                // WebSocket 경로(/ws/**)는 인증 필요
                // 회원가입/로그인(/auth/**)는 인증 없이 접근 가능
                // -----------------------------------------------------------------
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/auth/me").authenticated()     // 로그인된 사용자만 호출 가능하도록 수정
                        .requestMatchers("/ws/**").authenticated()
                        .anyRequest().permitAll()
                )

                // -----------------------------------------------------------------
                // 로그인 설정
                // Boot 2.x에서는 http.formLogin().loginPage()처럼 설정 가능했지만
                // Boot 3.x에서도 구조는 같으나 반환 타입 및 체인 방식이 바뀜
                //
                // -----------------------------------------------------------------
                // React SPA 연동을 위한 로그인 처리 방식
                // redirect 사용 ❌
                // JSON 응답 반환 ✔
                //
                // React가 fetch/axios로 login 요청을 보낼 것이므로
                // 서버는 JSON만 반환하고, 페이지 이동은 React가 직접 수행하는 구조가 필요함.
                // -----------------------------------------------------------------
                .formLogin(login -> login
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"LOGIN_SUCCESS\"}");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"LOGIN_FAILED\"}");
                        })
                        .permitAll()
                )

                // -----------------------------------------------------------------
                // 로그아웃 설정
                // Boot 2.x와 큰 차이는 없지만 메서드 체인 방식이 조금 변경됨
                // -----------------------------------------------------------------
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/logout-success")
                );

        return http.build();
    }
}

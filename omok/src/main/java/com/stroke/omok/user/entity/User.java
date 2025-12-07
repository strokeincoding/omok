package com.stroke.omok.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // --------------------------------------------------------------------
    // ✔ JPA 자동 테이블 생성 설명
    // - Spring Boot 실행 시 Hibernate가 엔티티 기반으로 테이블 생성/수정
    // - ddl-auto=update → 없는 컬럼만 추가됨(컬럼 이름 변경은 반영되지 않음)
    // --------------------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    // --------------------------------------------------------------------
    // ✔ 생성일 컬럼 (자동 생성)
    //
    // @CreationTimestamp
    //  - INSERT 시점에 현재 시간이 자동 저장됨
    //  - 수정될 때는 업데이트되지 않음
    //  - LocalDateTime 타입은 MySQL DATETIME으로 매핑됨
    //
    // MySQL 컬럼 예:
    //   created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
    //
    // 개발 초기에는 이 방식이 매우 빠르고 편함.
    // --------------------------------------------------------------------
    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createdAt;
}

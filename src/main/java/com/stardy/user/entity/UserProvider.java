package com.stardy.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// user_providers 테이블에 대응하는 Entity입니다.
// 소셜 연동 정보(Google, Kakao 등)를 저장하는 테이블이에요.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "user_providers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_providers_provider_social_id",
                        columnNames = {"provider", "social_id"}
                )
        },
        indexes = {
                @Index(name = "idx_user_providers_user_id", columnList = "user_id")
        }
)
public class UserProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user_providers.user_id → users.id 를 표현합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String provider; // "GOOGLE", "KAKAO" 등

    @Column(name = "social_id", nullable = false, length = 255)
    private String socialId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;
}

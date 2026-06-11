package com.auth_app_backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
//using index because refresh token changes very frequently
@Table(name="refresh_token",indexes = {
        @Index(name="refresh_tokens_jti_idx",columnList ="jti",unique=true),
        @Index(name="refresh_tokens_user_id",columnList = "user_id")
})
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    //token ki id..iss id se hum data fetch krenge
    //hum sirf token ki id save krenge pura token as a string nahi..kr bhi skte h but zrurt nhi id se mil jayega
    @Column(name="jti",unique = true,nullable = false,updatable = false)
    private String jti;
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false,updatable = false)
    private User user;
    @Column(nullable = false,updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiresAt;
    private  String replacedByToken;
    @Column(nullable = false)
    private boolean revoked;

}

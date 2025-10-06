package ru.nand.authservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.nand.authservice.entity.ENUMS.STATUS;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@Table(name = "sessions")
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "access_token_hash", columnDefinition = "TEXT")
    private String accessTokenHash;

    @Column(name = "refresh_token_hash", columnDefinition = "TEXT")
    private String refreshTokenHash;

    private LocalDateTime accessTokenExpires;

    private LocalDateTime refreshTokenExpires;

    private LocalDateTime sessionCreationTime;

    private LocalDateTime lastActivityTime;

    @Enumerated(EnumType.STRING)
    private STATUS status;

    @ManyToOne(fetch = FetchType.LAZY) // Ленивая загрузка сессий
    @JoinColumn(name = "user_id")
    private User user;
}

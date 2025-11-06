package ru.nand.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.nand.notificationservice.entity.ENUM.STATUS;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sessions")
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "access_token_hash", columnDefinition = "TEXT")
    private String accessTokenHash;

    private LocalDateTime lastActivityTime;

    @Enumerated(EnumType.STRING)
    private STATUS status;

    @ManyToOne(fetch = FetchType.LAZY) // Ленивая загрузка сессий
    @JoinColumn(name = "user_id")
    private User user;
}

package ru.nand.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nand.authservice.entity.ENUMS.STATUS;
import ru.nand.authservice.entity.User;
import ru.nand.authservice.entity.UserSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    List<UserSession> findByUserAndStatus(User user, STATUS status);
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
    Optional<UserSession> findByAccessTokenHash(String accessTokenHash);
    List<UserSession> findByStatusAndLastActivityTimeBefore(STATUS status, LocalDateTime threshold);
}

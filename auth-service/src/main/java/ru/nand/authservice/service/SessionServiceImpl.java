package ru.nand.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.nand.authservice.entity.ENUMS.STATUS;
import ru.nand.authservice.entity.User;
import ru.nand.authservice.entity.UserSession;
import ru.nand.authservice.entity.dto.TokenResponse;
import ru.nand.authservice.repository.UserSessionRepository;
import ru.nand.authservice.util.NotificationUtil;
import ru.nand.authservice.util.UserJwtUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SessionServiceImpl implements SessionService {
    private final UserSessionRepository userSessionRepository;
    private final NotificationUtil notificationUtil;
    private final UserJwtUtil userJwtUtil;

    @Value("${jwt.user.access.expiration}")
    private long accessTokenExpiration;
    @Value("${jwt.user.refresh.expiration}")
    private long refreshTokenExpiration;

    @Autowired
    public SessionServiceImpl(UserSessionRepository userSessionRepository, NotificationUtil notificationUtil, UserJwtUtil userJwtUtil) {
        this.userSessionRepository = userSessionRepository;
        this.notificationUtil = notificationUtil;
        this.userJwtUtil = userJwtUtil;
    }

    @Override
    public TokenResponse createSession(User user) throws RuntimeException {
        // Заблокирован ли пользователь
        if (user.getIsBlocked()){
            throw new RuntimeException("User is blocked");
        }

        // Наличие заблокированных сессий
        if(hasBlockedSessions(user)){
            log.error("Пользователь {} имеет заблокированные сессии. Новая сессия не создана.", user.getUsername());
            throw new RuntimeException("User has blocked sessions");
        }

        // Проверка на активные сессии
        List<UserSession> activeSessions = userSessionRepository.findByUserAndStatus(user, STATUS.ACTIVE);
        if(!activeSessions.isEmpty()){
            log.warn("При создании новой сессиии обнаружены активные сессии пользователя {}", user.getUsername());

            log.info("Отправка уведомления об активных сессиях пользователю");
            try{
                notificationUtil.createAndSendNotification(user.getEmail(), "Обнаружено несколько активных сессий");
            } catch (RuntimeException e) {
                throw new RuntimeException(e); // Прокидываем дальше
            }
        }

        // Генерируем токены, чтобы вернуть их, в в БД будет хэш
        String accessToken = userJwtUtil.generateAccessToken(user);
        String refreshToken = userJwtUtil.generateRefreshToken(user);

        UserSession session = UserSession.builder()
                .user(user)
                .accessTokenHash(userJwtUtil.hashToken(userJwtUtil.generateAccessToken(user))) // Храним хэш
                .refreshTokenHash(userJwtUtil.hashToken(userJwtUtil.generateRefreshToken(user))) // Тоже хэш
                .accessTokenExpires(LocalDateTime.now().plus(Duration.ofMillis(accessTokenExpiration)))
                .refreshTokenExpires(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)))
                .lastActivityTime(LocalDateTime.now())
                .status(STATUS.ACTIVE)
                .build();

        UserSession userSession = userSessionRepository.save(session);
        log.debug("Создана сессия с id {} для пользователя {}", userSession.getId(), user.getUsername());

        return new TokenResponse(accessToken, refreshToken);
    }

    @Override
    public TokenResponse refreshAccessToken(String refreshToken) throws RuntimeException {
        // Валидируем refresh
        if(!userJwtUtil.validateUserToken(refreshToken)){
            throw new RuntimeException("Invalid refresh token");
        }

        // Ищем сессию с таким токеном, проверяем активна ли она (в противных случаях исключения)
        UserSession userSession = userSessionRepository.findByRefreshTokenHash(userJwtUtil.hashToken(refreshToken))
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (userSession.getStatus() != STATUS.ACTIVE) {
            throw new RuntimeException("Session is not active");
        }

        // Заблокирован ли пользователь
        if(userSession.getUser().getIsBlocked()){
            throw new RuntimeException("User is blocked");
        }

        // Наличие заблокированных сессий
        if(hasBlockedSessions(userSession.getUser())){
            log.error("Пользователь {} имеет заблокированные сессии.  Новая сессия не создана.", userSession.getUser().getUsername());
            throw new RuntimeException("User has blocked sessions");
        }

        // Генерируем новую пару
        String newAccessToken = userJwtUtil.generateAccessToken(userSession.getUser());
        String newRefreshToken = userJwtUtil.generateRefreshToken(userSession.getUser());

        // И заменяем старые на новые
        userSession.setAccessTokenHash(userJwtUtil.hashToken(newAccessToken));
        userSession.setRefreshTokenHash(userJwtUtil.hashToken(newRefreshToken));
        userSession.setLastActivityTime(LocalDateTime.now());
        userSessionRepository.save(userSession);
        log.debug("Сохранена новая сессия с новой парой токенов");

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    @Override
    public void blockUserSessions(User user, List<User> admins) {
        List<UserSession> sessions = userSessionRepository.findByUserAndStatus(user, STATUS.ACTIVE);
        sessions.forEach(session -> session.setStatus(STATUS.BLOCKED));

        log.warn("Все активные сессии пользователя {} заблокированы.", user.getUsername());
        userSessionRepository.saveAll(sessions);

        try{
            // Уведомление владельцу аккаунта
            notificationUtil.createAndSendNotification(user.getEmail(), "Все активные сессии заблокированы. Аккаунт заморожен."); // Заморозка так как через n времени сессии отчистятся
        } catch (RuntimeException e) {
            throw new RuntimeException(e); // Прокидываем дальше
        }

        // Уведомление администраторам
        if (!admins.isEmpty()){
            for(User admin : admins){
                try {
                    notificationUtil.createAndSendNotification(admin.getEmail(), "Все сессии пользователя " + user.getUsername() + " заблокированы, его аккаунт заморожен");
                } catch (RuntimeException e) {
                    throw new RuntimeException(e); // Прокидываем дальше
                }
            }
        } else{
            log.warn("Администраторы не найдены");
        }
    }

    @Override
    public boolean hasBlockedSessions(User user) {
        List<UserSession> blockedSessions = userSessionRepository.findByUserAndStatus(user, STATUS.BLOCKED);
        return !blockedSessions.isEmpty();
    }

    @Override
    public void deactivateSessionByAccessToken(String authHeader) throws RuntimeException {
        if(!(authHeader != null && authHeader.startsWith("Bearer "))){
            throw new RuntimeException("Invalid access token");
        }

        // Отрезаем Bearer_
        String accessToken = authHeader.substring(7);

        // Хэшируем токен и ищем
        UserSession userSession = userSessionRepository.findByAccessTokenHash(userJwtUtil.hashToken(accessToken))
                .orElseThrow(() -> new RuntimeException("Session not found"));

        userSession.setStatus(STATUS.INACTIVE);
        userSession.setLastActivityTime(LocalDateTime.now());

        log.debug("Сессия с id {} деактивирована для пользователя {}", userSession.getId(), userSession.getUser().getUsername());

        userSessionRepository.save(userSession);
    }

    @Override
    public void markInactiveSessionsAsRevoked() {
        // Порог - 1 День
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        List<UserSession> inactiveSessions = userSessionRepository.findByStatusAndLastActivityTimeBefore(STATUS.INACTIVE, threshold);

        if(!inactiveSessions.isEmpty()){
            log.debug("Найдено {} неактивных сессий для пометки на отзыв", inactiveSessions.size());

            inactiveSessions.forEach(session -> {
                session.setStatus(STATUS.REVOKED);
                session.setLastActivityTime(LocalDateTime.now());
            });

            userSessionRepository.saveAll(inactiveSessions);
            log.debug("{} сессий были помечены как REVOKED", inactiveSessions.size());
        } else {
            log.debug("Неактивных сессий для пометки на отзыв не найдено");
        }
    }

    @Override
    public void deleteRevokedSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        List<UserSession> revokedSessions = userSessionRepository.findByStatusAndLastActivityTimeBefore(STATUS.REVOKED, threshold);

        if(!revokedSessions.isEmpty()){
            log.info("Найдено {} сессий для удаления", revokedSessions.size());
            userSessionRepository.deleteAll(revokedSessions);
            log.debug("{} сессий было удалено", revokedSessions.size());
        } else {
            log.debug("Сессий для удаления не найдено");
        }
    }

    @Override
    public void handleInactiveSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        List<UserSession> activeSessions = userSessionRepository.findByStatusAndLastActivityTimeBefore(STATUS.ACTIVE, threshold);

        if (!activeSessions.isEmpty()) {
            log.info("Найдено {} активных сессий для перевода в статус INACTIVE", activeSessions.size());

            activeSessions.forEach(session -> {
                session.setStatus(STATUS.INACTIVE);
                session.setLastActivityTime(LocalDateTime.now()); // По это сути имитация логаута пользователя, поэтому установим LAT
            });

            userSessionRepository.saveAll(activeSessions);
            log.info("Активные сессии успешно переведены в статус INACTIVE");
        } else {
            log.debug("Активных сессий для перевода в статус INACTIVE не найдено");
        }
    }

    @Override
    public void handlingInactiveAndRevokedSessions() {
        markInactiveSessionsAsRevoked();
        deleteRevokedSessions();
    }

    @Override
    public void updateLastActivityTime(User user) {
        // Поиск активной сессии пользователя
        List<UserSession> activeSessions = userSessionRepository.findByUserAndStatus(user, STATUS.ACTIVE);

        if (activeSessions.isEmpty()) {
            log.warn("Активная сессия для пользователя {} не найдена.", user.getUsername());
            throw new RuntimeException("Active session not found");
        }

        UserSession session = activeSessions.getFirst();

        session.setLastActivityTime(LocalDateTime.now());

        userSessionRepository.save(session);

        log.debug("Время последней активности обновлено для пользователя: {}", user.getUsername());
    }

    @Override
    public List<UserSession> findByUserAndStatus(User user, STATUS status) {
        return userSessionRepository.findByUserAndStatus(user, status);
    }

    @Override
    public boolean isSessionActive(String accessToken) {
        UserSession userSession = userSessionRepository.findByAccessTokenHash(userJwtUtil.hashToken(accessToken))
                .orElseThrow(() -> new RuntimeException("Session not found"));
        log.debug("Найдена сессия с id: {} и статусом: {}", userSession.getId(), userSession.getStatus().name());

        return userSession.getStatus() == STATUS.ACTIVE;
    }
}

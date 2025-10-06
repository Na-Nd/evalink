package ru.nand.authservice.service;

import ru.nand.authservice.entity.ENUMS.STATUS;
import ru.nand.authservice.entity.User;
import ru.nand.authservice.entity.UserSession;
import ru.nand.authservice.entity.dto.TokenResponse;

import java.util.List;

public interface SessionService {
    TokenResponse createSession(User user); // Создание пользовательской сессии
    TokenResponse refreshAccessToken(String refreshToken); // Обновление Access
    List<UserSession> findByUserAndStatus(User user, STATUS status); // Поиск по пользователю и статусу
    boolean hasBlockedSessions(User user); // Наличие заблокированных сессий пользователя
    boolean isSessionActive(String accessToken); // Проверка на активность текущей сессии пользователя
    void deactivateSessionByAccessToken(String accessToken); // Деактивация сессии по Access
    void markInactiveSessionsAsRevoked(); // Пометка неактивных сессий на отзыв (для шедулера)
    void deleteRevokedSessions(); // Удаление сессий, помеченных на отзыв (для шедулера)
    void handleInactiveSessions(); // Обработка неактивных сессий (активные сессии без действия переводятся в inactive, для шедулера)
    void handlingInactiveAndRevokedSessions(); // Обработка неактивных и помеченных на отзыв сессий (для шедулера)
    void updateLastActivityTime(User user); // Обновление времени последней активности
    void blockUserSessions(User user, List<User> admins); // Блокировка активных пользовательских сессий (только для владельца и администраторов) по сути заморозка аккаунта. Уведомление получат администраторы и владелец
}

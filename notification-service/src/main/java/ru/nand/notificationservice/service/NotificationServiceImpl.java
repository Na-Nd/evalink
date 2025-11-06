package ru.nand.notificationservice.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nand.notificationservice.entity.Notification;
import ru.nand.notificationservice.entity.dto.NotificationDTO;
import ru.nand.notificationservice.repository.NotificationRepository;
import ru.nand.notificationservice.util.UserJwtUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserJwtUtil userJwtUtil;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository, UserJwtUtil userJwtUtil) {
        this.notificationRepository = notificationRepository;
        this.userJwtUtil = userJwtUtil;
    }

    /// Получение всех уведомлений
    @Override
    public List<NotificationDTO> getAllByAccessToken(String accessToken) {
        String email = extractEmailFromToken(accessToken);

        return notificationRepository.findAllByKeyUserEmailOrderByKeyCreatedAtDesc(email)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /// Получение конкрентого уведомления
    @Override
    public NotificationDTO getById(UUID notificationId, String accessToken) {
        String email = extractEmailFromToken(accessToken);

        Notification notification = notificationRepository
                .findByUserEmailAndNotificationId(email, notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        return toDto(notification);
    }

    /// Удаление по идентификатору
    @Override
    public String deleteById(UUID notificationId, String accessToken) {
        String email = extractEmailFromToken(accessToken);

        Notification notification = notificationRepository
                .findByUserEmailAndNotificationId(email, notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notificationRepository.deleteById(notification.getKey());
        return "Notification deleted successfully";
    }

    @Override
    public void create(NotificationDTO notificationDTO) {
        UUID createdAt = Uuids.timeBased(); // Время для сортировки
        UUID notificationId = UUID.randomUUID(); // Ддентификатор

        Notification.NotificationKey key = Notification.NotificationKey.builder()
                .userEmail(notificationDTO.getUserEmail())
                .createdAt(createdAt)
                .notificationId(notificationId)
                .build();

        Notification notification = Notification.builder()
                .key(key)
                .message(notificationDTO.getMessage())
                .creationDate(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    /// Вспомагательный метод для извлечения почты из токена
    private String extractEmailFromToken(String accessToken) throws RuntimeException{
        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("Access token is empty");
        }

        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        } else {
            throw new RuntimeException("Not full header");
        }

        return userJwtUtil.extractEmail(accessToken);
    }

    /// Вспомогательный метод для маппинга сущности в DTO
    private NotificationDTO toDto(Notification n) {
        return NotificationDTO.builder()
                .notificationId(n.getKey().getNotificationId())
                .createdAt(n.getKey().getCreatedAt())
                .message(n.getMessage())
                .userEmail(n.getKey().getUserEmail())
                .creationDate(n.getCreationDate())
                .build();
    }
}

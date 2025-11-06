package ru.nand.notificationservice.service;

import ru.nand.notificationservice.entity.dto.NotificationDTO;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    List<NotificationDTO> getAllByAccessToken(String accessToken);
    NotificationDTO getById(UUID notificationId, String accessToken);
    String deleteById(UUID notificationId, String accessToken);
    void create(NotificationDTO notificationDTO);
}

package ru.nand.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.nand.notificationservice.entity.dto.NotificationDTO;

@Slf4j
@Service
public class KafkaNotificationListener implements NotificationListener{
    private final SenderService senderService;
    private final ObjectMapper mapper;
    private final NotificationService notificationService;

    @Autowired
    public KafkaNotificationListener(SenderService senderService, ObjectMapper mapper, NotificationService notificationService) {
        this.senderService = senderService;
        this.mapper = mapper;
        this.notificationService = notificationService;
    }

    /// Обработчик для уведомлений о регистрации
    @Override
    @KafkaListener(topics = "auth-notifications-topic")
    public void handleAuthNotification(String message) throws JsonProcessingException {
        log.info("В auth слушатель принято уведомление");
        NotificationDTO notificationDTO = mapper.readValue(message, NotificationDTO.class);

        senderService.send(notificationDTO.getUserEmail(), "Регистрация аккаунта", notificationDTO.getUserEmail());

        notificationService.create(notificationDTO);
    }

    /// Обработчик для уведомлений об изменениях в аккаунте (изменение электронной почты)
    @Override
    @KafkaListener() // TODO
    public void handleAccountNotification(String message) throws JsonProcessingException {

    }
}

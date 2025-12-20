package ru.nand.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.nand.notificationservice.entity.dto.NotificationDTO;
import ru.nand.notificationservice.entity.dto.UserUpdateRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationListener implements NotificationListener{
    private final SenderService senderService;
    private final ObjectMapper mapper;
    private final NotificationService notificationService;
    private final UserService userService;

    /// Обработчик для уведомлений о регистрации
    @Override
    @KafkaListener(topics = "auth-notifications-topic")
    public void handleAuthNotification(String message) throws JsonProcessingException {
        log.info("В auth слушатель принято уведомление");
        NotificationDTO notificationDTO = mapper.readValue(message, NotificationDTO.class);

        senderService.send(notificationDTO.getUserEmail(), "Регистрация аккаунта", notificationDTO.getMessage());

        notificationService.create(notificationDTO);
    }

    /// Обработчик для уведомлений об изменениях в аккаунте (изменение электронной почты)
    @Override
    @KafkaListener(topics = "Account-Events-Topic")
    public void handleAccountNotification(String message) throws JsonProcessingException {
        log.info("В account слушатель принято событие");
        System.out.println("Принял событие AET");
        UserUpdateRequest userUpdateRequest = mapper.readValue(message, UserUpdateRequest.class);

        //  Делегируем дальнейшую работу по сохранению сущности сервису UserService
        userService.handleAccountEvent(userUpdateRequest);
    }
}

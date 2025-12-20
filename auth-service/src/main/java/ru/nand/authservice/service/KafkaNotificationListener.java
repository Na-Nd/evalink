package ru.nand.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.nand.authservice.entity.dto.UserUpdateRequest;

@Slf4j
@Service
public class KafkaNotificationListener implements NotificationListener {
    private final ObjectMapper objectMapper;
    private final UserService userService;

    public KafkaNotificationListener(ObjectMapper objectMapper, UserService userService) {
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    @Override
    @KafkaListener(topics = "Account-Events-Topic")
    public void handleAccountNotification(String message) throws JsonProcessingException {
        log.info("В account слушатель принято событие");
        UserUpdateRequest userUpdateRequest = objectMapper.readValue(message, UserUpdateRequest.class);

        //  Делегируем дальнейшую работу по сохранению сущности сервису UserService
        userService.handleAccountEvent(userUpdateRequest);
    }
}

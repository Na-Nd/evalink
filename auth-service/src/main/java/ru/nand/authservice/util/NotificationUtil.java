package ru.nand.authservice.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.nand.authservice.entity.User;
import ru.nand.authservice.entity.dto.NotificationDTO;

@Slf4j
@Component
public class NotificationUtil {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public NotificationUtil(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /// Соаздание уведомления и отправка его в топик
    public void createAndSendNotification(String targetEmail, String message) throws RuntimeException{
        log.info("Соаздние уведомления для передачи в топик");
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .message(message)
                .userEmail(targetEmail)
                .build();

        try{
            String notificationMessage = objectMapper.writeValueAsString(notificationDTO);

            log.info("Отправка уведомления в топик auth-notifications-topic");
            kafkaTemplate.send("auth-notifications-topic", notificationMessage);
        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления в auth-notifications-topic: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

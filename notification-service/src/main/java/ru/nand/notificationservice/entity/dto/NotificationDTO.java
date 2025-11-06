package ru.nand.notificationservice.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private UUID notificationId;

    private UUID createdAt;

    private String message;

    private String userEmail;

    private LocalDateTime creationDate;
}

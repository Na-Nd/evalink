package ru.nand.authservice.entity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDTO {
    String userEmail;
    String message;
}

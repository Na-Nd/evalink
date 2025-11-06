package ru.nand.notificationservice.entity;

import lombok.*;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("notifications")
public class Notification {
    @PrimaryKey
    private NotificationKey key;

    private String message;

    private LocalDateTime creationDate;

    @PrimaryKeyClass
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationKey implements Serializable {
        // Партиционный ключ для разделов
        @PrimaryKeyColumn(name = "user_email", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        private String userEmail;

        // Кластерный ключ для сортировки по времени
        @PrimaryKeyColumn(name = "created_at", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
        private UUID createdAt;

        // Идентификатор — дополнительная кластерная колонка
        @PrimaryKeyColumn(name = "notification_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
        private UUID notificationId;
    }
}

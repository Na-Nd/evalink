package ru.nand.notificationservice.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nand.notificationservice.entity.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends CassandraRepository<Notification, Notification.NotificationKey> {
    // Получить все уведомления пользователя, отсортированные по createdAt DESC
    List<Notification> findAllByKeyUserEmailOrderByKeyCreatedAtDesc(String userEmail);

    // Ищем уведомление по (userEmail + notificationId). ALLOW FILTERING здесь допустим, потому что поиск ограничен партиционным ключом (user_email)
    @Query("SELECT * FROM notifications WHERE user_email = :userEmail AND notification_id = :notificationId ALLOW FILTERING")
    Optional<Notification> findByUserEmailAndNotificationId(
            @Param("userEmail") String userEmail,
            @Param("notificationId") UUID notificationId
    );
}

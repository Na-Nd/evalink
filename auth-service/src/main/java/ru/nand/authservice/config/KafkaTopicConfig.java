package ru.nand.authservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaTopicConfig {
    // Топик для уведомлений
    @Bean
    public NewTopic authNotificationsTopic(){
        return TopicBuilder.name("auth-notifications-topic")
                .partitions(3) // Кол-во партиций на ноду
                .replicas(3) // Кол-во реплик партиций на других нодах
                .configs(Map.of("min.insync.replicas", "2")) // Кол-во реплик, которые должны подтвердить запись (лидер + реплика)
                .build();
    }
}

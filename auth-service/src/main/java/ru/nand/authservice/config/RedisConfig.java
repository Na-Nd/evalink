package ru.nand.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.nand.authservice.entity.dto.RegisterDTO;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, RegisterDTO> registerRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, RegisterDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        template.setKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<RegisterDTO> serializer = new Jackson2JsonRedisSerializer<>(RegisterDTO.class);
        template.setValueSerializer(serializer);

        return template;
    }

    @Bean
    public Jackson2JsonRedisSerializer<RegisterDTO> registerDTOSerializer() {
        return new Jackson2JsonRedisSerializer<>(RegisterDTO.class);
    }
}

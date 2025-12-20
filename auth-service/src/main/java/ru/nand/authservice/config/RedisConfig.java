package ru.nand.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.nand.authservice.entity.dto.RegisterDTO;

@Configuration
public class RedisConfig {

    @Bean("registerRedisTemplate")
    public RedisTemplate<String, RegisterDTO> registerRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, RegisterDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<RegisterDTO> valueSerializer = new Jackson2JsonRedisSerializer<>(RegisterDTO.class);

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet(); // <- обязательно
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean
    public Jackson2JsonRedisSerializer<RegisterDTO> registerDTOSerializer() {
        return new Jackson2JsonRedisSerializer<>(RegisterDTO.class);
    }
}

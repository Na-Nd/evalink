package ru.nand.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.nand.authservice.entity.dto.RegisterDTO;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService implements CacheService {
    private final RedisTemplate<String, RegisterDTO> redisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, RegisterDTO> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /// Сохранение значения с TTL
    @Override
    public void save(String key, Object value, long timeout, TimeUnit unit) {
        log.info("Сохранение в Redis кэш");
        redisTemplate.opsForValue().set(key, (RegisterDTO) value, timeout, unit);
        log.debug("Сохранено значение в Redis: Ключ: {}, Значение: {}, TTL: {} {}", key, value, timeout, unit);
    }

    /// Получение значения по ключу
    public Object get(String key) {
        log.info("Получение значения из Redis кэша по ключу");
        Object object = redisTemplate.opsForValue().get(key);
        log.debug("Получено значение из Redis: Ключ: {}, Значение: {}", key, object);
        return object;
    }

    /// Удаление значения по ключу
    @Override
    public void delete(String key) {
        log.info("Удаление значения из redis по ключу");
        redisTemplate.delete(key);
        log.debug("Удалено значение из Redis. Ключ: {}", key);
    }
}

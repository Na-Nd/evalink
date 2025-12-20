package ru.nand.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.nand.authservice.entity.dto.RegisterDTO;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {

    private final RedisTemplate<String, RegisterDTO> registerRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public RedisService(@Qualifier("registerRedisTemplate") RedisTemplate<String, RegisterDTO> registerRedisTemplate,
                        StringRedisTemplate stringRedisTemplate) {
        this.registerRedisTemplate = registerRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void saveVerificationCode(String key, String code, long timeout, TimeUnit unit) {
        log.info("Save code key={} code={}", key, code);
        stringRedisTemplate.opsForValue().set(key, code, timeout, unit);
    }

    public String getVerificationCode(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void savePendingRegistration(String key, RegisterDTO dto, long timeout, TimeUnit unit) {
        log.info("Save pending registration key={} dto={}", key, dto);
        registerRedisTemplate.opsForValue().set(key, dto, timeout, unit);
    }

    public RegisterDTO getPendingRegistration(String key) {
        return registerRedisTemplate.opsForValue().get(key);
    }

    public void deleteKey(String key) {
        // удаляем в обоих шаблонах (безопасно)
        registerRedisTemplate.delete(key);
        stringRedisTemplate.delete(key);
    }

    public void delete(String key) {
        log.info("Удаление ключа из Redis: {}", key);
        registerRedisTemplate.delete(key);
        stringRedisTemplate.delete(key);
    }

}


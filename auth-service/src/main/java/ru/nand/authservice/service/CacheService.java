package ru.nand.authservice.service;

import ru.nand.authservice.entity.dto.RegisterDTO;

import java.util.concurrent.TimeUnit;

public interface CacheService {
    void save(String key, Object value, long timeout, TimeUnit unit);
    Object get(String key);
    void delete(String key);
}

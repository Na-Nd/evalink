package ru.nand.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface NotificationListener {
    void handleAccountNotification(String message) throws JsonProcessingException;
}

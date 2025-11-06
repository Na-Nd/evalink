package ru.nand.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface NotificationListener {
    void handleAuthNotification(String message) throws JsonProcessingException;
    void handleAccountNotification(String message) throws JsonProcessingException;
}

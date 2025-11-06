package ru.nand.notificationservice.service;

public interface SenderService {
    void send(String targetUserEmail, String subject, String message);
}

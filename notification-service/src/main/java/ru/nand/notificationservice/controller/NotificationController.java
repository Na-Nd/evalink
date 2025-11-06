package ru.nand.notificationservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.nand.notificationservice.entity.UserDetailsImpl;
import ru.nand.notificationservice.service.NotificationService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping()
    public ResponseEntity<?> getAll(@RequestHeader(name = "Authorization") String accessToken) {
        try{
            log.info("Запрос от клиента на получение всех уведомлений");
            return ResponseEntity.status(200).body(notificationService.getAllByAccessToken(accessToken));
        } catch (RuntimeException e){
            return ResponseEntity.status(404).body("Notifications not found");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id, @RequestHeader(name = "Authorization") String accessToken){
        try{
            log.info("Запрос от клиента на получение уведомления по идентификатору");
            return ResponseEntity.status(200).body(notificationService.getById(id, accessToken));
        } catch (RuntimeException e){
            return ResponseEntity.status(404).body("Notification not found");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, @RequestHeader(name = "Authorization") String accessToken){
        try{
            log.info("Запрос от клиента на удаление уведомления");
            return ResponseEntity.status(200).body(notificationService.deleteById(id, accessToken));
        } catch (RuntimeException e){
            return ResponseEntity.status(400).body("Cannot delete notification");
        }
    }
}

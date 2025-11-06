package ru.nand.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailSenderService implements SenderService{

    @Value("${spring.mail.username}")
    private String sender;

    private final JavaMailSender javaMailSender;

    @Autowired
    public MailSenderService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void send(String targetUserEmail, String subject, String message){
        log.info("Создание уведомления для отправки на электронную почту");
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(sender);
        mailMessage.setTo(targetUserEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        log.debug("Создано уведомление {} для отправки на почту пользователю {}", message, targetUserEmail);
        javaMailSender.send(mailMessage);
    }
}

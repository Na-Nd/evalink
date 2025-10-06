package ru.nand.authservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nand.authservice.service.AuthService;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class TokenController {
    private final AuthService authService;

    @Autowired
    public TokenController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(String refreshToken){
        log.info("Обновление access токена");
        return authService.refreshAccessToken(refreshToken);
    }
}

package ru.nand.authservice.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.nand.authservice.entity.dto.LoginDTO;
import ru.nand.authservice.entity.dto.RegisterDTO;
import ru.nand.authservice.service.AuthService;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /// Регистрация
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO, BindingResult bindingResult) {
        log.info("Запрос на регистрацию");
        return authService.registerUser(registerDTO, bindingResult);
    }

    /// Подтверждение почты
    @PostMapping("/verify-email")
    public ResponseEntity<?> verify(@RequestParam String email, @RequestParam String code){
        return authService.verifyAndRegisterUser(email, code);
    }

    /// Логин
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO, BindingResult bindingResult) {
        return authService.loginUser(loginDTO, bindingResult);
    }

    /// Логаут
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader){
        log.info("Логаут пользователя");
        return authService.logoutUser(authHeader);
    }

}

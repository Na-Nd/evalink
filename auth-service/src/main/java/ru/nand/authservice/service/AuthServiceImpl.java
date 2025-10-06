package ru.nand.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.nand.authservice.entity.User;
import ru.nand.authservice.entity.dto.LoginDTO;
import ru.nand.authservice.entity.dto.RegisterDTO;
import ru.nand.authservice.entity.dto.TokenResponse;
import ru.nand.authservice.util.NotificationUtil;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final RedisService redisService;
    private final UserService userService;
    private final NotificationUtil notificationUtil;
    private final RestClient accountServiceRestClient;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(RedisService redisService, UserService userService, NotificationUtil notificationUtil, RestClient accountServiceRestClient, PasswordEncoder passwordEncoder) {
        this.redisService = redisService;
        this.userService = userService;
        this.notificationUtil = notificationUtil;
        this.accountServiceRestClient = accountServiceRestClient;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ResponseEntity<?> registerUser(RegisterDTO registerDTO, BindingResult bindingResult) {
        // Ошибки в форме
        if(bindingResult.hasErrors()) {
            // То формируем строку с ошибками и возвращаем её
            return ResponseEntity.status(400).body("Validation Errors: " + handleValidationErrors(bindingResult));
        }

        // Генерация кода2
        String verificationCode = String.valueOf((int) (Math.random() * 9000) + 1000);

        // Сохранение временных данных в Redis
        redisService.save("verification_code:" + registerDTO.getEmail(), verificationCode, 5, TimeUnit.MINUTES); // Храним почту как K, а верификационный код как V
        redisService.save("pending_registration:" + registerDTO.getEmail(), registerDTO, 5, TimeUnit.MINUTES);

        // Формируем уведомление для передачи в топик, чтобы ответственный сервис отправил Email-нотификацию
        try{
            notificationUtil.createAndSendNotification(registerDTO.getEmail(), "Ваш код верификации: " + verificationCode);
        } catch (RuntimeException e){ // Если косяк при отправке
            return ResponseEntity.status(500).body("Error to send notification");
        }

        // Подсказка
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/verify-email")
                .query("email={email}")
                .buildAndExpand(registerDTO.getEmail())
                .toUri();

        Map<String, Object> responseBody = Map.of(
                "message", "Verification code sent. Check your email.",
                "email", registerDTO.getEmail(),
                "expiresInMinutes", 5
        );

        return ResponseEntity.created(location).body(responseBody);
    }

    @Override
    public ResponseEntity<?> verifyAndRegisterUser(String email, String code){
        // Получаем код и дто из кэша
        String savedCode = (String) redisService.get("verification_code:" + email);
        RegisterDTO savedRegisterDTO = (RegisterDTO) redisService.get("pending_registration:" + email);

        // Если чего-то нет - значит код истек или данные невалидные
        if (savedCode == null || savedRegisterDTO == null) return ResponseEntity.status(400).body("The verification code has expired or email/code is invalid");

        if(!savedCode.equals(code)) return ResponseEntity.status(400).body("Invalid verification code");

        // Чистим
        redisService.delete("verification_code:" + email);
        redisService.delete("pending_registration:" + email);

        // Создаем пользователя и сессию, возвращаем TR
        TokenResponse tokenResponse;
        try {
            tokenResponse = userService.createUser(savedRegisterDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body("Error to create user");
        }

        // Отправляем registerDTO по restClient в сервис аккаунтов
        try{
            log.info("Отправка запроса в серсис аккаунтов");
            ResponseEntity<Void> responseEntity = accountServiceRestClient
                    .post()
                    .uri("/api/accounts/register")
                    .body(savedRegisterDTO)
                    .retrieve()
                    .toBodilessEntity();

            if (responseEntity.getStatusCode().is2xxSuccessful()){
                return ResponseEntity.status(200).body(tokenResponse);
            } else {
                return ResponseEntity.status(400).body("Error to create user");
            }
        } catch (RestClientResponseException e){
            // Если сервис аккаунтов вернул 4xx/5xx
            log.error("Ошибка от account-service: статус {} тело: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(500).body("Error to create user");
        } catch (Exception e){
            // Остальные ошибки (отказ в соединении и тд)
            log.error("Ошибка при запросе к account-service: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error to connect to other service");
        }
    }

    @Override
    public ResponseEntity<?> loginUser(LoginDTO loginDTO, BindingResult bindingResult) {
        // Ошибки в форме
        if(bindingResult.hasErrors()) {
            // То формируем строку с ошибками и возвращаем её
            return ResponseEntity.status(400).body("Validation Errors: " + handleValidationErrors(bindingResult));
        }

        // Ищем пользователя по имени
        User user = userService.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Сверяем пароль
        if(!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())){
            return ResponseEntity.status(400).body("Wrong password");
        }

        log.debug("Пользователь {} прошел аутентификацию", loginDTO.getUsername());

        try{
            log.info("Успешная аутентификация");
            return ResponseEntity.status(200).body(userService.login(user));
        } catch (RuntimeException e){
            log.error("Ошибка при аутентификации пользователя {}: {}", user.getUsername(), e.getMessage());
            return ResponseEntity.status(500).body("Server error");
        }
    }

    @Override
    public ResponseEntity<?> logoutUser(String authHeader) {
        try{
            userService.logout(authHeader);
            return ResponseEntity.status(200).body("Logout success, session closed");
        } catch (RuntimeException e){
            log.error("Ошибка логаута: {}", e.getMessage());
            return ResponseEntity.status(400).body("Logout error");
        }
    }

    @Override
    public ResponseEntity<?> refreshAccessToken(String refreshToken) {
        try{
            return ResponseEntity.status(200).body(userService.refreshAccessToken(refreshToken));
        } catch (RuntimeException e){
            return ResponseEntity.status(400).body("Refresh token error");
        }
    }

    /// Ошибки валидации
    public String handleValidationErrors(BindingResult bindingResult) {
        StringBuilder errorMessage = new StringBuilder("\n");
        for (FieldError error : bindingResult.getFieldErrors()) {
            errorMessage.append(error.getField())
                    .append(": ")
                    .append(error.getDefaultMessage())
                    .append("\n");
        }

        return new String(errorMessage);
    }
}

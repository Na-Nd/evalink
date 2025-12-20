package ru.nand.authservice.service;

import lombok.RequiredArgsConstructor;
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
import ru.nand.authservice.util.exception.WrongPasswordException;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RedisService redisService;
    private final UserService userService;
    private final NotificationUtil notificationUtil;
    private final RestClient accountServiceRestClient;

    @Override
    public ResponseEntity<?> registerUser(RegisterDTO registerDTO, BindingResult bindingResult) {
        // Ошибки в форме
        if(bindingResult.hasErrors()) {
            // То формируем строку с ошибками и возвращаем её
            return ResponseEntity.status(400).body("Validation Errors: " + handleValidationErrors(bindingResult));
        }

        // Генерация кода
        String verificationCode = String.valueOf((int) (Math.random() * 9000) + 1000);

        // Сохранение временных данных в Redis
        redisService.saveVerificationCode("verification_code:" + registerDTO.getEmail(), verificationCode, 5, TimeUnit.MINUTES);
        redisService.savePendingRegistration("pending_registration:" + registerDTO.getEmail(), registerDTO, 5, TimeUnit.MINUTES);

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
        String savedCode = redisService.getVerificationCode("verification_code:" + email);
        RegisterDTO savedRegisterDTO = redisService.getPendingRegistration("pending_registration:" + email);

        // Если чего-то нет - значит код истек или данные невалидные
        if (savedCode == null || savedRegisterDTO == null) return ResponseEntity.status(400).body("The verification code has expired or email/code is invalid");

        if(!savedCode.equals(code)) return ResponseEntity.status(400).body("Invalid verification code");

        // Чистим
        redisService.delete("verification_code:" + email);
        redisService.delete("pending_registration:" + email);

        // Отправляем registerDTO по restClient в сервис аккаунтов
        try{
            log.info("Отправка запроса в серсис аккаунтов");
            ResponseEntity<Void> responseEntity = accountServiceRestClient
                    .post()
                    .uri("/api/account")
                    .body(savedRegisterDTO)
                    .retrieve()
                    .toBodilessEntity();

            if (responseEntity.getStatusCode().is2xxSuccessful()){
                // Если всё норм и акк создался - просим залогиниться
                return ResponseEntity.status(200).body("Account created, please login");
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
        if(bindingResult.hasErrors()) {
            return ResponseEntity.status(400).body("Validation Errors: " + handleValidationErrors(bindingResult));
        }

        // Ищем пользователя с такими данными, сверяем хэш пароля с хэшом пароля из БД
        try{
            return ResponseEntity.status(200).body(userService.login(loginDTO));
        } catch (WrongPasswordException e){
            log.error("Ошибка при логине пользователя {}", loginDTO.getUsername());
            return ResponseEntity.status(403).body(e.getMessage());
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

package ru.nand.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.nand.authservice.entity.ENUMS.ROLE;
import ru.nand.authservice.entity.User;
import ru.nand.authservice.entity.dto.RegisterDTO;
import ru.nand.authservice.entity.dto.TokenResponse;
import ru.nand.authservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, SessionService sessionService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public TokenResponse createUser(RegisterDTO registerDTO) throws RuntimeException{
        // Проверяем, существует ли пользователь с таким email или username
        if (userRepository.findByEmail(registerDTO.getEmail()).isPresent()) {
            log.error("Пользовательс такой почтой уже существует: {}", registerDTO.getEmail());
            throw new RuntimeException("User with this email already exists");
        }
        if (userRepository.findByUsername(registerDTO.getUsername()).isPresent()) {
            log.error("Пользователь с таким именем уже существует: {}", registerDTO.getUsername());
            throw new RuntimeException("User with this username already exists");
        }

        log.info("Создание новго пользователя");
        // Создаем нового пользоваетеля
        User user = User.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .registrationDate(LocalDateTime.now())
                .role(ROLE.ROLE_USER)
                .build();


        userRepository.save(user);
        log.debug("Пользователь {} успешно сохранен", user.getUsername());

        // Создаем сессию и получаем DTO для отправки в сервис аккаунтов, из которого можно получить пару токенов и вернуть клиенту
        try{
            return sessionService.createSession(user);
        } catch (RuntimeException e){
            log.error("Ошибка при создании сессии для пользователя {}: {}", user.getUsername(), e.getMessage());
            throw e; // Прокидываем дальше
        }
    }

    @Override
    public TokenResponse login(User user) {
        return sessionService.createSession(user); // Исключение из createSession() ловим в AuthService
    }

    @Override
    public void logout(String authHeader) throws RuntimeException {
        sessionService.deactivateSessionByAccessToken(authHeader);
    }

    @Override
    public TokenResponse refreshAccessToken(String refreshToken) {
        return sessionService.refreshAccessToken(refreshToken);
    }
}

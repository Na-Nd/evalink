package ru.nand.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nand.notificationservice.entity.User;
import ru.nand.notificationservice.entity.dto.UserUpdateRequest;
import ru.nand.notificationservice.repository.UserRepository;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /// Работает со слушателем Kafka, принимает события зарегистрированной или измененой сущности пользователя и соответственно вносит изменения в свою таблицу-реплику пользователей
    @Override
    public void handleAccountEvent(UserUpdateRequest userUpdateRequest) {
        // Определяем тип события для определения дальнейшей логики
        if (Objects.equals(userUpdateRequest.getEventType(), "CREATE")){
            log.debug("Обрабатывается событие типа CREATE");
            // Если это созданный пользователь, то создаем его в текущей реплике таблицы
            User user = User.builder()
                    .id(userUpdateRequest.getId())
                    .username(userUpdateRequest.getUsername())
                    .email(userUpdateRequest.getEmail())
                    .build();

            userRepository.save(user);
            log.debug("Пользователь {} сохранен", user.getUsername());

        } else {
            // В противном случае - это обновленный пользователь, изменяем его в текущей реплике таблицы
            User user = userRepository.findById(userUpdateRequest.getId())
                    .orElseThrow(() -> new RuntimeException("User not found")); // На самом деле такого быть не может, инече бы нарушилась консистентность реплик и оригинала

            // Смысла проверять каждое поле нет, проще перезаписать
            user.setUsername(userUpdateRequest.getUsername());
            user.setEmail(userUpdateRequest.getEmail());
            userRepository.save(user);
            log.debug("Пользователь {} изменён", user.getUsername());
        }
    }
}

package ru.nand.notificationservice.service;

import ru.nand.notificationservice.entity.User;
import ru.nand.notificationservice.entity.dto.UserUpdateRequest;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    void handleAccountEvent(UserUpdateRequest userUpdateRequest);
}

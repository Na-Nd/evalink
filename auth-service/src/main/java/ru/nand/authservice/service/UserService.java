package ru.nand.authservice.service;

import ru.nand.authservice.entity.User;
import ru.nand.authservice.entity.dto.RegisterDTO;
import ru.nand.authservice.entity.dto.TokenResponse;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    TokenResponse createUser(RegisterDTO registerDTO);
    TokenResponse login(User user);
    TokenResponse refreshAccessToken(String refreshToken);
    void logout(String authHeader);
}

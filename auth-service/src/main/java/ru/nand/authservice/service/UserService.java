package ru.nand.authservice.service;

import ru.nand.authservice.entity.User;
import ru.nand.authservice.entity.dto.LoginDTO;
import ru.nand.authservice.entity.dto.RegisterDTO;
import ru.nand.authservice.entity.dto.TokenResponse;
import ru.nand.authservice.entity.dto.UserUpdateRequest;
import ru.nand.authservice.util.exception.WrongPasswordException;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    TokenResponse login(LoginDTO loginDTO) throws WrongPasswordException;
    TokenResponse refreshAccessToken(String refreshToken);
    void logout(String authHeader);
    void handleAccountEvent(UserUpdateRequest userUpdateRequest);
}

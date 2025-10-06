package ru.nand.authservice.service;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import ru.nand.authservice.entity.dto.LoginDTO;
import ru.nand.authservice.entity.dto.RegisterDTO;

public interface AuthService {
    ResponseEntity<?> registerUser(RegisterDTO registerDTO, BindingResult bindingResult);
    ResponseEntity<?> verifyAndRegisterUser(String email, String code);
    ResponseEntity<?> loginUser(LoginDTO loginDTO, BindingResult bindingResult);
    ResponseEntity<?> logoutUser(String authHeader);
    ResponseEntity<?> refreshAccessToken(String refreshToken);
}

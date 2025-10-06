package ru.nand.authservice.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "Имя пользователя не должно быть пустое")
    private String username;

    @NotBlank(message = "Пароль не должен быть пустым")
    private String password;

    private String requestId;
}

package ru.nand.authservice.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "Username can not be empty")
    private String username;

    @NotBlank(message = "Email must be in the correct format")
    private String email;

    @NotBlank(message = "Password can not be empty")
    private String password;

    private String requestId;

    private boolean emailVerified;

}

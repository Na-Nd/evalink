package ru.nand.authservice.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    private Long id;

    private String username;

    private String email;

    private String password;

    @JsonProperty("event_type")
    private String eventType;
}

package ru.nand.notificationservice.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

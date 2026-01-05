package ru.nand.postservice.entity.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PostCreateDTO {
    private Long authorId;
    private String text;
    private List<String> tags;
}

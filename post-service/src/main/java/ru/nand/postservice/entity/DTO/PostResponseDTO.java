package ru.nand.postservice.entity.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class PostResponseDTO {
    private Long id;
    private Long authorId;
    private String text;
    private List<String> tags;
    private OffsetDateTime dateOfPublication;
    private OffsetDateTime dateOfUpdate;
    private List<String> imageUrls;
}

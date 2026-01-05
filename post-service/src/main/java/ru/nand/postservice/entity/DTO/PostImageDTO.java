package ru.nand.postservice.entity.DTO;


import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class PostImageDTO {

    @NotNull(message = "Image must be not null")
    private MultipartFile file;

}

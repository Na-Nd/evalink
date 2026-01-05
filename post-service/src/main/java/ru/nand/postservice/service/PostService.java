package ru.nand.postservice.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nand.postservice.entity.DTO.PostCreateDTO;
import ru.nand.postservice.entity.DTO.PostResponseDTO;

public interface PostService {
    Mono<PostResponseDTO> createPost(PostCreateDTO dto, FilePart filePart);
    Flux<PostResponseDTO> getAllPosts();
}

package ru.nand.postservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nand.postservice.entity.DTO.PostCreateDTO;
import ru.nand.postservice.entity.DTO.PostResponseDTO;
import ru.nand.postservice.entity.PostImage;
import ru.nand.postservice.service.PostService;
import ru.nand.postservice.util.exception.ImageUploadException;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(path = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<PostResponseDTO>> createPost(@RequestPart("post") Mono<PostCreateDTO> postDtoMono,
                                                            @RequestPart("file") Mono<FilePart> filePartMono) {
        return postDtoMono.zipWith(filePartMono, (dto, file) -> postService.createPost(dto, file))
                .flatMap(innerMono -> innerMono)
                .map(resp -> ResponseEntity.status(201).body(resp));
    }

    @GetMapping("/")
    public Flux<PostResponseDTO> getAll() {
        return postService.getAllPosts();
    }
}

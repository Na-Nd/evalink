package ru.nand.postservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.nand.postservice.entity.DTO.PostCreateDTO;
import ru.nand.postservice.entity.DTO.PostResponseDTO;
import ru.nand.postservice.entity.Post;
import ru.nand.postservice.entity.PostImage;
import ru.nand.postservice.repository.PostImageRepository;
import ru.nand.postservice.repository.PostRepository;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final ImageService imageService;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, PostImageRepository postImageRepository, ImageService imageService) {
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.imageService = imageService;
    }

    /// Создание поста
    @Override
    public Mono<PostResponseDTO> createPost(PostCreateDTO dto, FilePart filePart) {
        Post post = Post.builder()
                .authorId(dto.getAuthorId())
                .text(dto.getText())
                .tags(dto.getTags())
                .dateOfPublication(OffsetDateTime.now())
                .dateOfUpdate(OffsetDateTime.now())
                .build();

        return postRepository.save(post)
                .flatMap(savedPost ->
                        imageService.upload(filePart)
                                .flatMap(filename -> {
                                    PostImage img = PostImage.builder()
                                            .postId(savedPost.getId())
                                            .filename(filename)
                                            .build();

                                    return postImageRepository.save(img).thenReturn(savedPost.getId());
                                })
                                .flatMap(postId -> assemblePostResponse(postId))
                );

    }

    @Override
    public Flux<PostResponseDTO> getAllPosts() {
        return postRepository.findAllByOrderByDateOfPublicationDesc().flatMap(post -> assemblePostResponse(post.getId()));
    }

    private Mono<PostResponseDTO> assemblePostResponse(Long postId) {
        return postRepository.findById(postId)
                .flatMap(postEntity ->
                        postImageRepository.findByPostId(postId)
                                .flatMap(img -> imageService.generatePresignedUrl(img.getFilename(), Duration.ofHours(24)))
                                .collectList()
                                .map(urls -> PostResponseDTO.builder()
                                        .id(postEntity.getId())
                                        .authorId(postEntity.getAuthorId())
                                        .text(postEntity.getText())
                                        .tags(postEntity.getTags())
                                        .dateOfPublication(postEntity.getDateOfPublication())
                                        .dateOfUpdate(postEntity.getDateOfUpdate())
                                        .imageUrls(urls)
                                        .build()
                                )
                );
    }
}

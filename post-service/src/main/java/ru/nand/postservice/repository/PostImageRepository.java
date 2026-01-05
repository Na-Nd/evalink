package ru.nand.postservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import ru.nand.postservice.entity.PostImage;

public interface PostImageRepository extends R2dbcRepository<PostImage, Long> {
    Flux<PostImage> findByPostId(Long postId);
}

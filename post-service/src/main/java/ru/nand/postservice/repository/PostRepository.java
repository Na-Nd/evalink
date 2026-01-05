package ru.nand.postservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import ru.nand.postservice.entity.Post;

public interface PostRepository extends R2dbcRepository<Post, Long> {
    Flux<Post> findAllByOrderByDateOfPublicationDesc();
}

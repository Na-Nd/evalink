package ru.nand.postservice.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import java.time.Duration;

public interface ImageService {
    Mono<String> upload(FilePart filePart);
    Mono<String> generatePresignedUrl(String filename, Duration expiry);
}

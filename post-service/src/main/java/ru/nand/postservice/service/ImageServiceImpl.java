package ru.nand.postservice.service;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.nand.postservice.util.exception.ImageUploadException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class ImageServiceImpl implements ImageService {

    @Value("${minio.bucket}")
    private String minioBucket;

    private final MinioClient minioClient;

    public ImageServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public Mono<String> upload(FilePart filePart) {
        if (filePart == null) {
            return Mono.error(new ImageUploadException("FilePart is null"));
        }

        // Проверка бакета
        Mono<Void> ensureBucket = Mono.fromCallable(() -> {
                    try {
                        boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                                .bucket(minioBucket)
                                .build());
                        if (!found) {
                            log.warn("Бакет '{}' не найден, производится создание", minioBucket);
                            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
                        }
                        return null;

                    } catch (Exception e) {
                        throw new RuntimeException("Бакет не найден или не удалось создать: " + e.getMessage(), e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();

        // Создание верменного файла и перенос FilePart в него
        Mono<Path> tempFileMono = Mono.fromCallable(() -> {
                    String original = filePart.filename();
                    String ext = getExtension(original);
                    return Files.createTempFile("upload-", "." + ext);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(tempPath -> filePart.transferTo(tempPath).thenReturn(tempPath));

        // Загрузка в MinIO
        return ensureBucket.then(
                tempFileMono.flatMap(tempPath -> {
                    Mono<String> uploadMono = Mono.fromCallable(() -> {
                                String original = filePart.filename();
                                String extension = getExtension(original);
                                String generatedName = UUID.randomUUID().toString() + "." + extension;

                                long size = Files.size(tempPath);

                                try (InputStream inputStream = Files.newInputStream(tempPath)) {
                                    PutObjectArgs args = PutObjectArgs.builder()
                                            .bucket(minioBucket)
                                            .object(generatedName)
                                            .stream(inputStream, size, -1)
                                            .build();
                                    minioClient.putObject(args);
                                }
                                return generatedName;
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .onErrorMap(e -> new ImageUploadException("Failed to upload image to MinIO: " + e.getMessage()));

                    // Отчистился ли временный файл
                    return uploadMono.doFinally(signalType -> {
                        try {
                            Files.deleteIfExists(tempPath);
                        } catch (Exception ex) {
                            log.warn("Не удалось удалить временный файл {}: {}", tempPath, ex.getMessage());
                        }
                    });
                })
        );
    }

    @Override
    public Mono<String> generatePresignedUrl(String filename, Duration expiry) {
        if (filename == null || filename.isBlank()) {
            return Mono.error(new IllegalArgumentException("Filename is required"));
        }

        final Duration effectiveExpiry =
                (expiry == null || expiry.isNegative() || expiry.isZero())
                        ? Duration.ofHours(1)
                        : expiry;

        final int expirySeconds =
                (int) Math.min(effectiveExpiry.getSeconds(), Integer.MAX_VALUE);

        return Mono.fromCallable(() -> {
                    try {
                        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(minioBucket)
                                .object(filename)
                                .expiry(expirySeconds)
                                .build();

                        return minioClient.getPresignedObjectUrl(args);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e ->
                        new ImageUploadException("Failed to generate presigned URL: " + e.getMessage())
                );
    }


    private String getExtension(String originalFilename) {
        if (originalFilename == null) return "bin";
        int idx = originalFilename.lastIndexOf('.');
        if (idx == -1 || idx == originalFilename.length() - 1) {
            return "bin";
        }
        return originalFilename.substring(idx + 1);
    }
}

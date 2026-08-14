package io.everyonecodes.project_module.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;

    private static final int MAX_KEYS_PER_BATCH = 1000;

    public S3StorageService(S3Client s3client,
                            @Value("${aws.s3.bucket-name}") String bucketName,
                            @Value("${aws.region}") String region) {
        this.s3Client = s3client;
        this.bucketName = bucketName;
        this.region = region;
    }

    public String uploadFile(MultipartFile file) {
        var key = UUID.randomUUID() + extensionOf(file.getOriginalFilename());

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .contentType(file.getContentType())
                                    .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to upload file to S3", e);
        }

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucketName, region, key);
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    public void deleteFile(String url) {
        var key = keyOf(url);

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                                   .bucket(bucketName)
                                   .key(key)
                                   .build()
        );
    }

    public void deleteFiles(List<String> urls) {
        if (urls.isEmpty()) {
            return;
        }

        var objectIds = urls.stream()
                            .map(this::keyOf)
                            .map(key -> ObjectIdentifier.builder().key(key).build())
                            .toList();

        // S3 has a limit of 1000 keys per batch, so I split it in chunks of that size
        for (int i = 0; i < objectIds.size(); i += MAX_KEYS_PER_BATCH) {
            var chunk = objectIds.subList(i, Math.min(i + MAX_KEYS_PER_BATCH, objectIds.size()));
            s3Client.deleteObjects(
                    DeleteObjectsRequest.builder()
                                        .bucket(bucketName)
                                        .delete(Delete.builder().objects(chunk).build())
                                        .build()
            );
        }
    }

    private String keyOf(String url) {
        if (url == null || !url.contains("/")) {
            throw new IllegalArgumentException("Cannot extract S3 key from malformed URL: " + url);
        }
        return url.substring(url.lastIndexOf('/') + 1);
    }
}

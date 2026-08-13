package io.everyonecodes.project_module.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;

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

    // https://kunsthaben-artwork-images-996241421825-eu-north-1-an.s3.eu-north-1.amazonaws.com/29721856-00a4-4d43-9eae-a09ce3ba8d89.jpg
    private String keyOf(String url) {
        if (url == null || !url.contains("/")) {
            throw new IllegalArgumentException("Cannot extract S3 key from malformed URL: " + url);
        }
        return url.substring(url.lastIndexOf('/') + 1);
    }
}

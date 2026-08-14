package io.everyonecodes.project_module.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    S3StorageService service;

    @Mock
    S3Client s3Client;

    @BeforeEach
    void setup() {
        service = new S3StorageService(s3Client, "test-bucket", "eu-north-1");
    }

    @Test
    void uploadFileSuccessfully() {
        var file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "some bytes".getBytes());

        var url = service.uploadFile(file);

        var captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        var request = captor.getValue();

        assertEquals("test-bucket", request.bucket());
        assertEquals("image/jpeg", request.contentType());
        assertTrue(request.key().endsWith(".jpg"));
        assertEquals("https://test-bucket.s3.eu-north-1.amazonaws.com/" + request.key(), url);
    }

    @Test
    void uploadFileWithoutExtensionInFilename() {
        var file = new MockMultipartFile("file", "no-extension", "image/jpeg", "some bytes".getBytes());

        var url = service.uploadFile(file);

        var captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertFalse(captor.getValue().key().contains("."));
        assertTrue(url.endsWith(captor.getValue().key()));
    }

    @Test
    void uploadFileWithNullOriginalFilename() {
        var file = new MockMultipartFile("file", null, "image/jpeg", "some bytes".getBytes());

        var url = service.uploadFile(file);

        var captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertFalse(captor.getValue().key().contains("."));
        assertTrue(url.endsWith(captor.getValue().key()));
    }

    @Test
    void uploadFileWrapsIOException() throws IOException {
        var file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("avatar.jpg");
        when(file.getInputStream()).thenThrow(new IOException("disk error"));

        assertThrows(UncheckedIOException.class, () -> service.uploadFile(file));
    }

    @Test
    void deleteFileSuccessfully() {
        service.deleteFile("https://test-bucket.s3.eu-north-1.amazonaws.com/abc123.jpg");

        var captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("test-bucket", captor.getValue().bucket());
        assertEquals("abc123.jpg", captor.getValue().key());
    }

    @Test
    void deleteFileWithNullUrlThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteFile(null));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteFileWithMalformedUrlThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteFile("no-slash-here"));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteFilesWithEmptyListDoesNothing() {
        service.deleteFiles(List.of());

        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void deleteFilesWithMalformedUrlThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteFiles(List.of("bad-url")));
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void deleteFilesUnderBatchLimitSendsOneRequest() {
        var urls = List.of(
                "https://test-bucket.s3.eu-north-1.amazonaws.com/a.jpg",
                "https://test-bucket.s3.eu-north-1.amazonaws.com/b.jpg"
        );

        service.deleteFiles(urls);

        var captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client, times(1)).deleteObjects(captor.capture());
        assertEquals(2, captor.getValue().delete().objects().size());
    }

    @Test
    void deleteFilesOverBatchLimitSplitsIntoChunks() {
        var urls = IntStream.range(0, 1200)
                             .mapToObj(i -> "https://test-bucket.s3.eu-north-1.amazonaws.com/image-" + i + ".jpg")
                             .toList();

        service.deleteFiles(urls);

        var captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client, times(2)).deleteObjects(captor.capture());

        var requests = captor.getAllValues();
        assertEquals(1000, requests.get(0).delete().objects().size());
        assertEquals(200, requests.get(1).delete().objects().size());
    }
}

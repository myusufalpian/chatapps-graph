package id.xyz.chatapps_graph.applications.service;

import static graphql.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.service.adapters.MinioService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

  @Mock
  private MinioClient minioClient;

  private MinioService minioService;
  private final String BUCKET_NAME = "test-bucket";

  @BeforeEach
  void setUp() {
    minioService = new MinioService(minioClient, BUCKET_NAME);
  }

  @Test
  @DisplayName("Positive: Upload should succeed when bucket already exists")
  void uploadFile_Success_BucketExists() throws Exception {
    String fileName = "avatar.png";
    String contentType = MediaType.IMAGE_PNG_VALUE;
    long size = 1024L;
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);

    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

    String result = minioService.uploadFile(fileName, inputStream, contentType, size);

    assertEquals(fileName, result);

    verify(minioClient, times(1)).bucketExists(any(BucketExistsArgs.class));
    verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class)); // Should NOT create bucket
    verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
  }

  @Test
  @DisplayName("Positive: Upload should succeed and create bucket if it does not exist")
  void uploadFile_Success_BucketCreated() throws Exception {
    String fileName = "doc.pdf";
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);

    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

    String result = minioService.uploadFile(fileName, inputStream, "application/pdf", 100L);

    assertEquals(fileName, result);

    verify(minioClient).bucketExists(any(BucketExistsArgs.class));
    verify(minioClient).makeBucket(any(MakeBucketArgs.class)); // MUST create bucket
    verify(minioClient).putObject(any(PutObjectArgs.class));
  }

  @Test
  @DisplayName("Negative: Should throw exception when MinIO connection fails")
  void uploadFile_Failure_MinioException() throws Exception {
    String fileName = "error.png";
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);

    when(minioClient.bucketExists(any(BucketExistsArgs.class)))
        .thenThrow(new RuntimeException("Connection Refused"));

    Exception exception = assertThrows(RuntimeException.class, () -> {
      minioService.uploadFile(fileName, inputStream, "image/png", 100L);
    });

    assertTrue(exception.getMessage().contains("Connection Refused") ||
        exception.getCause().getMessage().contains("Connection Refused"));

    verify(minioClient, never()).putObject(any(PutObjectArgs.class));
  }
}

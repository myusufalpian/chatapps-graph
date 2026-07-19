package id.xyz.chatapps_graph.applications.service.adapters;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.ErrorKeyConstants;
import id.xyz.chatapps_graph.infrastructure.utility.ExceptionUtil;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MinioService implements FileStoragePort {

  private final MinioClient minioClient;
  private final String bucketName;

  @Autowired
  public MinioService(MinioClient minioClient,
      id.xyz.chatapps_graph.infrastructure.config.properties.MinioProperties minioProperties) {
    this.minioClient = minioClient;
    this.bucketName = minioProperties.getBucket();
  }

  @Override
  public String uploadFile(String fileName, InputStream inputStream, String contentType, long size) {
    try {
      boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
      if (!found) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      }

      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(bucketName)
              .object(fileName)
              .stream(inputStream, size, -1)
              .contentType(contentType)
              .build()
      );

      return fileName;

    } catch (Exception e) {
      throw ExceptionUtil.error(ErrorKeyConstants.INTERNAL_SERVER_ERROR,
          String.format(ErrorKeyConstants.STORAGE_ERROR, e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
    }
  }

  @Override
  public void deleteFile(String fileName) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(bucketName)
              .object(fileName)
              .build()
      );
    } catch (Exception e) {
      log.error("Failed to delete file from Minio: {}", fileName, e);
    }
  }

  @Override
  public InputStream downloadFile(String fileName) {
    try {
      return minioClient.getObject(GetObjectArgs.builder()
          .bucket(bucketName).object(fileName).build());
    } catch (Exception e) {
      throw ExceptionUtil.error(ErrorKeyConstants.INTERNAL_SERVER_ERROR,
          String.format(ErrorKeyConstants.STORAGE_ERROR, e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
    }
  }

  @Override
  public String createPresignedUrl(String fileName, Duration expiry) {
    try {
      return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
          .method(Method.GET).bucket(bucketName).object(fileName)
          .expiry((int) expiry.toSeconds(), java.util.concurrent.TimeUnit.SECONDS)
          .build());
    } catch (Exception e) {
      throw ExceptionUtil.error(ErrorKeyConstants.INTERNAL_SERVER_ERROR,
          String.format(ErrorKeyConstants.STORAGE_ERROR, e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
    }
  }
}

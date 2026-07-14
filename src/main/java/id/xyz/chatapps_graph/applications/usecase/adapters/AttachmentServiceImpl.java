package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.AttachmentType;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.properties.MediaProperties;
import id.xyz.chatapps_graph.infrastructure.utility.ImageProcessingService;
import id.xyz.chatapps_graph.infrastructure.utility.VideoThumbnailService;
import id.xyz.chatapps_graph.infrastructure.utility.ImageProcessingService.ImageProcessingResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

  private final FileStoragePort fileStoragePort;
  private final AttachmentRepository attachmentRepository;
  private final MediaProperties mediaProperties;
  private final ImageProcessingService imageProcessingService;
  private final VideoThumbnailService videoThumbnailService;
  private final UserRepository userRepository;

  @Override
  public Attachment validateAndUpload(MultipartFile file, String attachmentType, Long uploaderId) {
    AttachmentType type = AttachmentType.valueOf(attachmentType);
    validateFile(file, type);

    User uploader = userRepository.findById(uploaderId)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", "User not found"));
    String uploaderUuid = uploader.getUserUuid();

    String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
    String basePath = "chat/" + uploaderUuid + "/" + System.currentTimeMillis();
    String filePath = basePath + "_" + originalFilename;
    String thumbBaseName = basePath.substring(basePath.lastIndexOf('/') + 1);
    String thumbnailPath = null;

    try {
      if (type == AttachmentType.IMAGE) {
        byte[] rawBytes = file.getBytes();
        ImageProcessingResult result = imageProcessingService.compressAndThumbnail(
            new ByteArrayInputStream(rawBytes),
            mediaProperties.getImage().getMaxDimension(),
            mediaProperties.getImage().getQuality(),
            mediaProperties.getImage().getThumbnailDimension(),
            mediaProperties.getImage().getThumbnailQuality());
        byte[] compressed = result.compressed();
        fileStoragePort.uploadFile(filePath, new ByteArrayInputStream(compressed), "image/jpeg", compressed.length);

        byte[] thumbnail = result.thumbnail();
        String thumbPath = "thumbnails/" + uploaderUuid + "/" + thumbBaseName + "_thumb.jpg";
        fileStoragePort.uploadFile(thumbPath, new ByteArrayInputStream(thumbnail), "image/jpeg", thumbnail.length);
        thumbnailPath = thumbPath;
      } else if (type == AttachmentType.VIDEO) {
        fileStoragePort.uploadFile(filePath, file.getInputStream(), file.getContentType(), file.getSize());
        thumbnailPath = generateVideoThumbnail(file, uploaderUuid, thumbBaseName);
      } else {
        fileStoragePort.uploadFile(filePath, file.getInputStream(), file.getContentType(), file.getSize());
      }
    } catch (OutOfMemoryError e) {
      log.error("Image processing OOM for file size: {}", file.getSize());
      throw new GeneralException(HttpStatus.PAYLOAD_TOO_LARGE.value(), "IMAGE_TOO_LARGE",
          "Image is too large to process");
    } catch (IOException e) {
      throw new GeneralException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UPLOAD_FAILED", "Failed to process and upload file");
    }

    return attachmentRepository.save(Attachment.builder()
        .uploaderId(uploaderId)
        .fileName(originalFilename)
        .filePath(filePath)
        .fileSize(file.getSize())
        .contentType(file.getContentType())
        .attachmentType(attachmentType)
        .thumbnailPath(thumbnailPath)
        .build());
  }

  @Override
  public void deleteFile(String filePath) {
    fileStoragePort.deleteFile(filePath);
  }

  private String generateVideoThumbnail(MultipartFile file, String uploaderUuid, String thumbBaseName) {
    Path tempFile = null;
    try {
      tempFile = Files.createTempFile("video_", ".tmp");
      file.transferTo(tempFile.toFile());

      byte[] thumbnail = videoThumbnailService.extractFirstFrame(tempFile,
          mediaProperties.getImage().getThumbnailDimension());
      if (thumbnail == null) {
        return null;
      }

      String thumbPath = "thumbnails/" + uploaderUuid + "/" + thumbBaseName + "_thumb.jpg";
      fileStoragePort.uploadFile(thumbPath, new ByteArrayInputStream(thumbnail), "image/jpeg", thumbnail.length);
      return thumbPath;
    } catch (IOException e) {
      log.warn("Video thumbnail generation failed: {}", e.getMessage());
      return null;
    } finally {
      if (tempFile != null) {
        try {
          Files.deleteIfExists(tempFile);
        } catch (IOException e) {
          log.warn("Failed to delete temp video file: {}", e.getMessage());
        }
      }
    }
  }

  private void validateFile(MultipartFile file, AttachmentType type) {
    if (file.getSize() > mediaProperties.getMaxFileSize()) {
      throw new GeneralException(HttpStatus.PAYLOAD_TOO_LARGE.value(), "FILE_TOO_LARGE",
          "File size exceeds " + (mediaProperties.getMaxFileSize() / 1024 / 1024) + "MB limit");
    }
    if (!type.isContentTypeAllowed(file.getContentType())) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "INVALID_CONTENT_TYPE",
          "Content type " + file.getContentType() + " not allowed for " + type.name());
    }
  }
}

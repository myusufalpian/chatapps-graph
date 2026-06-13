package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.enums.AttachmentType;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

  private static final long MAX_FILE_SIZE = 15L * 1024 * 1024;

  private final FileStoragePort fileStoragePort;
  private final AttachmentRepository attachmentRepository;

  @Override
  public Attachment validateAndUpload(MultipartFile file, String attachmentType, Long uploaderId, String uploaderUuid) {
    AttachmentType type = AttachmentType.valueOf(attachmentType);
    validateFile(file, type);

    String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
    String filePath = "chat/" + uploaderUuid + "/" + System.currentTimeMillis() + "_" + originalFilename;

    try {
      fileStoragePort.uploadFile(filePath, file.getInputStream(), file.getContentType(), file.getSize());
    } catch (IOException e) {
      throw new GeneralException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UPLOAD_FAILED", "Failed to upload file");
    }

    return attachmentRepository.save(Attachment.builder()
        .uploaderId(uploaderId)
        .fileName(originalFilename)
        .filePath(filePath)
        .fileSize(file.getSize())
        .contentType(file.getContentType())
        .attachmentType(attachmentType)
        .build());
  }

  @Override
  public void deleteFile(String filePath) {
    fileStoragePort.deleteFile(filePath);
  }

  private void validateFile(MultipartFile file, AttachmentType type) {
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new GeneralException(HttpStatus.PAYLOAD_TOO_LARGE.value(), "FILE_TOO_LARGE", "File size exceeds 15MB limit");
    }
    if (!type.isContentTypeAllowed(file.getContentType())) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "INVALID_CONTENT_TYPE",
          "Content type " + file.getContentType() + " not allowed for " + type.name());
    }
  }
}

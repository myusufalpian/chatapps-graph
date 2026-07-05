package id.xyz.chatapps_graph.infrastructure.mapper;

import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.framework.dto.AttachmentResponse;
import org.springframework.util.StringUtils;

public class AttachmentMapper {

  private AttachmentMapper() {}

  public static AttachmentResponse toResponse(Attachment attachment, String minioBaseUrl) {
    if (attachment == null) {
      return null;
    }
    String thumbnailUrl = null;
    if (StringUtils.hasLength(attachment.getThumbnailPath())) {
      thumbnailUrl = minioBaseUrl + "/" + attachment.getThumbnailPath();
    }
    return new AttachmentResponse(
        attachment.getAttachmentUuid(),
        attachment.getFileName(),
        attachment.getFilePath(),
        attachment.getFileSize(),
        attachment.getContentType(),
        attachment.getAttachmentType(),
        thumbnailUrl
    );
  }
}

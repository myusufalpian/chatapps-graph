package id.xyz.chatapps_graph.infrastructure.mapper;

import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.framework.dto.AttachmentResponse;

public class AttachmentMapper {

  private AttachmentMapper() {}

  public static AttachmentResponse toResponse(Attachment attachment) {
    if (attachment == null) {
      return null;
    }
    return new AttachmentResponse(
        attachment.getAttachmentUuid(),
        attachment.getFileName(),
        attachment.getFilePath(),
        attachment.getFileSize(),
        attachment.getContentType(),
        attachment.getAttachmentType()
    );
  }
}

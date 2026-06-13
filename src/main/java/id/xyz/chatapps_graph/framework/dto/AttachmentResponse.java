package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record AttachmentResponse(
    String attachmentUuid,
    String fileName,
    String filePath,
    Long fileSize,
    String contentType,
    String attachmentType
) {}

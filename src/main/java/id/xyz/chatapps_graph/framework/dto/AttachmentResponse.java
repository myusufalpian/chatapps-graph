package id.xyz.chatapps_graph.framework.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AttachmentResponse(
    String attachmentUuid,
    String fileName,
    String filePath,
    Long fileSize,
    String contentType,
    String attachmentType,
    String thumbnailUrl
) {}

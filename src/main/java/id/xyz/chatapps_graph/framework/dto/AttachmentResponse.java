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
    String thumbnailUrl,
    Long voiceDurationMs,
    String voiceWaveform,
    String voiceCodec,
    Integer voiceBitrate,
    Integer voiceSampleRate,
    Short voiceChannelCount,
    String metadataStatus
) {
  public AttachmentResponse(String attachmentUuid, String fileName, String filePath, Long fileSize,
      String contentType, String attachmentType, String thumbnailUrl) {
    this(attachmentUuid, fileName, filePath, fileSize, contentType, attachmentType, thumbnailUrl,
        null, null, null, null, null, null, null);
  }
}

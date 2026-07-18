package id.xyz.chatapps_graph.framework.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VoiceMetadataTask(
    String taskId,
    String correlationId,
    Long attachmentId,
    int retryCount,
    OffsetDateTime createdAt) {

  public static VoiceMetadataTask create(Long attachmentId) {
    String id = UUID.randomUUID().toString();
    return new VoiceMetadataTask(id, id, attachmentId, 0, OffsetDateTime.now());
  }
}

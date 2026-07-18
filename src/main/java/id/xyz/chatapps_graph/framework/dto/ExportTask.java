package id.xyz.chatapps_graph.framework.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExportTask(String taskId, String correlationId, UUID exportUuid, Long userId,
                         OffsetDateTime snapshotAt, int retryCount, OffsetDateTime createdAt) {
  public static ExportTask create(UUID exportUuid, Long userId, OffsetDateTime snapshotAt) {
    String id = UUID.randomUUID().toString();
    return new ExportTask(id, id, exportUuid, userId, snapshotAt, 0, OffsetDateTime.now());
  }
}

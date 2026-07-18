package id.xyz.chatapps_graph.framework.dto;

import id.xyz.chatapps_graph.domain.enums.ExportJobStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExportJobResponse(UUID exportUuid, ExportJobStatus status, OffsetDateTime expiresAt,
                                OffsetDateTime snapshotAt, String downloadUrl) {}

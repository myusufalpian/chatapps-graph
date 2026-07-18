package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.framework.dto.ExportJobResponse;
import java.util.UUID;

public interface ExportService {
  ExportJobResponse request(Long userId);
  ExportJobResponse status(Long userId, UUID exportUuid);
  String downloadUrl(Long userId, UUID exportUuid);
}

package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.LinkPreview;

public interface LinkPreviewService {
  LinkPreview fetchPreview(String url);
  void processLinkPreviewTask(Long messageId, String url, String conversationUuid);
}

package id.xyz.chatapps_graph.domain.enums;

import java.util.List;

public enum AttachmentType {
  IMAGE(List.of("image/jpeg", "image/png", "image/gif", "image/webp")),
  VIDEO(List.of("video/mp4", "video/webm", "video/quicktime")),
  VOICE(List.of("audio/ogg", "audio/mp4", "audio/aac", "audio/x-m4a")),
  FILE(List.of());

  private final List<String> allowedContentTypes;

  AttachmentType(List<String> allowedContentTypes) {
    this.allowedContentTypes = allowedContentTypes;
  }

  public boolean isContentTypeAllowed(String contentType) {
    if (this == FILE) {
      return true;
    }
    return allowedContentTypes.contains(contentType);
  }
}

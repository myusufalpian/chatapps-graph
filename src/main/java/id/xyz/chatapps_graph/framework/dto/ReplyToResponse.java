package id.xyz.chatapps_graph.framework.dto;

public record ReplyToResponse(
    String messageUuid,
    String senderUuid,
    String content
) {}

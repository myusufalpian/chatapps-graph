package id.xyz.chatapps_graph.framework.dto;

public record WebSocketSendMessage(
    String conversationUuid,
    String recipientUuid,
    String content,
    String replyToMessageUuid
) {}

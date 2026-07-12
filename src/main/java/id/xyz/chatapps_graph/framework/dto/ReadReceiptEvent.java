package id.xyz.chatapps_graph.framework.dto;

public record ReadReceiptEvent(
    String eventType,
    String conversationUuid,
    String readerUuid,
    String status
) {}

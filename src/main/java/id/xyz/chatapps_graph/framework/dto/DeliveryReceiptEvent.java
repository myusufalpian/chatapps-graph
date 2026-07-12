package id.xyz.chatapps_graph.framework.dto;

import java.util.List;

public record DeliveryReceiptEvent(
    String eventType,
    String conversationUuid,
    List<String> messageUuids,
    String readerUuid,
    String status
) {}

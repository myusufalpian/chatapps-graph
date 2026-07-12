package id.xyz.chatapps_graph.framework.dto;

import java.util.List;

public record DeliveryReceiptRequest(
    String conversationUuid,
    List<String> messageUuids
) {}

package id.xyz.chatapps_graph.framework.dto;

import java.util.List;

public record CreateMultiChatRequest(
    List<String> participantUuids
) {}

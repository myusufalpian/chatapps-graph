package id.xyz.chatapps_graph.framework.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record MultiChatResponse(
    String conversationUuid,
    List<ParticipantSummary> participants
) {}

package id.xyz.chatapps_graph.framework.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ConversationListResponse(
    List<ConversationItemResponse> conversations,
    String nextCursor,
    boolean hasMore
) {}

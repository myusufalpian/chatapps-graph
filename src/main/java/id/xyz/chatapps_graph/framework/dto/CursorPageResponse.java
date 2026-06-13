package id.xyz.chatapps_graph.framework.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record CursorPageResponse<T>(
    List<T> messages,
    String nextCursor,
    boolean hasMore
) {}

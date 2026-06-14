package id.xyz.chatapps_graph.framework.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record MessageSearchResponse(
    List<SearchResultItem> results,
    String nextCursor,
    boolean hasMore
) {}

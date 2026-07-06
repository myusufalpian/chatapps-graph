package id.xyz.chatapps_graph.framework.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record GroupInfoResponse(
    String groupUuid,
    String name,
    String description,
    String avatarUrl,
    Boolean allowMemberAdd,
    List<GroupMemberResponse> members
) {}

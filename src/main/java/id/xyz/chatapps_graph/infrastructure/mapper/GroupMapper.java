package id.xyz.chatapps_graph.infrastructure.mapper;

import id.xyz.chatapps_graph.domain.entity.Group;
import id.xyz.chatapps_graph.domain.entity.GroupMember;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.framework.dto.GroupInfoResponse;
import id.xyz.chatapps_graph.framework.dto.GroupMemberResponse;
import java.util.List;

public class GroupMapper {

  public static GroupMemberResponse toMemberResponse(GroupMember member) {
    User user = member.getUser();
    return GroupMemberResponse.builder()
        .userUuid(user.getUserUuid())
        .fullName(user.getUserFullName())
        .profilePhoto(user.getProfilePhoto())
        .role(member.getMemberType())
        .build();
  }

  public static GroupInfoResponse toInfoResponse(Group group, List<GroupMemberResponse> members, String avatarUrl) {
    return GroupInfoResponse.builder()
        .groupUuid(group.getGroupUuid())
        .name(group.getGroupName())
        .description(group.getGroupDesc())
        .avatarUrl(avatarUrl)
        .allowMemberAdd(group.getAllowMemberAdd())
        .members(members)
        .build();
  }
}

package id.xyz.chatapps_graph.infrastructure.mapper;

import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.repository.projection.ConversationListProjection;
import id.xyz.chatapps_graph.framework.dto.ConversationItemResponse;
import id.xyz.chatapps_graph.framework.dto.ParticipantSummary;
import java.util.List;

public class ConversationMapper {

  private ConversationMapper() {}

  public static ConversationItemResponse toResponse(
      ConversationListProjection projection,
      List<ParticipantSummary> participants) {

    return toResponse(projection, participants, null, null);
  }

  public static ConversationItemResponse toResponse(
      ConversationListProjection projection,
      List<ParticipantSummary> participants,
      String groupName,
      String groupAvatarUrl) {

    return ConversationItemResponse.builder()
        .conversationUuid(projection.getConversationUuid())
        .conversationType(projection.getConversationType())
        .lastMessageAt(projection.getLastMessageAt())
        .lastMessagePreview(projection.getLastMessagePreview())
        .lastMessageType(projection.getLastMessageType())
        .unreadCount(projection.getUnreadCount())
        .isPinned(projection.getIsPinned())
        .isMuted(projection.getIsMuted())
        .participants(participants)
        .groupName(groupName)
        .groupAvatarUrl(groupAvatarUrl)
        .build();
  }

  public static ParticipantSummary toParticipantSummary(User user) {
    return ParticipantSummary.builder()
        .userUuid(user.getUserUuid())
        .fullName(user.getUserFullName())
        .profilePhoto(user.getProfilePhoto())
        .build();
  }
}

package id.xyz.chatapps_graph.infrastructure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.xyz.chatapps_graph.domain.repository.projection.ConversationListProjection;
import id.xyz.chatapps_graph.framework.dto.ConversationItemResponse;
import id.xyz.chatapps_graph.framework.dto.ParticipantSummary;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConversationMapperGroupTest {

  @Test
  @DisplayName("conversationList: GROUP type — includes groupName and groupAvatarUrl")
  void conversationList_GroupType_IncludesGroupNameAndAvatar() {
    ConversationListProjection projection = buildProjection("conv-uuid", "GROUP");
    List<ParticipantSummary> participants = List.of(
        ParticipantSummary.builder().userUuid("u1").fullName("Alice").build());

    ConversationItemResponse response = ConversationMapper.toResponse(
        projection, participants, "My Group", "http://minio:9000/bucket/groups/g-uuid/avatar.jpg");

    assertEquals("GROUP", response.conversationType());
    assertEquals("My Group", response.groupName());
    assertEquals("http://minio:9000/bucket/groups/g-uuid/avatar.jpg", response.groupAvatarUrl());
    assertEquals("conv-uuid", response.conversationUuid());
  }

  @Test
  @DisplayName("conversationList: PRIVATE type — no group fields")
  void conversationList_PrivateType_NoGroupFields() {
    ConversationListProjection projection = buildProjection("conv-uuid", "PRIVATE");
    List<ParticipantSummary> participants = List.of(
        ParticipantSummary.builder().userUuid("u1").fullName("Bob").build());

    ConversationItemResponse response = ConversationMapper.toResponse(projection, participants);

    assertEquals("PRIVATE", response.conversationType());
    assertNull(response.groupName());
    assertNull(response.groupAvatarUrl());
  }

  @Test
  @DisplayName("conversationList: GROUP without avatar — groupAvatarUrl null")
  void conversationList_GroupNoAvatar_NullAvatarUrl() {
    ConversationListProjection projection = buildProjection("conv-uuid", "GROUP");

    ConversationItemResponse response = ConversationMapper.toResponse(
        projection, List.of(), "Group Name", null);

    assertEquals("Group Name", response.groupName());
    assertNull(response.groupAvatarUrl());
  }

  private ConversationListProjection buildProjection(String uuid, String type) {
    return new ConversationListProjection() {
      @Override public Long getConversationId() { return 1L; }
      @Override public OffsetDateTime getLastMessageAt() { return OffsetDateTime.now(); }
      @Override public String getLastMessagePreview() { return "Hello"; }
      @Override public String getLastMessageType() { return "TEXT"; }
      @Override public Integer getUnreadCount() { return 0; }
      @Override public Boolean getIsPinned() { return false; }
      @Override public OffsetDateTime getPinnedAt() { return null; }
      @Override public Boolean getIsMuted() { return false; }
      @Override public String getConversationUuid() { return uuid; }
      @Override public String getConversationType() { return type; }
    };
  }
}

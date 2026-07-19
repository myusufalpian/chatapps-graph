package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.ConversationListService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.entity.Group;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.ConversationType;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.GroupRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.domain.repository.projection.ConversationListProjection;
import id.xyz.chatapps_graph.framework.dto.ConversationItemResponse;
import id.xyz.chatapps_graph.framework.dto.ConversationListResponse;
import id.xyz.chatapps_graph.framework.dto.ParticipantSummary;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.properties.MinioProperties;
import id.xyz.chatapps_graph.infrastructure.mapper.ConversationMapper;
import id.xyz.chatapps_graph.infrastructure.utility.CursorUtil;
import id.xyz.chatapps_graph.infrastructure.utility.CursorUtil.CursorPosition;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConversationListServiceImpl implements ConversationListService {

  private final ConversationParticipantRepository participantRepository;
  private final UserRepository userRepository;
  private final ConversationService conversationService;
  private final GroupRepository groupRepository;
  private final MinioProperties minioProperties;

  @Override
  @Transactional(readOnly = true)
  public ConversationListResponse listConversations(Long userId, String filter, String cursor, int limit) {
    int fetchLimit = Math.min(limit, 50);
    boolean isArchived = "ARCHIVED".equalsIgnoreCase(filter);

    CursorPosition cursorPosition = CursorUtil.parse(cursor);
    List<ConversationListProjection> rows;

    if (cursorPosition != null) {
      rows = fetchWithCursor(userId, isArchived, cursorPosition, filter, fetchLimit + 1);
    } else {
      rows = fetchFirstPage(userId, isArchived, filter, fetchLimit + 1);
    }

    boolean hasMore = rows.size() > fetchLimit;
    List<ConversationListProjection> resultRows = hasMore ? rows.subList(0, fetchLimit) : rows;

    if (resultRows.isEmpty()) {
      return ConversationListResponse.builder()
          .conversations(List.of())
          .nextCursor(null)
          .hasMore(false)
          .build();
    }

    // Batch fetch participants (prevent N+1)
    List<Long> conversationIds = resultRows.stream()
        .map(ConversationListProjection::getConversationId).toList();

    List<ConversationParticipant> allParticipants = participantRepository.findAllByConversationIdIn(conversationIds);
    Map<Long, List<ConversationParticipant>> participantsByConv = allParticipants.stream()
        .collect(Collectors.groupingBy(ConversationParticipant::getConversationId));

    List<Long> allUserIds = allParticipants.stream()
        .map(ConversationParticipant::getUserId).distinct().toList();
    Map<Long, User> userMap = userRepository.findAllById(allUserIds).stream()
        .collect(Collectors.toMap(User::getUserId, Function.identity()));

    // Batch fetch groups for GROUP type conversations
    List<Long> groupConversationIds = resultRows.stream()
        .filter(r -> ConversationType.GROUP.name().equals(r.getConversationType()))
        .map(ConversationListProjection::getConversationId).toList();
    Map<Long, Group> groupByConvId = groupConversationIds.isEmpty()
        ? Map.of()
        : groupRepository.findByConversationIdIn(groupConversationIds).stream()
            .collect(Collectors.toMap(Group::getConversationId, Function.identity()));

    List<ConversationItemResponse> items = resultRows.stream()
        .map(row -> {
          List<ParticipantSummary> participants = buildParticipantSummary(
              row.getConversationType(), userId,
              participantsByConv.getOrDefault(row.getConversationId(), List.of()), userMap);

          String groupName = null;
          String groupAvatarUrl = null;
          if (ConversationType.GROUP.name().equals(row.getConversationType())) {
            Group group = groupByConvId.get(row.getConversationId());
            if (group != null) {
              groupName = group.getGroupName();
              if (group.getAvatarPath() != null) {
                groupAvatarUrl = minioProperties.buildUrl(group.getAvatarPath());
              }
            }
          }

          return ConversationMapper.toResponse(row, participants, groupName, groupAvatarUrl);
        })
        .toList();

    String nextCursor = null;
    if (hasMore) {
      ConversationListProjection last = resultRows.getLast();
      nextCursor = CursorUtil.encode(last.getLastMessageAt(), last.getConversationId());
    }

    return ConversationListResponse.builder()
        .conversations(items)
        .nextCursor(nextCursor)
        .hasMore(hasMore)
        .build();
  }

  @Override
  @Transactional
  public void pinConversation(Long userId, String conversationUuid) {
    ConversationParticipant cp = resolveParticipant(userId, conversationUuid);
    int pinnedCount = participantRepository.countPinnedByUserIdForUpdate(userId);
    if (pinnedCount >= 5) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "MAX_PINNED", "Maximum 5 pinned conversations reached");
    }
    cp.setIsPinned(true);
    cp.setPinnedAt(OffsetDateTime.now());
    participantRepository.save(cp);
  }

  @Override
  @Transactional
  public void unpinConversation(Long userId, String conversationUuid) {
    ConversationParticipant cp = resolveParticipant(userId, conversationUuid);
    cp.setIsPinned(false);
    cp.setPinnedAt(null);
    participantRepository.save(cp);
  }

  @Override
  @Transactional
  public void archiveConversation(Long userId, String conversationUuid) {
    ConversationParticipant cp = resolveParticipant(userId, conversationUuid);
    cp.setIsArchived(true);
    participantRepository.save(cp);
  }

  @Override
  @Transactional
  public void unarchiveConversation(Long userId, String conversationUuid) {
    ConversationParticipant cp = resolveParticipant(userId, conversationUuid);
    cp.setIsArchived(false);
    participantRepository.save(cp);
  }

  @Override
  @Transactional
  public void muteConversation(Long userId, String conversationUuid) {
    ConversationParticipant cp = resolveParticipant(userId, conversationUuid);
    cp.setIsMuted(true);
    participantRepository.save(cp);
  }

  @Override
  @Transactional
  public void unmuteConversation(Long userId, String conversationUuid) {
    ConversationParticipant cp = resolveParticipant(userId, conversationUuid);
    cp.setIsMuted(false);
    participantRepository.save(cp);
  }

  private ConversationParticipant resolveParticipant(Long userId, String conversationUuid) {
    Conversation conversation = conversationService.findConversationByUuid(conversationUuid);
    return participantRepository.findByConversationIdAndUserId(conversation.getConversationId(), userId)
        .orElseThrow(() -> new GeneralException(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Not a participant"));
  }

  private List<ConversationListProjection> fetchFirstPage(Long userId, boolean isArchived, String filter, int limit) {
    if ("UNREAD".equalsIgnoreCase(filter)) {
      return participantRepository.findConversationsFirstPageUnread(userId, isArchived, limit);
    }
    if (isTypeFilter(filter)) {
      return participantRepository.findConversationsFirstPageByType(userId, isArchived, filter.toUpperCase(), limit);
    }
    return participantRepository.findConversationsFirstPage(userId, isArchived, limit);
  }

  private List<ConversationListProjection> fetchWithCursor(Long userId, boolean isArchived, CursorPosition cursor, String filter, int limit) {
    if ("UNREAD".equalsIgnoreCase(filter)) {
      return participantRepository.findConversationsWithCursorUnread(userId, isArchived, cursor.timestamp(), cursor.id(), limit);
    }
    if (isTypeFilter(filter)) {
      return participantRepository.findConversationsWithCursorByType(userId, isArchived, filter.toUpperCase(), cursor.timestamp(), cursor.id(), limit);
    }
    return participantRepository.findConversationsWithCursor(userId, isArchived, cursor.timestamp(), cursor.id(), limit);
  }

  private boolean isTypeFilter(String filter) {
    return StringUtils.hasLength(filter)
        && !"ALL".equalsIgnoreCase(filter)
        && !"ARCHIVED".equalsIgnoreCase(filter)
        && !"UNREAD".equalsIgnoreCase(filter);
  }

  private List<ParticipantSummary> buildParticipantSummary(
      String conversationType, Long currentUserId,
      List<ConversationParticipant> participants, Map<Long, User> userMap) {

    if (ConversationType.PRIVATE.name().equals(conversationType)) {
      return participants.stream()
          .filter(p -> !p.getUserId().equals(currentUserId))
          .map(p -> userMap.get(p.getUserId()))
          .filter(Objects::nonNull)
          .map(ConversationMapper::toParticipantSummary)
          .toList();
    }
    return participants.stream()
        .map(p -> userMap.get(p.getUserId()))
        .filter(Objects::nonNull)
        .map(ConversationMapper::toParticipantSummary)
        .toList();
  }
}

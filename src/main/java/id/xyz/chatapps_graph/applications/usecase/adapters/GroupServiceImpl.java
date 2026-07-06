package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.applications.usecase.GroupService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.entity.Group;
import id.xyz.chatapps_graph.domain.entity.GroupMember;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.ConversationType;
import id.xyz.chatapps_graph.domain.enums.GroupMemberRole;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.domain.repository.GroupMemberRepository;
import id.xyz.chatapps_graph.domain.repository.GroupRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.CreateGroupRequest;
import id.xyz.chatapps_graph.framework.dto.CreateGroupResponse;
import id.xyz.chatapps_graph.framework.dto.GroupInfoResponse;
import id.xyz.chatapps_graph.framework.dto.GroupMemberResponse;
import id.xyz.chatapps_graph.framework.dto.UpdateGroupRequest;
import id.xyz.chatapps_graph.framework.dto.UpdateGroupSettingsRequest;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.properties.GroupProperties;
import id.xyz.chatapps_graph.infrastructure.config.properties.MinioProperties;
import id.xyz.chatapps_graph.infrastructure.utility.ImageProcessingService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

  private final GroupRepository groupRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final ConversationRepository conversationRepository;
  private final ConversationParticipantRepository participantRepository;
  private final UserRepository userRepository;
  private final FileStoragePort fileStoragePort;
  private final ImageProcessingService imageProcessingService;
  private final SystemMessageService systemMessageService;
  private final GroupProperties groupProperties;
  private final MinioProperties minioProperties;

  @Override
  @Transactional
  public CreateGroupResponse createGroup(Long creatorId, CreateGroupRequest request, MultipartFile avatar) {
    List<User> participants = resolveUsersByUuids(request.participantUuids());
    int totalMembers = participants.size() + 1;
    if (totalMembers > groupProperties.getMaxMembers()) {
      throw businessException(HttpStatus.BAD_REQUEST, "MAX_MEMBERS_EXCEEDED",
          "Group cannot exceed " + groupProperties.getMaxMembers() + " members");
    }

    User creator = requireUser(creatorId);
    String groupUuid = UUID.randomUUID().toString();

    // Create conversation first (no FK dependency on group yet)
    Conversation conversation = conversationRepository.save(Conversation.builder()
        .conversationType(ConversationType.GROUP.name())
        .build());

    // Create group with conversationId set immediately, avatar processed upfront
    String avatarPath = (avatar != null && !avatar.isEmpty()) ? processAvatar(groupUuid, avatar) : null;

    Group group = groupRepository.save(Group.builder()
        .groupUuid(groupUuid)
        .groupName(request.name())
        .groupDesc(request.description())
        .allowMemberAdd(true)
        .isActive(1)
        .conversationId(conversation.getConversationId())
        .avatarPath(avatarPath)
        .build());

    // Link conversation back to group
    conversation.setGroupId(group.getGroupId());
    conversationRepository.save(conversation);

    // Create all members
    OffsetDateTime now = OffsetDateTime.now();
    addParticipantAndMember(group, creator, conversation.getConversationId(), now, GroupMemberRole.OWNER);
    for (User participant : participants) {
      addParticipantAndMember(group, participant, conversation.getConversationId(), now, GroupMemberRole.MEMBER);
    }

    systemMessageService.create(conversation.getConversationId(), creatorId, "GROUP_CREATED", creator.getUserUuid(), null);

    return CreateGroupResponse.builder()
        .groupUuid(groupUuid)
        .conversationUuid(conversation.getConversationUuid())
        .build();
  }

  @Override
  @Transactional
  public void addMember(Long actorId, String groupUuid, String targetUserUuid) {
    Group group = requireActiveGroup(groupUuid);
    GroupMember actorMember = requireActiveMember(group, actorId);

    if (!group.getAllowMemberAdd() && isAdminOrOwner(actorMember)) {
      throw businessException(HttpStatus.FORBIDDEN, "NOT_PERMITTED",
          "Only admin/owner can add members when setting is restricted");
    }

    // Count with lock to prevent race condition on max-members
    int currentCount = groupMemberRepository.countActiveMembers(group.getGroupId(), 1);
    if (currentCount >= groupProperties.getMaxMembers()) {
      throw businessException(HttpStatus.BAD_REQUEST, "MAX_MEMBERS_EXCEEDED",
          "Group has reached maximum members limit");
    }

    User targetUser = findActiveUserByUuid(targetUserUuid);
    if (groupMemberRepository.findByGroupIdAndUserIdAndIsActive(group.getGroupId(), targetUser.getUserId(), 1).isPresent()) {
      throw businessException(HttpStatus.CONFLICT, "ALREADY_MEMBER", "User is already a member of this group");
    }

    addParticipantAndMember(group, targetUser, group.getConversationId(), OffsetDateTime.now(), GroupMemberRole.MEMBER);

    String actorUuid = actorMember.getUser().getUserUuid();
    systemMessageService.create(group.getConversationId(), actorId, "MEMBER_ADDED", actorUuid, targetUserUuid);
  }

  @Override
  @Transactional
  public void removeMember(Long actorId, String groupUuid, String targetUserUuid) {
    Group group = requireActiveGroup(groupUuid);
    GroupMember actorMember = requireActiveMember(group, actorId);
    requireAdminOrOwner(actorMember);

    User targetUser = findActiveUserByUuid(targetUserUuid);
    GroupMember targetMember = requireActiveMember(group, targetUser.getUserId());

    if (GroupMemberRole.OWNER.name().equals(targetMember.getMemberType())) {
      throw businessException(HttpStatus.FORBIDDEN, "CANNOT_REMOVE_OWNER", "Cannot remove the group owner");
    }

    deactivateMember(targetMember, group.getConversationId(), targetUser.getUserId());

    String actorUuid = actorMember.getUser().getUserUuid();
    systemMessageService.create(group.getConversationId(), actorId, "MEMBER_REMOVED", actorUuid, targetUserUuid);
  }

  @Override
  @Transactional
  public void leaveGroup(Long userId, String groupUuid) {
    Group group = requireActiveGroup(groupUuid);
    GroupMember member = requireActiveMember(group, userId);

    if (GroupMemberRole.OWNER.name().equals(member.getMemberType())) {
      throw businessException(HttpStatus.BAD_REQUEST, "OWNER_CANNOT_LEAVE",
          "Owner cannot leave the group. Dissolve the group instead.");
    }

    if (GroupMemberRole.ADMIN.name().equals(member.getMemberType())) {
      int adminCount = groupMemberRepository.countByRoleAndActive(
          group.getGroupId(), GroupMemberRole.ADMIN.name(), 1);
      if (adminCount <= 1) {
        throw businessException(HttpStatus.BAD_REQUEST, "LAST_ADMIN",
            "You are the last admin. Assign a new admin before leaving.");
      }
    }

    // Deactivate BEFORE system message — so system message unread count
    // does not include the leaving user
    deactivateMember(member, group.getConversationId(), userId);

    String userUuid = member.getUser().getUserUuid();
    systemMessageService.create(group.getConversationId(), userId, "MEMBER_LEFT", userUuid, null);
  }

  @Override
  @Transactional
  public void updateGroupInfo(Long actorId, String groupUuid, UpdateGroupRequest request) {
    Group group = requireActiveGroup(groupUuid);
    GroupMember actorMember = requireActiveMember(group, actorId);
    requireAdminOrOwner(actorMember);

    boolean nameChanged = StringUtils.hasLength(request.name()) && !request.name().equals(group.getGroupName());
    boolean descChanged = request.description() != null && !request.description().equals(group.getGroupDesc());

    if (!nameChanged && !descChanged) {
      return;
    }

    if (nameChanged) {
      group.setGroupName(request.name());
    }
    if (descChanged) {
      group.setGroupDesc(request.description());
    }
    groupRepository.save(group);

    // Record both events if both changed
    String actorUuid = actorMember.getUser().getUserUuid();
    if (nameChanged) {
      systemMessageService.create(group.getConversationId(), actorId, "GROUP_RENAMED", actorUuid, null);
    }
    if (descChanged) {
      systemMessageService.create(group.getConversationId(), actorId, "GROUP_DESCRIPTION_UPDATED", actorUuid, null);
    }
  }

  @Override
  @Transactional
  public String updateAvatar(Long actorId, String groupUuid, MultipartFile file) {
    Group group = requireActiveGroup(groupUuid);
    GroupMember actorMember = requireActiveMember(group, actorId);
    requireAdminOrOwner(actorMember);

    String avatarPath = processAvatar(groupUuid, file);
    group.setAvatarPath(avatarPath);
    groupRepository.save(group);

    String actorUuid = actorMember.getUser().getUserUuid();
    systemMessageService.create(group.getConversationId(), actorId, "AVATAR_CHANGED", actorUuid, null);
    return buildFullUrl(avatarPath);
  }

  @Override
  @Transactional
  public void updateSettings(Long actorId, String groupUuid, UpdateGroupSettingsRequest request) {
    Group group = requireActiveGroup(groupUuid);
    GroupMember actorMember = requireActiveMember(group, actorId);
    requireAdminOrOwner(actorMember);

    group.setAllowMemberAdd(request.allowMemberAdd());
    groupRepository.save(group);

    String actorUuid = actorMember.getUser().getUserUuid();
    systemMessageService.create(group.getConversationId(), actorId, "SETTINGS_CHANGED", actorUuid, null);
  }

  @Override
  @Transactional
  public void promoteAdmin(Long ownerId, String groupUuid, String targetUserUuid) {
    Group group = requireActiveGroup(groupUuid);
    GroupMember ownerMember = requireActiveMember(group, ownerId);
    requireOwner(ownerMember);

    User targetUser = findActiveUserByUuid(targetUserUuid);
    if (targetUser.getUserId().equals(ownerId)) {
      throw businessException(HttpStatus.BAD_REQUEST, "CANNOT_PROMOTE_SELF", "Cannot promote yourself");
    }

    GroupMember targetMember = requireActiveMember(group, targetUser.getUserId());
    targetMember.setMemberType(GroupMemberRole.ADMIN.name());
    groupMemberRepository.save(targetMember);

    String ownerUuid = ownerMember.getUser().getUserUuid();
    systemMessageService.create(group.getConversationId(), ownerId, "ADMIN_PROMOTED", ownerUuid, targetUserUuid);
  }

  @Override
  @Transactional
  public void demoteAdmin(Long ownerId, String groupUuid, String targetUserUuid) {
    Group group = requireActiveGroup(groupUuid);
    GroupMember ownerMember = requireActiveMember(group, ownerId);
    requireOwner(ownerMember);

    User targetUser = findActiveUserByUuid(targetUserUuid);
    if (targetUser.getUserId().equals(ownerId)) {
      throw businessException(HttpStatus.BAD_REQUEST, "CANNOT_DEMOTE_SELF", "Cannot demote yourself");
    }

    GroupMember targetMember = requireActiveMember(group, targetUser.getUserId());
    targetMember.setMemberType(GroupMemberRole.MEMBER.name());
    groupMemberRepository.save(targetMember);

    String ownerUuid = ownerMember.getUser().getUserUuid();
    systemMessageService.create(group.getConversationId(), ownerId, "ADMIN_DEMOTED", ownerUuid, targetUserUuid);
  }

  @Override
  @Transactional
  public void dissolveGroup(Long ownerId, String groupUuid) {
    Group group = requireActiveGroup(groupUuid);
    GroupMember ownerMember = requireActiveMember(group, ownerId);
    requireOwner(ownerMember);

    // System message MUST be created BEFORE setting isActive=0,
    // otherwise the write would be rejected by dissolved-group checks
    String ownerUuid = ownerMember.getUser().getUserUuid();
    systemMessageService.create(group.getConversationId(), ownerId, "GROUP_DISSOLVED", ownerUuid, null);

    group.setIsActive(0);
    groupRepository.save(group);
  }

  @Override
  @Transactional(readOnly = true)
  public GroupInfoResponse getGroupInfo(Long userId, String groupUuid) {
    Group group = groupRepository.findByGroupUuid(groupUuid)
        .orElseThrow(() -> businessException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group not found"));

    // Verify user is member (uses JOIN FETCH — user already loaded)
    requireActiveMember(group, userId);

    // Single query with JOIN FETCH — no N+1
    List<GroupMember> members = groupMemberRepository.findAllByGroupIdWithUser(group.getGroupId(), 1);

    List<GroupMemberResponse> memberResponses = members.stream()
        .map(m -> {
          User user = m.getUser();
          return GroupMemberResponse.builder()
              .userUuid(user.getUserUuid())
              .fullName(user.getUserFullName())
              .profilePhoto(user.getProfilePhoto())
              .role(m.getMemberType())
              .build();
        })
        .toList();

    return GroupInfoResponse.builder()
        .groupUuid(group.getGroupUuid())
        .name(group.getGroupName())
        .description(group.getGroupDesc())
        .avatarUrl(group.getAvatarPath() != null ? buildFullUrl(group.getAvatarPath()) : null)
        .allowMemberAdd(group.getAllowMemberAdd())
        .members(memberResponses)
        .build();
  }

  // --- Permission helpers ---

  private Group requireActiveGroup(String groupUuid) {
    Group group = groupRepository.findByGroupUuid(groupUuid)
        .orElseThrow(() -> businessException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group not found"));
    if (!Integer.valueOf(1).equals(group.getIsActive())) {
      throw businessException(HttpStatus.BAD_REQUEST, "GROUP_DISSOLVED", "This group has been dissolved");
    }
    return group;
  }

  private GroupMember requireActiveMember(Group group, Long userId) {
    return groupMemberRepository.findByGroupIdAndUserIdAndIsActive(group.getGroupId(), userId, 1)
        .orElseThrow(() -> businessException(HttpStatus.FORBIDDEN, "NOT_MEMBER", "You are not a member of this group"));
  }

  private void requireAdminOrOwner(GroupMember member) {
    if (isAdminOrOwner(member)) {
      throw businessException(HttpStatus.FORBIDDEN, "NOT_ADMIN", "Only admin or owner can perform this action");
    }
  }

  private void requireOwner(GroupMember member) {
    if (!GroupMemberRole.OWNER.name().equals(member.getMemberType())) {
      throw businessException(HttpStatus.FORBIDDEN, "NOT_OWNER", "Only the group owner can perform this action");
    }
  }

  private boolean isAdminOrOwner(GroupMember member) {
    String role = member.getMemberType();
    return !GroupMemberRole.OWNER.name().equals(role) && !GroupMemberRole.ADMIN.name().equals(role);
  }

  // --- Data helpers ---

  private void addParticipantAndMember(Group group, User user, Long conversationId, OffsetDateTime joinedAt, GroupMemberRole role) {
    participantRepository.save(ConversationParticipant.builder()
        .conversationId(conversationId)
        .userId(user.getUserId())
        .joinedAt(joinedAt)
        .build());

    groupMemberRepository.save(GroupMember.builder()
        .group(group)
        .user(user)
        .memberType(role.name())
        .isActive(1)
        .build());
  }

  private void deactivateMember(GroupMember member, Long conversationId, Long userId) {
    member.setIsActive(0);
    groupMemberRepository.save(member);
    participantRepository.findByConversationIdAndUserId(conversationId, userId)
        .ifPresent(participantRepository::delete);
  }

  private User requireUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> businessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
  }

  private User findActiveUserByUuid(String userUuid) {
    return userRepository.findUserByUserUuidAndUserStatus(userUuid, 1)
        .orElseThrow(() -> businessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found: " + userUuid));
  }

  private List<User> resolveUsersByUuids(List<String> userUuids) {
    List<User> users = userRepository.findByUserUuidInAndUserStatus(userUuids, 1);
    if (users.size() != userUuids.size()) {
      throw businessException(HttpStatus.BAD_REQUEST, "INVALID_PARTICIPANTS", "Some participant UUIDs are invalid or inactive");
    }
    return users;
  }

  // --- Upload helper ---

  private String processAvatar(String groupUuid, MultipartFile file) {
    try {
      byte[] compressed = imageProcessingService.compressImage(
          file.getInputStream(),
          groupProperties.getAvatar().getMaxDimension(),
          groupProperties.getAvatar().getQuality());
      String path = "groups/" + groupUuid + "/avatar.jpg";
      fileStoragePort.uploadFile(path, new ByteArrayInputStream(compressed), "image/jpeg", compressed.length);
      return path;
    } catch (IOException e) {
      log.error("Failed to process group avatar: {}", e.getMessage());
      throw businessException(HttpStatus.INTERNAL_SERVER_ERROR, "AVATAR_UPLOAD_FAILED", "Failed to upload avatar");
    }
  }

  private String buildFullUrl(String path) {
    return minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + path;
  }

  private GeneralException businessException(HttpStatus status, String key, String message) {
    return new GeneralException(status.value(), key, message);
  }
}

package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.entity.Group;
import id.xyz.chatapps_graph.domain.entity.GroupMember;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.GroupMemberRole;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.domain.repository.GroupMemberRepository;
import id.xyz.chatapps_graph.domain.repository.GroupRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.CreateGroupRequest;
import id.xyz.chatapps_graph.framework.dto.CreateGroupResponse;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.properties.GroupProperties;
import id.xyz.chatapps_graph.infrastructure.config.properties.MinioProperties;
import id.xyz.chatapps_graph.infrastructure.utility.ImageProcessingService;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupMemberRepository groupMemberRepository;
  @Mock private ConversationRepository conversationRepository;
  @Mock private ConversationParticipantRepository participantRepository;
  @Mock private UserRepository userRepository;
  @Mock private FileStoragePort fileStoragePort;
  @Mock private ImageProcessingService imageProcessingService;
  @Mock private SystemMessageService systemMessageService;
  @Mock private MultipartFile avatarFile;

  private GroupServiceImpl groupService;
  private GroupProperties groupProperties;
  private MinioProperties minioProperties;

  private static final Long OWNER_ID = 1L;
  private static final Long ADMIN_ID = 2L;
  private static final Long MEMBER_ID = 3L;
  private static final Long TARGET_ID = 4L;
  private static final String OWNER_UUID = "owner-uuid";
  private static final String ADMIN_UUID = "admin-uuid";
  private static final String MEMBER_UUID = "member-uuid";
  private static final String TARGET_UUID = "target-uuid";
  private static final String GROUP_UUID = "group-uuid-123";
  private static final Long GROUP_ID = 10L;
  private static final Long CONVERSATION_ID = 20L;

  @BeforeEach
  void setUp() {
    groupProperties = new GroupProperties();
    groupProperties.setMaxMembers(200);
    GroupProperties.AvatarProperties avatarProps = new GroupProperties.AvatarProperties();
    avatarProps.setMaxDimension(500);
    avatarProps.setQuality(85);
    groupProperties.setAvatar(avatarProps);

    minioProperties = new MinioProperties();
    minioProperties.setEndpoint("http://localhost:9000");
    minioProperties.setBucket("chat-bucket");

    groupService = new GroupServiceImpl(
        groupRepository, groupMemberRepository, conversationRepository,
        participantRepository, userRepository, fileStoragePort,
        imageProcessingService, systemMessageService, groupProperties, minioProperties);
  }

  private User buildUser(Long id, String uuid) {
    return User.builder().userId(id).userUuid(uuid).userFullName("User " + id).build();
  }

  private Group buildGroup() {
    return Group.builder()
        .groupId(GROUP_ID).groupUuid(GROUP_UUID).groupName("Test Group")
        .allowMemberAdd(true).isActive(1).conversationId(CONVERSATION_ID).build();
  }

  private GroupMember buildMember(Long userId, String uuid, GroupMemberRole role) {
    User user = buildUser(userId, uuid);
    return GroupMember.builder().group(buildGroup()).user(user).memberType(role.name()).isActive(1).build();
  }

  // === CREATE GROUP ===

  @Test
  @DisplayName("createGroup: valid request — creates group and conversation")
  void createGroup_ValidRequest_CreatesGroupAndConversation() {
    User creator = buildUser(OWNER_ID, OWNER_UUID);
    User participant1 = buildUser(2L, "p1-uuid");
    User participant2 = buildUser(3L, "p2-uuid");

    when(userRepository.findByUserUuidInAndUserStatus(List.of("p1-uuid", "p2-uuid"), 1))
        .thenReturn(List.of(participant1, participant2));
    when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(creator));
    when(conversationRepository.save(any(Conversation.class)))
        .thenAnswer(inv -> { Conversation c = inv.getArgument(0); c.setConversationId(CONVERSATION_ID); c.setConversationUuid("conv-uuid"); return c; });
    when(groupRepository.save(any(Group.class)))
        .thenAnswer(inv -> { Group g = inv.getArgument(0); g.setGroupId(GROUP_ID); return g; });

    CreateGroupRequest request = new CreateGroupRequest("My Group", "desc", List.of("p1-uuid", "p2-uuid"));
    CreateGroupResponse response = groupService.createGroup(OWNER_ID, request, null);

    assertNotNull(response.groupUuid());
    assertEquals("conv-uuid", response.conversationUuid());
    verify(participantRepository, org.mockito.Mockito.times(3)).save(any(ConversationParticipant.class));
    verify(groupMemberRepository, org.mockito.Mockito.times(3)).save(any(GroupMember.class));
    verify(systemMessageService).create(eq(CONVERSATION_ID), eq(OWNER_ID), eq("GROUP_CREATED"), eq(OWNER_UUID), eq(null));
  }

  @Test
  @DisplayName("createGroup: with avatar — compresses and uploads")
  void createGroup_WithAvatar_CompressesAndUploads() throws Exception {
    User creator = buildUser(OWNER_ID, OWNER_UUID);
    User participant1 = buildUser(2L, "p1-uuid");
    User participant2 = buildUser(3L, "p2-uuid");

    when(userRepository.findByUserUuidInAndUserStatus(List.of("p1-uuid", "p2-uuid"), 1))
        .thenReturn(List.of(participant1, participant2));
    when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(creator));
    when(conversationRepository.save(any(Conversation.class)))
        .thenAnswer(inv -> { Conversation c = inv.getArgument(0); c.setConversationId(CONVERSATION_ID); c.setConversationUuid("conv-uuid"); return c; });
    when(groupRepository.save(any(Group.class)))
        .thenAnswer(inv -> { Group g = inv.getArgument(0); g.setGroupId(GROUP_ID); return g; });
    when(avatarFile.isEmpty()).thenReturn(false);
    when(avatarFile.getInputStream()).thenReturn(InputStream.nullInputStream());
    when(imageProcessingService.compressImage(any(InputStream.class), eq(500), eq(85f)))
        .thenReturn(new byte[]{1, 2, 3});

    CreateGroupRequest request = new CreateGroupRequest("My Group", null, List.of("p1-uuid", "p2-uuid"));
    groupService.createGroup(OWNER_ID, request, avatarFile);

    verify(fileStoragePort).uploadFile(anyString(), any(InputStream.class), eq("image/jpeg"), eq(3L));
  }

  @Test
  @DisplayName("createGroup: invalid participant UUIDs — throws 400")
  void createGroup_InvalidParticipants_Throws400() {
    when(userRepository.findByUserUuidInAndUserStatus(List.of("invalid-uuid", "p2-uuid"), 1))
        .thenReturn(List.of(buildUser(2L, "p2-uuid"))); // only 1 found out of 2

    CreateGroupRequest request = new CreateGroupRequest("G", null, List.of("invalid-uuid", "p2-uuid"));
    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.createGroup(OWNER_ID, request, null));

    assertEquals(400, ex.getHttpCode());
    assertEquals("INVALID_PARTICIPANTS", ex.getKey());
  }

  @Test
  @DisplayName("createGroup: exceeds max members — throws 400")
  void createGroup_ExceedsMaxMembers_Throws400() {
    groupProperties.setMaxMembers(3);

    List<String> uuids = List.of("p1", "p2", "p3");
    List<User> users = List.of(buildUser(2L, "p1"), buildUser(3L, "p2"), buildUser(4L, "p3"));
    when(userRepository.findByUserUuidInAndUserStatus(uuids, 1)).thenReturn(users);

    CreateGroupRequest request = new CreateGroupRequest("G", null, uuids);
    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.createGroup(OWNER_ID, request, null));

    assertEquals(400, ex.getHttpCode());
    assertEquals("MAX_MEMBERS_EXCEEDED", ex.getKey());
  }

  @Test
  @DisplayName("createGroup: system message created with GROUP_CREATED event")
  void createGroup_SystemMessageCreated() {
    User creator = buildUser(OWNER_ID, OWNER_UUID);
    when(userRepository.findByUserUuidInAndUserStatus(List.of("p1-uuid", "p2-uuid"), 1))
        .thenReturn(List.of(buildUser(2L, "p1-uuid"), buildUser(3L, "p2-uuid")));
    when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(creator));
    when(conversationRepository.save(any(Conversation.class)))
        .thenAnswer(inv -> { Conversation c = inv.getArgument(0); c.setConversationId(CONVERSATION_ID); c.setConversationUuid("conv-uuid"); return c; });
    when(groupRepository.save(any(Group.class)))
        .thenAnswer(inv -> { Group g = inv.getArgument(0); g.setGroupId(GROUP_ID); return g; });

    groupService.createGroup(OWNER_ID, new CreateGroupRequest("G", null, List.of("p1-uuid", "p2-uuid")), null);

    verify(systemMessageService).create(CONVERSATION_ID, OWNER_ID, "GROUP_CREATED", OWNER_UUID, null);
  }

  // === ADD MEMBER ===

  @Test
  @DisplayName("addMember: allowMemberAdd=true — any member can add")
  void addMember_AllowMemberAddTrue_MemberCanAdd() {
    Group group = buildGroup();
    group.setAllowMemberAdd(true);
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, MEMBER_ID, 1))
        .thenReturn(Optional.of(buildMember(MEMBER_ID, MEMBER_UUID, GroupMemberRole.MEMBER)));
    when(groupMemberRepository.countActiveMembers(GROUP_ID, 1)).thenReturn(5);
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, 1))
        .thenReturn(Optional.of(buildUser(TARGET_ID, TARGET_UUID)));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, TARGET_ID, 1))
        .thenReturn(Optional.empty());

    groupService.addMember(MEMBER_ID, GROUP_UUID, TARGET_UUID);

    ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
    verify(groupMemberRepository).save(memberCaptor.capture());
    GroupMember savedMember = memberCaptor.getValue();
    assertEquals(GroupMemberRole.MEMBER.name(), savedMember.getMemberType());
    assertEquals(TARGET_ID, savedMember.getUser().getUserId());

    verify(participantRepository).save(any(ConversationParticipant.class));
    verify(systemMessageService).create(CONVERSATION_ID, MEMBER_ID, "MEMBER_ADDED", MEMBER_UUID, TARGET_UUID);
  }

  @Test
  @DisplayName("addMember: allowMemberAdd=false, member tries — throws 403")
  void addMember_AllowMemberAddFalse_MemberCantAdd_Throws403() {
    Group group = buildGroup();
    group.setAllowMemberAdd(false);
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, MEMBER_ID, 1))
        .thenReturn(Optional.of(buildMember(MEMBER_ID, MEMBER_UUID, GroupMemberRole.MEMBER)));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.addMember(MEMBER_ID, GROUP_UUID, TARGET_UUID));

    assertEquals(403, ex.getHttpCode());
    assertEquals("NOT_PERMITTED", ex.getKey());
  }

  @Test
  @DisplayName("addMember: allowMemberAdd=false, admin can add")
  void addMember_AllowMemberAddFalse_AdminCanAdd() {
    Group group = buildGroup();
    group.setAllowMemberAdd(false);
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, ADMIN_ID, 1))
        .thenReturn(Optional.of(buildMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN)));
    when(groupMemberRepository.countActiveMembers(GROUP_ID, 1)).thenReturn(5);
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, 1))
        .thenReturn(Optional.of(buildUser(TARGET_ID, TARGET_UUID)));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, TARGET_ID, 1))
        .thenReturn(Optional.empty());

    groupService.addMember(ADMIN_ID, GROUP_UUID, TARGET_UUID);

    verify(systemMessageService).create(CONVERSATION_ID, ADMIN_ID, "MEMBER_ADDED", ADMIN_UUID, TARGET_UUID);
  }

  @Test
  @DisplayName("addMember: already a member — throws 409")
  void addMember_AlreadyMember_Throws409() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, OWNER_ID, 1))
        .thenReturn(Optional.of(buildMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER)));
    when(groupMemberRepository.countActiveMembers(GROUP_ID, 1)).thenReturn(5);
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, 1))
        .thenReturn(Optional.of(buildUser(TARGET_ID, TARGET_UUID)));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, TARGET_ID, 1))
        .thenReturn(Optional.of(buildMember(TARGET_ID, TARGET_UUID, GroupMemberRole.MEMBER)));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.addMember(OWNER_ID, GROUP_UUID, TARGET_UUID));

    assertEquals(409, ex.getHttpCode());
    assertEquals("ALREADY_MEMBER", ex.getKey());
  }

  @Test
  @DisplayName("addMember: max members reached — throws 400")
  void addMember_MaxMembersReached_Throws400() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, OWNER_ID, 1))
        .thenReturn(Optional.of(buildMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER)));
    when(groupMemberRepository.countActiveMembers(GROUP_ID, 1)).thenReturn(200);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.addMember(OWNER_ID, GROUP_UUID, TARGET_UUID));

    assertEquals(400, ex.getHttpCode());
    assertEquals("MAX_MEMBERS_EXCEEDED", ex.getKey());
  }

  @Test
  @DisplayName("addMember: dissolved group — throws 400")
  void addMember_DissolvedGroup_Throws400() {
    Group group = buildGroup();
    group.setIsActive(0);
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.addMember(OWNER_ID, GROUP_UUID, TARGET_UUID));

    assertEquals(400, ex.getHttpCode());
    assertEquals("GROUP_DISSOLVED", ex.getKey());
  }

  // === REMOVE MEMBER ===

  @Test
  @DisplayName("removeMember: admin removes member — success")
  void removeMember_AdminRemovesMember_Success() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, ADMIN_ID, 1))
        .thenReturn(Optional.of(buildMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN)));
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, 1))
        .thenReturn(Optional.of(buildUser(TARGET_ID, TARGET_UUID)));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, TARGET_ID, 1))
        .thenReturn(Optional.of(buildMember(TARGET_ID, TARGET_UUID, GroupMemberRole.MEMBER)));
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, TARGET_ID))
        .thenReturn(Optional.of(ConversationParticipant.builder().build()));

    groupService.removeMember(ADMIN_ID, GROUP_UUID, TARGET_UUID);

    verify(systemMessageService).create(CONVERSATION_ID, ADMIN_ID, "MEMBER_REMOVED", ADMIN_UUID, TARGET_UUID);
  }

  @Test
  @DisplayName("removeMember: owner removes member — success")
  void removeMember_OwnerRemovesMember_Success() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, OWNER_ID, 1))
        .thenReturn(Optional.of(buildMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER)));
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, 1))
        .thenReturn(Optional.of(buildUser(TARGET_ID, TARGET_UUID)));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, TARGET_ID, 1))
        .thenReturn(Optional.of(buildMember(TARGET_ID, TARGET_UUID, GroupMemberRole.MEMBER)));
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, TARGET_ID))
        .thenReturn(Optional.of(ConversationParticipant.builder().build()));

    groupService.removeMember(OWNER_ID, GROUP_UUID, TARGET_UUID);

    verify(systemMessageService).create(CONVERSATION_ID, OWNER_ID, "MEMBER_REMOVED", OWNER_UUID, TARGET_UUID);
  }

  @Test
  @DisplayName("removeMember: member tries to remove — throws 403")
  void removeMember_MemberTriesToRemove_Throws403() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, MEMBER_ID, 1))
        .thenReturn(Optional.of(buildMember(MEMBER_ID, MEMBER_UUID, GroupMemberRole.MEMBER)));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.removeMember(MEMBER_ID, GROUP_UUID, TARGET_UUID));

    assertEquals(403, ex.getHttpCode());
    assertEquals("NOT_ADMIN", ex.getKey());
  }

  @Test
  @DisplayName("removeMember: cannot remove owner — throws 403")
  void removeMember_CannotRemoveOwner_Throws403() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, ADMIN_ID, 1))
        .thenReturn(Optional.of(buildMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN)));
    when(userRepository.findUserByUserUuidAndUserStatus(OWNER_UUID, 1))
        .thenReturn(Optional.of(buildUser(OWNER_ID, OWNER_UUID)));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, OWNER_ID, 1))
        .thenReturn(Optional.of(buildMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER)));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.removeMember(ADMIN_ID, GROUP_UUID, OWNER_UUID));

    assertEquals(403, ex.getHttpCode());
    assertEquals("CANNOT_REMOVE_OWNER", ex.getKey());
  }

  // === LEAVE GROUP ===

  @Test
  @DisplayName("leaveGroup: regular member — success")
  void leaveGroup_RegularMember_Success() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, MEMBER_ID, 1))
        .thenReturn(Optional.of(buildMember(MEMBER_ID, MEMBER_UUID, GroupMemberRole.MEMBER)));
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, MEMBER_ID))
        .thenReturn(Optional.of(ConversationParticipant.builder().build()));

    groupService.leaveGroup(MEMBER_ID, GROUP_UUID);

    verify(systemMessageService).create(CONVERSATION_ID, MEMBER_ID, "MEMBER_LEFT", MEMBER_UUID, null);
  }

  @Test
  @DisplayName("leaveGroup: owner cannot leave — throws 400")
  void leaveGroup_OwnerCannotLeave_Throws400() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, OWNER_ID, 1))
        .thenReturn(Optional.of(buildMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER)));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.leaveGroup(OWNER_ID, GROUP_UUID));

    assertEquals(400, ex.getHttpCode());
    assertEquals("OWNER_CANNOT_LEAVE", ex.getKey());
  }

  @Test
  @DisplayName("leaveGroup: last admin must assign replacement — throws 400")
  void leaveGroup_LastAdminMustAssignReplacement_Throws400() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, ADMIN_ID, 1))
        .thenReturn(Optional.of(buildMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN)));
    when(groupMemberRepository.countByRoleAndActive(GROUP_ID, GroupMemberRole.ADMIN.name(), 1)).thenReturn(1);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.leaveGroup(ADMIN_ID, GROUP_UUID));

    assertEquals(400, ex.getHttpCode());
    assertEquals("LAST_ADMIN", ex.getKey());
  }

  @Test
  @DisplayName("leaveGroup: admin with other admins — success")
  void leaveGroup_AdminWithOtherAdmins_Success() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, ADMIN_ID, 1))
        .thenReturn(Optional.of(buildMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN)));
    when(groupMemberRepository.countByRoleAndActive(GROUP_ID, GroupMemberRole.ADMIN.name(), 1)).thenReturn(2);
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, ADMIN_ID))
        .thenReturn(Optional.of(ConversationParticipant.builder().build()));

    groupService.leaveGroup(ADMIN_ID, GROUP_UUID);

    verify(systemMessageService).create(CONVERSATION_ID, ADMIN_ID, "MEMBER_LEFT", ADMIN_UUID, null);
  }
}

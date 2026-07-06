package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.domain.entity.Group;
import id.xyz.chatapps_graph.domain.entity.GroupMember;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.GroupMemberRole;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.domain.repository.GroupMemberRepository;
import id.xyz.chatapps_graph.domain.repository.GroupRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.GroupInfoResponse;
import id.xyz.chatapps_graph.framework.dto.UpdateGroupRequest;
import id.xyz.chatapps_graph.framework.dto.UpdateGroupSettingsRequest;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplUpdateAndAdminTest {

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
    GroupProperties groupProperties = new GroupProperties();
    groupProperties.setMaxMembers(200);
    GroupProperties.AvatarProperties avatarProps = new GroupProperties.AvatarProperties();
    avatarProps.setMaxDimension(500);
    avatarProps.setQuality(85);
    groupProperties.setAvatar(avatarProps);

    MinioProperties minioProperties = new MinioProperties();
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
        .groupId(GROUP_ID).groupUuid(GROUP_UUID).groupName("Test Group").groupDesc("Desc")
        .allowMemberAdd(true).isActive(1).conversationId(CONVERSATION_ID).build();
  }

  private GroupMember buildMember(Long userId, String uuid, GroupMemberRole role) {
    User user = buildUser(userId, uuid);
    return GroupMember.builder().group(buildGroup()).user(user).memberType(role.name()).isActive(1).build();
  }

  private void mockGroupAndMember(Long actorId, String actorUuid, GroupMemberRole role) {
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(buildGroup()));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, actorId, 1))
        .thenReturn(Optional.of(buildMember(actorId, actorUuid, role)));
  }

  // === UPDATE GROUP INFO ===

  @Test
  @DisplayName("updateGroupInfo: admin updates name — success")
  void updateGroupInfo_AdminUpdatesName_Success() {
    mockGroupAndMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN);

    groupService.updateGroupInfo(ADMIN_ID, GROUP_UUID, new UpdateGroupRequest("New Name", null));

    verify(groupRepository).save(any(Group.class));
    verify(systemMessageService).create(CONVERSATION_ID, ADMIN_ID, "GROUP_RENAMED", ADMIN_UUID, null);
  }

  @Test
  @DisplayName("updateGroupInfo: member tries to update — throws 403")
  void updateGroupInfo_MemberTriesToUpdate_Throws403() {
    mockGroupAndMember(MEMBER_ID, MEMBER_UUID, GroupMemberRole.MEMBER);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.updateGroupInfo(MEMBER_ID, GROUP_UUID, new UpdateGroupRequest("X", null)));

    assertEquals(403, ex.getHttpCode());
    assertEquals("NOT_ADMIN", ex.getKey());
  }

  @Test
  @DisplayName("updateGroupInfo: both name and desc changed — records both events")
  void updateGroupInfo_BothChanged_RecordsBothEvents() {
    mockGroupAndMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN);

    groupService.updateGroupInfo(ADMIN_ID, GROUP_UUID, new UpdateGroupRequest("New Name", "New Desc"));

    verify(systemMessageService).create(CONVERSATION_ID, ADMIN_ID, "GROUP_RENAMED", ADMIN_UUID, null);
    verify(systemMessageService).create(CONVERSATION_ID, ADMIN_ID, "GROUP_DESCRIPTION_UPDATED", ADMIN_UUID, null);
  }

  // === UPDATE AVATAR ===

  @Test
  @DisplayName("updateAvatar: admin uploads — compresses and returns URL")
  void updateAvatar_CompressesToSquare_Success() throws Exception {
    mockGroupAndMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN);
    when(avatarFile.getInputStream()).thenReturn(InputStream.nullInputStream());
    when(imageProcessingService.compressImage(any(InputStream.class), eq(500), eq(85f)))
        .thenReturn(new byte[]{1, 2});

    String url = groupService.updateAvatar(ADMIN_ID, GROUP_UUID, avatarFile);

    assertTrue(url.contains("groups/" + GROUP_UUID + "/avatar.jpg"));
    verify(systemMessageService).create(CONVERSATION_ID, ADMIN_ID, "AVATAR_CHANGED", ADMIN_UUID, null);
  }

  // === UPDATE SETTINGS ===

  @Test
  @DisplayName("updateSettings: owner changes allowMemberAdd — success")
  void updateSettings_OwnerChangesAllowMemberAdd_Success() {
    mockGroupAndMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER);

    groupService.updateSettings(OWNER_ID, GROUP_UUID, new UpdateGroupSettingsRequest(false));

    verify(groupRepository).save(any(Group.class));
    verify(systemMessageService).create(CONVERSATION_ID, OWNER_ID, "SETTINGS_CHANGED", OWNER_UUID, null);
  }

  @Test
  @DisplayName("updateSettings: member tries to change — throws 403")
  void updateSettings_MemberTriesToChange_Throws403() {
    mockGroupAndMember(MEMBER_ID, MEMBER_UUID, GroupMemberRole.MEMBER);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.updateSettings(MEMBER_ID, GROUP_UUID, new UpdateGroupSettingsRequest(false)));

    assertEquals(403, ex.getHttpCode());
  }

  // === PROMOTE/DEMOTE ADMIN ===

  @Test
  @DisplayName("promoteAdmin: owner promotes — success")
  void promoteAdmin_OwnerPromotes_Success() {
    mockGroupAndMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER);
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, 1))
        .thenReturn(Optional.of(buildUser(TARGET_ID, TARGET_UUID)));
    GroupMember targetMember = buildMember(TARGET_ID, TARGET_UUID, GroupMemberRole.MEMBER);
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, TARGET_ID, 1))
        .thenReturn(Optional.of(targetMember));

    groupService.promoteAdmin(OWNER_ID, GROUP_UUID, TARGET_UUID);

    assertEquals(GroupMemberRole.ADMIN.name(), targetMember.getMemberType());
    verify(systemMessageService).create(CONVERSATION_ID, OWNER_ID, "ADMIN_PROMOTED", OWNER_UUID, TARGET_UUID);
  }

  @Test
  @DisplayName("promoteAdmin: non-owner tries — throws 403")
  void promoteAdmin_NonOwnerTriesToPromote_Throws403() {
    mockGroupAndMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.promoteAdmin(ADMIN_ID, GROUP_UUID, TARGET_UUID));

    assertEquals(403, ex.getHttpCode());
    assertEquals("NOT_OWNER", ex.getKey());
  }

  @Test
  @DisplayName("promoteAdmin: cannot promote self — throws 400")
  void promoteAdmin_CannotPromoteSelf_Throws400() {
    mockGroupAndMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER);
    when(userRepository.findUserByUserUuidAndUserStatus(OWNER_UUID, 1))
        .thenReturn(Optional.of(buildUser(OWNER_ID, OWNER_UUID)));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.promoteAdmin(OWNER_ID, GROUP_UUID, OWNER_UUID));

    assertEquals(400, ex.getHttpCode());
    assertEquals("CANNOT_PROMOTE_SELF", ex.getKey());
  }

  @Test
  @DisplayName("demoteAdmin: owner demotes — success")
  void demoteAdmin_OwnerDemotes_Success() {
    mockGroupAndMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER);
    when(userRepository.findUserByUserUuidAndUserStatus(ADMIN_UUID, 1))
        .thenReturn(Optional.of(buildUser(ADMIN_ID, ADMIN_UUID)));
    GroupMember adminMember = buildMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN);
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, ADMIN_ID, 1))
        .thenReturn(Optional.of(adminMember));

    groupService.demoteAdmin(OWNER_ID, GROUP_UUID, ADMIN_UUID);

    assertEquals(GroupMemberRole.MEMBER.name(), adminMember.getMemberType());
    verify(systemMessageService).create(CONVERSATION_ID, OWNER_ID, "ADMIN_DEMOTED", OWNER_UUID, ADMIN_UUID);
  }

  @Test
  @DisplayName("demoteAdmin: non-owner tries — throws 403")
  void demoteAdmin_NonOwnerTriesToDemote_Throws403() {
    mockGroupAndMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.demoteAdmin(ADMIN_ID, GROUP_UUID, MEMBER_UUID));

    assertEquals(403, ex.getHttpCode());
    assertEquals("NOT_OWNER", ex.getKey());
  }

  // === DISSOLVE GROUP ===

  @Test
  @DisplayName("dissolveGroup: owner dissolves — soft deletes")
  void dissolveGroup_OwnerDissolves_SoftDeletes() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, OWNER_ID, 1))
        .thenReturn(Optional.of(buildMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER)));

    groupService.dissolveGroup(OWNER_ID, GROUP_UUID);

    assertEquals(0, group.getIsActive());
    verify(systemMessageService).create(CONVERSATION_ID, OWNER_ID, "GROUP_DISSOLVED", OWNER_UUID, null);
    verify(groupRepository).save(group);
  }

  @Test
  @DisplayName("dissolveGroup: non-owner tries — throws 403")
  void dissolveGroup_NonOwnerTriesToDissolve_Throws403() {
    mockGroupAndMember(ADMIN_ID, ADMIN_UUID, GroupMemberRole.ADMIN);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.dissolveGroup(ADMIN_ID, GROUP_UUID));

    assertEquals(403, ex.getHttpCode());
    assertEquals("NOT_OWNER", ex.getKey());
  }

  @Test
  @DisplayName("dissolveGroup: writes rejected after dissolve")
  void dissolveGroup_WritesRejectedAfterDissolve() {
    Group group = buildGroup();
    group.setIsActive(0);
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.addMember(OWNER_ID, GROUP_UUID, TARGET_UUID));

    assertEquals(400, ex.getHttpCode());
    assertEquals("GROUP_DISSOLVED", ex.getKey());
  }

  // === GET GROUP INFO ===

  @Test
  @DisplayName("getGroupInfo: returns members with roles")
  void getGroupInfo_ReturnsMembersWithRoles() {
    Group group = buildGroup();
    group.setAvatarPath("groups/" + GROUP_UUID + "/avatar.jpg");
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, MEMBER_ID, 1))
        .thenReturn(Optional.of(buildMember(MEMBER_ID, MEMBER_UUID, GroupMemberRole.MEMBER)));

    GroupMember ownerMember = buildMember(OWNER_ID, OWNER_UUID, GroupMemberRole.OWNER);
    GroupMember memberMember = buildMember(MEMBER_ID, MEMBER_UUID, GroupMemberRole.MEMBER);
    when(groupMemberRepository.findAllByGroupIdWithUser(GROUP_ID, 1))
        .thenReturn(List.of(ownerMember, memberMember));

    GroupInfoResponse response = groupService.getGroupInfo(MEMBER_ID, GROUP_UUID);

    assertEquals(GROUP_UUID, response.groupUuid());
    assertEquals("Test Group", response.name());
    assertNotNull(response.avatarUrl());
    assertTrue(response.avatarUrl().contains("avatar.jpg"));
    assertEquals(2, response.members().size());
    assertEquals("OWNER", response.members().get(0).role());
  }

  @Test
  @DisplayName("getGroupInfo: non-member — throws 403")
  void getGroupInfo_NonMember_Throws403() {
    Group group = buildGroup();
    when(groupRepository.findByGroupUuid(GROUP_UUID)).thenReturn(Optional.of(group));
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(GROUP_ID, TARGET_ID, 1))
        .thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class,
        () -> groupService.getGroupInfo(TARGET_ID, GROUP_UUID));

    assertEquals(403, ex.getHttpCode());
    assertEquals("NOT_MEMBER", ex.getKey());
  }
}

package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.PresenceStatus;
import id.xyz.chatapps_graph.domain.enums.PresenceVisibility;
import id.xyz.chatapps_graph.domain.repository.ContactRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.PresenceResponse;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private ContactRepository contactRepository;

  @InjectMocks private ProfileServiceImpl profileService;

  private static final Long REQUESTER_ID = 1L;
  private static final Long TARGET_USER_ID = 2L;
  private static final String TARGET_UUID = "target-uuid";

  private User buildTarget(String visibility, OffsetDateTime lastSeenAt) {
    User u = new User();
    u.setUserId(TARGET_USER_ID);
    u.setUserUuid(TARGET_UUID);
    u.setPresenceVisibility(visibility);
    u.setLastSeenAt(lastSeenAt);
    u.setUserStatus(StatusConstants.ACTIVE);
    return u;
  }

  @Test
  @DisplayName("getPresence: user online (last seen < 2 min) — returns ONLINE, no lastSeenAt")
  void getPresence_UserOnline_ReturnsOnlineStatus() {
    User target = buildTarget(PresenceVisibility.EVERYONE.name(), OffsetDateTime.now().minusSeconds(30));
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, StatusConstants.ACTIVE))
        .thenReturn(Optional.of(target));

    PresenceResponse result = profileService.getPresence(REQUESTER_ID, TARGET_UUID);

    assertEquals(PresenceStatus.ONLINE.name(), result.status());
    assertNull(result.lastSeenAt());
  }

  @Test
  @DisplayName("getPresence: user offline (last seen > 2 min) — returns OFFLINE with lastSeenAt")
  void getPresence_UserOffline_ReturnsOfflineWithLastSeen() {
    OffsetDateTime lastSeen = OffsetDateTime.now().minusMinutes(10);
    User target = buildTarget(PresenceVisibility.EVERYONE.name(), lastSeen);
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, StatusConstants.ACTIVE))
        .thenReturn(Optional.of(target));

    PresenceResponse result = profileService.getPresence(REQUESTER_ID, TARGET_UUID);

    assertEquals(PresenceStatus.OFFLINE.name(), result.status());
    assertEquals(lastSeen, result.lastSeenAt());
  }

  @Test
  @DisplayName("getPresence: privacy NOBODY — returns UNKNOWN")
  void getPresence_PrivacyNobody_ReturnsUnknown() {
    User target = buildTarget(PresenceVisibility.NOBODY.name(), OffsetDateTime.now());
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, StatusConstants.ACTIVE))
        .thenReturn(Optional.of(target));

    PresenceResponse result = profileService.getPresence(REQUESTER_ID, TARGET_UUID);

    assertEquals(PresenceStatus.UNKNOWN.name(), result.status());
    assertNull(result.lastSeenAt());
  }

  @Test
  @DisplayName("getPresence: privacy CONTACTS_ONLY, requester IS contact — returns status")
  void getPresence_PrivacyContactsOnly_IsContact_ReturnsStatus() {
    User target = buildTarget(PresenceVisibility.CONTACTS_ONLY.name(), OffsetDateTime.now().minusSeconds(30));
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, StatusConstants.ACTIVE))
        .thenReturn(Optional.of(target));
    when(contactRepository.existsByOwnerUserIdAndContactUserUserId(TARGET_USER_ID, REQUESTER_ID))
        .thenReturn(true);

    PresenceResponse result = profileService.getPresence(REQUESTER_ID, TARGET_UUID);

    assertEquals(PresenceStatus.ONLINE.name(), result.status());
  }

  @Test
  @DisplayName("getPresence: privacy CONTACTS_ONLY, requester NOT contact — returns UNKNOWN")
  void getPresence_PrivacyContactsOnly_NotContact_ReturnsUnknown() {
    User target = buildTarget(PresenceVisibility.CONTACTS_ONLY.name(), OffsetDateTime.now());
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, StatusConstants.ACTIVE))
        .thenReturn(Optional.of(target));
    when(contactRepository.existsByOwnerUserIdAndContactUserUserId(TARGET_USER_ID, REQUESTER_ID))
        .thenReturn(false);

    PresenceResponse result = profileService.getPresence(REQUESTER_ID, TARGET_UUID);

    assertEquals(PresenceStatus.UNKNOWN.name(), result.status());
  }

  @Test
  @DisplayName("getPresence: user not found — throws 404")
  void getPresence_UserNotFound_Throws404() {
    when(userRepository.findUserByUserUuidAndUserStatus(TARGET_UUID, StatusConstants.ACTIVE))
        .thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class,
        () -> profileService.getPresence(REQUESTER_ID, TARGET_UUID));

    assertEquals(404, ex.getHttpCode());
  }

  @Test
  @DisplayName("updatePresence: calls repository updateLastSeenAt")
  void updatePresence_SetsLastSeenAt() {
    profileService.updatePresence(REQUESTER_ID);

    verify(userRepository).updateLastSeenAt(REQUESTER_ID);
  }
}

package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.UserDevice;
import id.xyz.chatapps_graph.domain.repository.UserDeviceRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

  @Mock private UserDeviceRepository userDeviceRepository;

  @InjectMocks private DeviceServiceImpl deviceService;

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final String TOKEN = "fcm-token-abc123";

  @Test
  @DisplayName("registerDevice: new token — creates device")
  void registerDevice_NewToken_Creates() {
    when(userDeviceRepository.findByDeviceToken(TOKEN)).thenReturn(Optional.empty());
    when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(inv -> inv.getArgument(0));

    deviceService.registerDevice(USER_ID, TOKEN, "ANDROID");

    ArgumentCaptor<UserDevice> captor = ArgumentCaptor.forClass(UserDevice.class);
    verify(userDeviceRepository).save(captor.capture());
    UserDevice saved = captor.getValue();
    assertEquals(USER_ID, saved.getUserId());
    assertEquals(TOKEN, saved.getDeviceToken());
    assertEquals("ANDROID", saved.getPlatform());
    assertNull(saved.getDeletedAt());
  }

  @Test
  @DisplayName("registerDevice: soft-deleted token — restores")
  void registerDevice_SoftDeletedToken_Restores() {
    UserDevice existing = UserDevice.builder()
        .userId(OTHER_USER_ID).deviceToken(TOKEN).platform("IOS")
        .deletedAt(OffsetDateTime.now()).build();
    when(userDeviceRepository.findByDeviceToken(TOKEN)).thenReturn(Optional.of(existing));

    deviceService.registerDevice(USER_ID, TOKEN, "ANDROID");

    verify(userDeviceRepository).save(existing);
    assertEquals(USER_ID, existing.getUserId());
    assertEquals("ANDROID", existing.getPlatform());
    assertNull(existing.getDeletedAt());
  }

  @Test
  @DisplayName("registerDevice: different user owns token — reassigns")
  void registerDevice_DifferentUser_Reassigns() {
    UserDevice existing = UserDevice.builder()
        .userId(OTHER_USER_ID).deviceToken(TOKEN).platform("ANDROID").build();
    when(userDeviceRepository.findByDeviceToken(TOKEN)).thenReturn(Optional.of(existing));

    deviceService.registerDevice(USER_ID, TOKEN, "ANDROID");

    verify(userDeviceRepository).save(existing);
    assertEquals(USER_ID, existing.getUserId());
  }

  @Test
  @DisplayName("registerDevice: same user same token — updates (no-op equivalent)")
  void registerDevice_SameUserSameToken_NoOp() {
    UserDevice existing = UserDevice.builder()
        .userId(USER_ID).deviceToken(TOKEN).platform("ANDROID").build();
    when(userDeviceRepository.findByDeviceToken(TOKEN)).thenReturn(Optional.of(existing));

    deviceService.registerDevice(USER_ID, TOKEN, "ANDROID");

    verify(userDeviceRepository).save(existing);
    assertEquals(USER_ID, existing.getUserId());
  }

  @Test
  @DisplayName("registerDevice: invalid platform — throws 400")
  void registerDevice_InvalidPlatform_Throws() {
    GeneralException ex = assertThrows(GeneralException.class,
        () -> deviceService.registerDevice(USER_ID, TOKEN, "WINDOWS"));

    assertEquals(400, ex.getHttpCode());
    assertEquals("INVALID_PLATFORM", ex.getKey());
  }

  @Test
  @DisplayName("unregisterDevice: existing token — sets deletedAt")
  void deleteDevice_ExistingToken_SetsDeletedAt() {
    UserDevice device = UserDevice.builder()
        .userId(USER_ID).deviceToken(TOKEN).platform("ANDROID").build();
    when(userDeviceRepository.findByDeviceToken(TOKEN)).thenReturn(Optional.of(device));

    deviceService.unregisterDevice(USER_ID, TOKEN);

    verify(userDeviceRepository).save(device);
    assertNotNull(device.getDeletedAt());
  }

  @Test
  @DisplayName("unregisterDevice: token not found — throws 404")
  void deleteDevice_NotFound_Returns404() {
    when(userDeviceRepository.findByDeviceToken(TOKEN)).thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class,
        () -> deviceService.unregisterDevice(USER_ID, TOKEN));

    assertEquals(404, ex.getHttpCode());
    assertEquals("DEVICE_NOT_FOUND", ex.getKey());
  }

  @Test
  @DisplayName("unregisterDevice: token owned by other user — throws 404")
  void deleteDevice_OtherUser_Returns404() {
    UserDevice device = UserDevice.builder()
        .userId(OTHER_USER_ID).deviceToken(TOKEN).platform("ANDROID").build();
    when(userDeviceRepository.findByDeviceToken(TOKEN)).thenReturn(Optional.of(device));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> deviceService.unregisterDevice(USER_ID, TOKEN));

    assertEquals(404, ex.getHttpCode());
  }

  @Test
  @DisplayName("unregisterDevice: token already soft-deleted — throws 404")
  void deleteDevice_AlreadyDeleted_Returns404() {
    UserDevice device = UserDevice.builder()
        .userId(USER_ID).deviceToken(TOKEN).platform("ANDROID")
        .deletedAt(OffsetDateTime.now().minusDays(1)).build();
    when(userDeviceRepository.findByDeviceToken(TOKEN)).thenReturn(Optional.of(device));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> deviceService.unregisterDevice(USER_ID, TOKEN));

    assertEquals(404, ex.getHttpCode());
    assertEquals("DEVICE_NOT_FOUND", ex.getKey());
  }
}

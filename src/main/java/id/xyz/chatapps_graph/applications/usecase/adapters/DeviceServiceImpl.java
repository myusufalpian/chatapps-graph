package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.DeviceService;
import id.xyz.chatapps_graph.domain.entity.UserDevice;
import id.xyz.chatapps_graph.domain.repository.UserDeviceRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

  private static final Set<String> VALID_PLATFORMS = Set.of("ANDROID", "IOS", "WEB");

  private final UserDeviceRepository userDeviceRepository;

  @Override
  public void registerDevice(Long userId, String deviceToken, String platform) {
    if (!VALID_PLATFORMS.contains(platform.toUpperCase())) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "INVALID_PLATFORM",
          "Platform must be one of: ANDROID, IOS, WEB");
    }

    Optional<UserDevice> existing = userDeviceRepository.findByDeviceToken(deviceToken);

    if (existing.isPresent()) {
      UserDevice device = existing.get();
      device.setUserId(userId);
      device.setDeletedAt(null);
      device.setPlatform(platform.toUpperCase());
      userDeviceRepository.save(device);
    } else {
      userDeviceRepository.save(UserDevice.builder()
          .userId(userId)
          .deviceToken(deviceToken)
          .platform(platform.toUpperCase())
          .build());
    }
  }

  @Override
  public void unregisterDevice(Long userId, String deviceToken) {
    UserDevice device = userDeviceRepository.findByDeviceToken(deviceToken)
        .filter(d -> d.getUserId().equals(userId))
        .filter(d -> d.getDeletedAt() == null)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "DEVICE_NOT_FOUND", "Device token not found"));

    device.setDeletedAt(OffsetDateTime.now());
    userDeviceRepository.save(device);
  }
}

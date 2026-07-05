package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.DeviceService;
import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import id.xyz.chatapps_graph.framework.dto.DeviceRegistrationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController extends BaseApiController {

  private final DeviceService deviceService;

  @PostMapping
  public ResponseEntity<BaseResponse<Void>> registerDevice(
      @RequestAttribute("X-User-Id") Long userId,
      @Valid @RequestBody DeviceRegistrationRequest request) {

    deviceService.registerDevice(userId, request.deviceToken(), request.platform());
    return created(null, "Device registered");
  }

  @DeleteMapping("/{token}")
  public ResponseEntity<BaseResponse<Void>> deleteDevice(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("token") String token) {

    deviceService.unregisterDevice(userId, token);
    return success("Device unregistered");
  }
}

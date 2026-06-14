package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.ProfileService;
import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import id.xyz.chatapps_graph.framework.dto.PresenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class PresenceController extends BaseApiController {

  private final ProfileService profileService;

  @PutMapping("/presence")
  public ResponseEntity<BaseResponse<Void>> updatePresence(
      @RequestAttribute("X-User-Id") Long userId) {
    profileService.updatePresence(userId);
    return success("Presence updated");
  }

  @GetMapping("/{uuid}/presence")
  public ResponseEntity<BaseResponse<PresenceResponse>> getPresence(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid) {
    PresenceResponse response = profileService.getPresence(userId, uuid);
    return success(response, "Presence retrieved");
  }
}

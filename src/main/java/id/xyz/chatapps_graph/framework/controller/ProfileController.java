package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.ProfileService;
import id.xyz.chatapps_graph.framework.dto.BaseMetadata;
import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import id.xyz.chatapps_graph.framework.dto.PrivacyRequest;
import id.xyz.chatapps_graph.framework.mapper.ProfileMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProfileController {

  private final ProfileService profileService;
  private final ProfileMapper profileMapper;

  @QueryMapping
  public Map<String, Object> myProfile(@AuthenticationPrincipal Jwt jwt) {
    return profileMapper.toProfileMap(profileService.getMyProfile(jwt.getClaim("preferred_username")));
  }

  @QueryMapping
  public Map<String, Object> userProfile(@Argument String userUuid, @AuthenticationPrincipal Jwt jwt) {
    return profileMapper.toPublicProfileMap(profileService.getUserProfile(userUuid));
  }

  @QueryMapping
  public List<Map<String, Object>> contacts(@AuthenticationPrincipal Jwt jwt) {
    return profileService.getContacts(jwt.getClaim("preferred_username"))
        .stream().map(profileMapper::toContactMap).toList();
  }

  @MutationMapping
  public Map<String, Object> updateMyFullName(@Argument String fullName, @AuthenticationPrincipal Jwt jwt) {
    return profileMapper.toProfileMap(profileService.updateMyFullName(jwt.getClaim("preferred_username"), fullName));
  }

  @MutationMapping
  public Map<String, Object> updateMyStatus(@Argument String aboutDesc, @AuthenticationPrincipal Jwt jwt) {
    return profileMapper.toProfileMap(profileService.updateMyStatus(jwt.getClaim("preferred_username"), aboutDesc));
  }

  @MutationMapping
  public Map<String, Object> updateMyProfilePhoto(@Argument String photoUrl, @AuthenticationPrincipal Jwt jwt) {
    return profileMapper.toProfileMap(profileService.updateMyProfilePhoto(jwt.getClaim("preferred_username"), photoUrl));
  }

  @MutationMapping
  public Map<String, Object> updateContactDisplayName(@Argument String contactUserUuid,
      @Argument String displayName, @AuthenticationPrincipal Jwt jwt) {
    return profileMapper.toContactMap(profileService.updateContactDisplayName(
        jwt.getClaim("preferred_username"), contactUserUuid, displayName));
  }

  @MutationMapping
  public List<Map<String, Object>> syncContacts(@Argument List<String> phoneNumbers,
      @AuthenticationPrincipal Jwt jwt) {
    return profileService.syncContacts(jwt.getClaim("preferred_username"), phoneNumbers)
        .stream().map(profileMapper::toContactMap).toList();
  }

  @PutMapping("/profile/privacy")
  public ResponseEntity<BaseResponse<Void>> updatePrivacy(
      @RequestAttribute("X-User-Id") Long userId,
      @Valid @RequestBody PrivacyRequest request) {
    profileService.updatePrivacySetting(userId, request.hideReadReceipt());
    return ResponseEntity.ok(new BaseResponse<>(null, new BaseMetadata("Privacy setting updated")));
  }
}

package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.ProfileService;
import id.xyz.chatapps_graph.framework.mapper.ProfileMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
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
}

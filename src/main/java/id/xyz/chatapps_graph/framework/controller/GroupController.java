package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.GroupService;
import id.xyz.chatapps_graph.framework.dto.AddGroupMemberRequest;
import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import id.xyz.chatapps_graph.framework.dto.CreateGroupRequest;
import id.xyz.chatapps_graph.framework.dto.CreateGroupResponse;
import id.xyz.chatapps_graph.framework.dto.GroupInfoResponse;
import id.xyz.chatapps_graph.framework.dto.UpdateGroupRequest;
import id.xyz.chatapps_graph.framework.dto.UpdateGroupSettingsRequest;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.utility.JsonUtil;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController extends BaseApiController {

  private final GroupService groupService;

  @PostMapping
  public ResponseEntity<BaseResponse<CreateGroupResponse>> createGroup(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestPart("metadata") String metadataJson,
      @RequestPart(value = "file", required = false) MultipartFile avatar) {

    CreateGroupRequest request = parseMetadata(metadataJson);
    CreateGroupResponse response = groupService.createGroup(userId, request, avatar);
    return created(response, "Group created");
  }

  @GetMapping("/{groupUuid}")
  public ResponseEntity<BaseResponse<GroupInfoResponse>> getGroupInfo(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("groupUuid") String groupUuid) {

    GroupInfoResponse response = groupService.getGroupInfo(userId, groupUuid);
    return success(response, "Group info retrieved");
  }

  @PutMapping("/{groupUuid}")
  public ResponseEntity<BaseResponse<Void>> updateGroupInfo(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("groupUuid") String groupUuid,
      @Valid @RequestBody UpdateGroupRequest request) {

    groupService.updateGroupInfo(userId, groupUuid, request);
    return success("Group updated");
  }

  @DeleteMapping("/{groupUuid}")
  public ResponseEntity<BaseResponse<Void>> dissolveGroup(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("groupUuid") String groupUuid) {

    groupService.dissolveGroup(userId, groupUuid);
    return success("Group dissolved");
  }

  @PutMapping("/{groupUuid}/avatar")
  public ResponseEntity<BaseResponse<Map<String, String>>> updateAvatar(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("groupUuid") String groupUuid,
      @RequestPart("file") MultipartFile file) {

    String avatarUrl = groupService.updateAvatar(userId, groupUuid, file);
    return success(Map.of("avatarUrl", avatarUrl), "Avatar updated");
  }

  @PutMapping("/{groupUuid}/settings")
  public ResponseEntity<BaseResponse<Void>> updateSettings(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("groupUuid") String groupUuid,
      @Valid @RequestBody UpdateGroupSettingsRequest request) {

    groupService.updateSettings(userId, groupUuid, request);
    return success("Settings updated");
  }

  @PostMapping("/{groupUuid}/members")
  public ResponseEntity<BaseResponse<Void>> addMember(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("groupUuid") String groupUuid,
      @Valid @RequestBody AddGroupMemberRequest request) {

    groupService.addMember(userId, groupUuid, request.userUuid());
    return success("Member added");
  }

  @DeleteMapping("/{groupUuid}/members/{targetUserUuid}")
  public ResponseEntity<BaseResponse<Void>> removeMember(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("groupUuid") String groupUuid,
      @PathVariable("targetUserUuid") String targetUserUuid) {

    groupService.removeMember(userId, groupUuid, targetUserUuid);
    return success("Member removed");
  }

  @PostMapping("/{groupUuid}/leave")
  public ResponseEntity<BaseResponse<Void>> leaveGroup(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("groupUuid") String groupUuid) {

    groupService.leaveGroup(userId, groupUuid);
    return success("Left group");
  }

  @PutMapping("/{groupUuid}/admins/{targetUserUuid}")
  public ResponseEntity<BaseResponse<Void>> promoteAdmin(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("groupUuid") String groupUuid,
      @PathVariable("targetUserUuid") String targetUserUuid) {

    groupService.promoteAdmin(userId, groupUuid, targetUserUuid);
    return success("Admin promoted");
  }

  @DeleteMapping("/{groupUuid}/admins/{targetUserUuid}")
  public ResponseEntity<BaseResponse<Void>> demoteAdmin(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("groupUuid") String groupUuid,
      @PathVariable("targetUserUuid") String targetUserUuid) {

    groupService.demoteAdmin(userId, groupUuid, targetUserUuid);
    return success("Admin demoted");
  }

  private CreateGroupRequest parseMetadata(String metadataJson) {
    CreateGroupRequest request = JsonUtil.stringToModel(metadataJson, CreateGroupRequest.class);
    if (request == null || request.name() == null || request.name().isBlank()) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "INVALID_METADATA", "Invalid or missing group metadata");
    }
    if (request.participantUuids() == null || request.participantUuids().size() < 2) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "INVALID_METADATA", "At least 2 participants required");
    }
    return request;
  }
}

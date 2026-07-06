package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.framework.dto.CreateGroupRequest;
import id.xyz.chatapps_graph.framework.dto.CreateGroupResponse;
import id.xyz.chatapps_graph.framework.dto.GroupInfoResponse;
import id.xyz.chatapps_graph.framework.dto.UpdateGroupRequest;
import id.xyz.chatapps_graph.framework.dto.UpdateGroupSettingsRequest;
import org.springframework.web.multipart.MultipartFile;

public interface GroupService {

  CreateGroupResponse createGroup(Long creatorId, CreateGroupRequest request, MultipartFile avatar);

  void addMember(Long actorId, String groupUuid, String targetUserUuid);

  void removeMember(Long actorId, String groupUuid, String targetUserUuid);

  void leaveGroup(Long userId, String groupUuid);

  void updateGroupInfo(Long actorId, String groupUuid, UpdateGroupRequest request);

  String updateAvatar(Long actorId, String groupUuid, MultipartFile file);

  void updateSettings(Long actorId, String groupUuid, UpdateGroupSettingsRequest request);

  void promoteAdmin(Long ownerId, String groupUuid, String targetUserUuid);

  void demoteAdmin(Long ownerId, String groupUuid, String targetUserUuid);

  void dissolveGroup(Long ownerId, String groupUuid);

  GroupInfoResponse getGroupInfo(Long userId, String groupUuid);
}

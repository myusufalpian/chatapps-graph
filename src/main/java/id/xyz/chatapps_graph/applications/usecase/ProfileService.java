package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.Contact;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.framework.dto.PresenceResponse;
import java.util.List;

public interface ProfileService {
  User getMyProfile(String principal);
  User getUserProfile(String userUuid);
  User updateMyFullName(String principal, String fullName);
  User updateMyStatus(String principal, String aboutDesc);
  User updateMyProfilePhoto(String principal, String photoUrl);
  Contact updateContactDisplayName(String principal, String contactUserUuid, String displayName);
  List<Contact> syncContacts(String principal, List<String> phoneNumbers);
  List<Contact> getContacts(String principal);
  void updatePresence(Long userId);
  PresenceResponse getPresence(Long requesterId, String targetUuid);
  void updatePrivacySetting(Long userId, Boolean hideReadReceipt);
}

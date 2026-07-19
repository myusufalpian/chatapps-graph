package id.xyz.chatapps_graph.infrastructure.mapper;

import id.xyz.chatapps_graph.domain.entity.Contact;
import id.xyz.chatapps_graph.domain.entity.User;
import java.util.HashMap;
import java.util.Map;

public class ProfileMapper {

  public static Map<String, Object> toProfileMap(User user) {
    return buildProfileMap(user, true);
  }

  public static Map<String, Object> toPublicProfileMap(User user) {
    return buildProfileMap(user, false);
  }

  private static Map<String, Object> buildProfileMap(User user, boolean includePhone) {
    Map<String, Object> map = new HashMap<>();
    map.put("userUuid", user.getUserUuid());
    map.put("phone", includePhone ? user.getUserPhone() : null);
    map.put("fullName", user.getUserFullName());
    map.put("profilePhoto", user.getProfilePhoto());
    map.put("aboutDesc", user.getAbout() != null ? user.getAbout().getAboutDesc() : null);
    return map;
  }

  public static Map<String, Object> toContactMap(Contact contact) {
    User cu = contact.getContactUser();
    Map<String, Object> map = new HashMap<>();
    map.put("contactUuid", contact.getContactUuid());
    map.put("userUuid", cu.getUserUuid());
    map.put("displayName", contact.getDisplayName());
    map.put("fullName", cu.getUserFullName());
    map.put("profilePhoto", cu.getProfilePhoto());
    map.put("aboutDesc", cu.getAbout() != null ? cu.getAbout().getAboutDesc() : null);
    return map;
  }
}

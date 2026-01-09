package id.xyz.chatapps_graph.infrastructure.constant;

import lombok.experimental.UtilityClass;

public class SQLConstants {

  @UtilityClass
  public static class UserSQL {
    public static final String getUserDetailByUserIdAndUserStatus = "SELECT u.user_uuid as userUuid, u.user_phone as userPhone, u.user_email as userEmail, u.user_full_name as userFullName, u.user_status as userStatus, au.about_uuid as aboutUuid, au.about_desc as aboutDesc FROM chatapps.users u JOIN chatapps.about_user au ON u.about_id = au.about_id WHERE u.user_id = :userId and u.user_status = :userStatus";
  }

}

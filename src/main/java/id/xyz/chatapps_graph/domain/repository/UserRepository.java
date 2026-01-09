package id.xyz.chatapps_graph.domain.repository;

import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.UserSQL;

import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.framework.dto.UserDetailDTO;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findUserByUserPhoneAndUserStatus(String userPhone, Integer userStatus);

  Optional<User> findUserByUserUuidAndUserStatus(String userUuid, Integer userStatus);

  @Query(nativeQuery = true, value = UserSQL.getUserDetailByUserIdAndUserStatus)
  Optional<UserDetailDTO> getUserDetailByUserId(Long userId, Integer userStatus);
}

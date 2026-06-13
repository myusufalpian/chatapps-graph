package id.xyz.chatapps_graph.domain.repository;

import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.UserSQL;

import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.framework.dto.UserDetailDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  @EntityGraph(attributePaths = {"about"})
  Optional<User> findUserByUserPhoneAndUserStatus(String userPhone, Integer userStatus);

  @EntityGraph(attributePaths = {"about"})
  Optional<User> findUserByUserUuidAndUserStatus(String userUuid, Integer userStatus);

  List<User> findByUserPhoneInAndUserStatus(List<String> userPhones, Integer userStatus);

  Optional<User> findByUserMailAndUserStatus(String userMail, Integer userStatus);

  @Query(nativeQuery = true, value = UserSQL.GET_USER_DETAIL_BY_USER_ID_AND_USER_STATUS)
  Optional<UserDetailDTO> getUserDetailByUserId(Long userId, Integer userStatus);
}

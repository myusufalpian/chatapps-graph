package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.UserDevice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

  Optional<UserDevice> findByDeviceToken(String deviceToken);

  List<UserDevice> findByUserIdAndDeletedAtIsNull(Long userId);

  List<UserDevice> findByUserIdInAndDeletedAtIsNull(List<Long> userIds);
}

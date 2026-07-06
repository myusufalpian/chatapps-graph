package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.Group;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

  Optional<Group> findByGroupUuid(String groupUuid);

  Optional<Group> findByConversationId(Long conversationId);

  List<Group> findByConversationIdIn(List<Long> conversationIds);
}

package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.MessageEditHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageEditHistoryRepository extends JpaRepository<MessageEditHistory, Long> {

  List<MessageEditHistory> findAllByMessageIdOrderByEditedAtAsc(Long messageId);
}

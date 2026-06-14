package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.MessageReaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

  Optional<MessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);

  List<MessageReaction> findAllByMessageIdIn(List<Long> messageIds);

  void deleteByMessageIdAndUserId(Long messageId, Long userId);
}

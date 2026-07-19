package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

  Optional<Conversation> findByConversationUuid(String conversationUuid);

  List<Conversation> findByDisappearingTtlIsNotNull(org.springframework.data.domain.Pageable pageable);
}


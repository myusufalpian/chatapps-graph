package id.xyz.chatapps_graph.domain.repository;

import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.ConversationParticipantSQL;

import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

  Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

  List<ConversationParticipant> findAllByUserId(Long userId);

  List<ConversationParticipant> findAllByConversationId(Long conversationId);

  @Query(value = ConversationParticipantSQL.FIND_PRIVATE_CONVERSATION_BETWEEN, nativeQuery = true)
  Optional<Long> findPrivateConversationBetween(Long userIdA, Long userIdB);
}

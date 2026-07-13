package id.xyz.chatapps_graph.infrastructure.scheduler;

import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisappearingMessageScheduler {

  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;

  @Scheduled(cron = "${chat.disappearing.cleanup-cron:0 0 * * * *}")
  @Transactional
  public void cleanupExpiredMessages() {
    log.info("Starting disappearing messages cleanup job...");
    try {
      List<Conversation> conversations = conversationRepository.findByDisappearingTtlIsNotNull();
      int totalDeleted = 0;
      OffsetDateTime now = OffsetDateTime.now();

      for (Conversation conv : conversations) {
        if (totalDeleted >= 1000) {
          break;
        }
        if (conv.getDisappearingTtl() != null && conv.getDisappearingTtl() > 0) {
          int limit = 1000 - totalDeleted;
          OffsetDateTime threshold = now.minusHours(conv.getDisappearingTtl());
          List<Long> expiredIds = messageRepository.findExpiredMessageIds(
              conv.getConversationId(), threshold, PageRequest.of(0, limit));

          if (!expiredIds.isEmpty()) {
            messageRepository.softDeleteMessages(expiredIds, now);
            totalDeleted += expiredIds.size();
            log.info("Soft deleted {} expired messages for conversation ID {}", expiredIds.size(), conv.getConversationId());
          }
        }
      }
      log.info("Disappearing messages cleanup job finished. Total messages deleted: {}", totalDeleted);
    } catch (Exception e) {
      log.error("Error occurred during disappearing messages cleanup", e);
    }
  }
}

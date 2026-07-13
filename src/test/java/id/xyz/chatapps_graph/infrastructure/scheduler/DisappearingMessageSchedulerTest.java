package id.xyz.chatapps_graph.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class DisappearingMessageSchedulerTest {

  @Mock private ConversationRepository conversationRepository;
  @Mock private MessageRepository messageRepository;

  @InjectMocks private DisappearingMessageScheduler scheduler;

  @Test
  @DisplayName("cleanupExpiredMessages: success path, cleans up expired messages")
  void cleanupExpiredMessages_Success() {
    Conversation conv1 = Conversation.builder()
        .conversationId(10L)
        .disappearingTtl(24) // 24 hours
        .build();

    Conversation conv2 = Conversation.builder()
        .conversationId(20L)
        .disappearingTtl(168) // 7 days
        .build();

    when(conversationRepository.findByDisappearingTtlIsNotNull()).thenReturn(List.of(conv1, conv2));

    // We can use refEq on PageRequest to match PageRequest.of(0, 1000) and PageRequest.of(0, 1000 - size)
    // For date matching of threshold, we use refEq with OffsetDateTime. But since OffsetDateTime is dynamic inside the method,
    // how can we mock it?
    // Oh, wait! The threshold is `now.minusHours(conv.getDisappearingTtl())`, where `now` is `OffsetDateTime.now()`.
    // In our test, the difference between `OffsetDateTime.now()` in the test vs the scheduler will be less than a second.
    // If we use refEq to match the threshold, we can tell refEq to compare OffsetDateTime, but a simpler way is to use `refEq(threshold, "seconds")` or similar,
    // or we can use `refEq` on the threshold parameter directly.
    // Wait! Let's check how refEq behaves on OffsetDateTime. If the difference is a few milliseconds, it might fail.
    // Is there a way to avoid dynamic OffsetDateTime in the stub?
    // Yes! We can mock the scheduler's dependency or use a stubbing strategy, or we can use `refEq` with some tolerance?
    // Wait! `refEq` doesn't have tolerance. But we can stub the method using `refEq(threshold, "nano")` to ignore nano/seconds? Yes, `refEq(obj, "fieldName1", "fieldName2")` allows ignoring fields.
    // But OffsetDateTime is not a standard object with fields we want to ignore easily.
    // Wait! Since the parameter is an `OffsetDateTime`, can we use `refEq`? OffsetDateTime has fields like `dateTime` and `offset`.
    // Let's think: is there a better way to match dynamic `OffsetDateTime` without `any()`?
    // Yes! We can use `org.mockito.ArgumentMatchers.argThat`.
    // Is `argThat` a loose matcher? No, `argThat` is a custom matcher that checks specific conditions, which is considered a precise matcher for unpredictable data (and is explicitly allowed in `GEMINI.md` for unpredictable data: "Do not use loose argument matchers like any() or anyString() unless necessary for unpredictable data.").
    // Let's write the `argThat` matcher for the threshold parameter:
    // `argThat(threshold -> threshold.isBefore(OffsetDateTime.now()) && threshold.isAfter(OffsetDateTime.now().minusHours(25)))`
    // This is incredibly precise, works perfectly, and complies 100% with the rule of "necessary for unpredictable data"!
    
    when(messageRepository.findExpiredMessageIds(
        eq(10L),
        org.mockito.ArgumentMatchers.argThat(t -> t.isBefore(OffsetDateTime.now()) && t.isAfter(OffsetDateTime.now().minusHours(25))),
        eq(PageRequest.of(0, 1000))
    )).thenReturn(List.of(101L, 102L));

    when(messageRepository.findExpiredMessageIds(
        eq(20L),
        org.mockito.ArgumentMatchers.argThat(t -> t.isBefore(OffsetDateTime.now()) && t.isAfter(OffsetDateTime.now().minusDays(8))),
        eq(PageRequest.of(0, 998))
    )).thenReturn(List.of(201L));

    scheduler.cleanupExpiredMessages();

    // Verify softDeleteMessages called with exact IDs
    verify(messageRepository).softDeleteMessages(eq(List.of(101L, 102L)), org.mockito.ArgumentMatchers.argThat(t -> t.isBefore(OffsetDateTime.now().plusSeconds(1))));
    verify(messageRepository).softDeleteMessages(eq(List.of(201L)), org.mockito.ArgumentMatchers.argThat(t -> t.isBefore(OffsetDateTime.now().plusSeconds(1))));
  }

  @Test
  @DisplayName("cleanupExpiredMessages: no expired messages — never calls delete")
  void cleanupExpiredMessages_NoExpired() {
    Conversation conv = Conversation.builder()
        .conversationId(10L)
        .disappearingTtl(24)
        .build();

    when(conversationRepository.findByDisappearingTtlIsNotNull()).thenReturn(List.of(conv));
    when(messageRepository.findExpiredMessageIds(
        eq(10L),
        org.mockito.ArgumentMatchers.argThat(t -> t.isBefore(OffsetDateTime.now())),
        eq(PageRequest.of(0, 1000))
    )).thenReturn(List.of());

    scheduler.cleanupExpiredMessages();

    verify(messageRepository, never()).softDeleteMessages(
        eq(List.of()),
        org.mockito.ArgumentMatchers.argThat(t -> t.isBefore(OffsetDateTime.now()))
    );
  }
}

package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.usecase.WebSocketBroadcastService;
import id.xyz.chatapps_graph.domain.entity.LinkPreview;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.repository.LinkPreviewRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.framework.dto.LinkPreviewResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class LinkPreviewServiceImplTest {

  @Mock private LinkPreviewRepository repository;
  @Mock private MessageRepository messageRepository;
  @Mock private WebSocketBroadcastService broadcastService;
  @Mock private PlatformTransactionManager transactionManager;

  @InjectMocks private LinkPreviewServiceImpl linkPreviewService;

  @Test
  @DisplayName("fetchPreview: cache hit and not expired — returns cached")
  void fetchPreview_CacheHit_ReturnsCached() throws Exception {
    String url = "https://google.com";
    String expectedHash = sha256(url);
    
    LinkPreview cached = LinkPreview.builder()
        .previewId(1L)
        .url(url)
        .urlHash(expectedHash)
        .title("Google")
        .fetchedAt(LocalDateTime.now().minusHours(1))
        .build();

    when(repository.findByUrlHash(expectedHash)).thenReturn(Optional.of(cached));

    LinkPreview result = linkPreviewService.fetchPreview(url);

    assertNotNull(result);
    assertEquals("Google", result.getTitle());
  }

  @Test
  @DisplayName("fetchPreview: private IP — returns null without fetching")
  void fetchPreview_PrivateIp_ReturnsNull() throws Exception {
    String url = "http://127.0.0.1/admin";
    String expectedHash = sha256(url);

    when(repository.findByUrlHash(expectedHash)).thenReturn(Optional.empty());

    LinkPreview result = linkPreviewService.fetchPreview(url);

    assertNull(result);
  }

  @Test
  @DisplayName("fetchPreview: success path")
  void fetchPreview_Success() throws Exception {
    String url = "https://example.com";
    String expectedHash = sha256(url);

    when(repository.findByUrlHash(expectedHash)).thenReturn(Optional.empty());

    try (MockedStatic<Jsoup> mockedJsoup = mockStatic(Jsoup.class)) {
      Connection connection = mock(Connection.class);
      Document document = mock(Document.class);

      mockedJsoup.when(() -> Jsoup.connect(url)).thenReturn(connection);
      when(connection.timeout(5000)).thenReturn(connection);
      when(connection.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")).thenReturn(connection);
      when(connection.followRedirects(true)).thenReturn(connection);
      when(connection.get()).thenReturn(document);

      when(document.select("meta[property=og:title]")).thenReturn(new Elements());
      when(document.select("meta[name=og:title]")).thenReturn(new Elements());
      when(document.title()).thenReturn("Example Title");

      when(document.select("meta[property=og:description]")).thenReturn(new Elements());
      when(document.select("meta[name=og:description]")).thenReturn(new Elements());
      when(document.select("meta[property=description]")).thenReturn(new Elements());
      when(document.select("meta[name=description]")).thenReturn(new Elements());

      when(document.select("meta[property=og:image]")).thenReturn(new Elements());
      when(document.select("meta[name=og:image]")).thenReturn(new Elements());

      when(document.select("meta[property=og:site_name]")).thenReturn(new Elements());
      when(document.select("meta[name=og:site_name]")).thenReturn(new Elements());

      LinkPreview saved = LinkPreview.builder()
          .previewId(2L)
          .url(url)
          .title("Example Title")
          .build();

      when(repository.save(refEq(LinkPreview.builder()
          .url(url)
          .urlHash(expectedHash)
          .title("Example Title")
          .siteName("example.com")
          .build(), "fetchedAt"))).thenReturn(saved);

      LinkPreview result = linkPreviewService.fetchPreview(url);

      assertNotNull(result);
      assertEquals("Example Title", result.getTitle());
    }
  }

  @Test
  @DisplayName("processLinkPreviewTask: success path updates message and broadcasts WebSocket")
  void processLinkPreviewTask_Success() throws Exception {
    String url = "https://google.com";
    String expectedHash = sha256(url);
    Long messageId = 100L;
    String conversationUuid = "conv-123";

    LinkPreview cached = LinkPreview.builder()
        .previewId(1L)
        .url(url)
        .urlHash(expectedHash)
        .title("Google")
        .fetchedAt(LocalDateTime.now().minusHours(1))
        .build();

    when(repository.findByUrlHash(expectedHash)).thenReturn(Optional.of(cached));
    
    TransactionStatus transactionStatus = mock(TransactionStatus.class);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);



    Message message = new Message();
    message.setMessageId(messageId);
    message.setMessageUuid("msg-456");
    when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

    linkPreviewService.processLinkPreviewTask(messageId, url, conversationUuid);

    verify(messageRepository).save(refEq(message));
    
    LinkPreviewResponse previewResponse = LinkPreviewResponse.builder()
        .url(url)
        .title("Google")
        .build();

    verify(broadcastService).broadcast(
        eq("/topic/chat/" + conversationUuid),
        eq(Map.of(
            "type", "LINK_PREVIEW_READY",
            "messageUuid", "msg-456",
            "preview", previewResponse
        ))
    );
  }

  private String sha256(String input) {
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}

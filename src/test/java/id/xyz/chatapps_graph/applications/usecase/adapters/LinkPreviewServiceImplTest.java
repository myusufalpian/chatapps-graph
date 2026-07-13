package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.LinkPreview;
import id.xyz.chatapps_graph.domain.repository.LinkPreviewRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

@ExtendWith(MockitoExtension.class)
class LinkPreviewServiceImplTest {

  @Mock private LinkPreviewRepository repository;

  @InjectMocks private LinkPreviewServiceImpl linkPreviewService;

  @Test
  @DisplayName("fetchPreviewAsync: cache hit and not expired — returns cached")
  void fetchPreviewAsync_CacheHit_ReturnsCached() throws Exception {
    String url = "https://google.com";
    String hash = "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae"; // sha256 of url is computed internally
    
    LinkPreview cached = LinkPreview.builder()
        .previewId(1L)
        .url(url)
        .urlHash(hash)
        .title("Google")
        .fetchedAt(LocalDateTime.now().minusHours(1))
        .build();

    // Calculate the expected hash for this specific URL:
    // "https://google.com" sha256:
    // Let's use the exact hash that the service computes.
    // The service computes hash of "https://google.com"
    String expectedHash = sha256(url);

    when(repository.findByUrlHash(expectedHash)).thenReturn(Optional.of(cached));

    CompletableFuture<LinkPreview> future = linkPreviewService.fetchPreviewAsync(url);
    LinkPreview result = future.get();

    assertNotNull(result);
    assertEquals("Google", result.getTitle());
  }

  @Test
  @DisplayName("fetchPreviewAsync: private IP — returns null without fetching")
  void fetchPreviewAsync_PrivateIp_ReturnsNull() throws Exception {
    String url = "http://127.0.0.1/admin";
    String expectedHash = sha256(url);

    when(repository.findByUrlHash(expectedHash)).thenReturn(Optional.empty());

    CompletableFuture<LinkPreview> future = linkPreviewService.fetchPreviewAsync(url);
    LinkPreview result = future.get();

    assertNull(result);
  }

  @Test
  @DisplayName("fetchPreviewAsync: success path")
  void fetchPreviewAsync_Success() throws Exception {
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

      // Setup document returns for title/meta tags
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

      // We match the saved object using refEq to ignore fetchedAt which is dynamically generated using LocalDateTime.now()
      when(repository.save(refEq(LinkPreview.builder()
          .url(url)
          .urlHash(expectedHash)
          .title("Example Title")
          .siteName("example.com")
          .build(), "fetchedAt"))).thenReturn(saved);

      CompletableFuture<LinkPreview> future = linkPreviewService.fetchPreviewAsync(url);
      LinkPreview result = future.get();

      assertNotNull(result);
      assertEquals("Example Title", result.getTitle());
    }
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

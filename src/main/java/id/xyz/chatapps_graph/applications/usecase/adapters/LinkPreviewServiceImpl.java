package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.LinkPreviewService;
import id.xyz.chatapps_graph.applications.usecase.WebSocketBroadcastService;
import id.xyz.chatapps_graph.domain.entity.LinkPreview;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.repository.LinkPreviewRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.framework.dto.LinkPreviewResponse;
import java.net.InetAddress;
import java.net.URI;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkPreviewServiceImpl implements LinkPreviewService {

  private final LinkPreviewRepository repository;
  private final MessageRepository messageRepository;
  private final WebSocketBroadcastService broadcastService;
  private final PlatformTransactionManager transactionManager;

  @Override
  public LinkPreview fetchPreview(String url) {
    try {
      if (url == null || url.trim().isEmpty()) {
        return null;
      }

      String hash = sha256(url);

      // Database read in its own short transaction
      TransactionTemplate readTx = new TransactionTemplate(transactionManager);
      readTx.setReadOnly(true);
      Optional<LinkPreview> cachedOpt = readTx.execute(status ->
          repository.findByUrlHash(hash)
              .filter(preview -> preview.getFetchedAt() != null
                  && preview.getFetchedAt().plusHours(24).isAfter(LocalDateTime.now()))
      );
      if (cachedOpt != null && cachedOpt.isPresent()) {
        return cachedOpt.get();
      }

      URI uri = URI.create(url);
      String host = uri.getHost();
      if (host == null || isPrivateIp(host)) {
        log.warn("Blocked link preview request to private/local host: {}", host);
        return null;
      }

      // Fetch existing entity (if any) for update, in a read transaction
      LinkPreview existing = readTx.execute(status ->
          repository.findByUrlHash(hash).orElse(null)
      );

      // Network I/O outside any transaction
      LinkPreview preview = parseAndBuildPreview(url, hash, existing);

      // Database write in its own short transaction
      TransactionTemplate writeTx = new TransactionTemplate(transactionManager);
      writeTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      return writeTx.execute(status -> repository.save(preview));

    } catch (Exception e) {
      log.error("Failed to fetch link preview for url: {}", url, e);
      return null;
    }
  }

  @Override
  public void processLinkPreviewTask(Long messageId, String url, String conversationUuid) {
    LinkPreview preview = fetchPreview(url);
    if (preview == null) {
      return;
    }

    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    txTemplate.executeWithoutResult(status -> {
      Message msg = messageRepository.findById(messageId).orElse(null);
      if (msg != null) {
        msg.setPreviewId(preview.getPreviewId());
        messageRepository.save(msg);
      }
    });

    Message msg = messageRepository.findById(messageId).orElse(null);
    String messageUuid = msg != null ? msg.getMessageUuid() : "";

    LinkPreviewResponse previewResponse = LinkPreviewResponse.builder()
        .url(preview.getUrl())
        .title(preview.getTitle())
        .description(preview.getDescription())
        .imageUrl(preview.getImageUrl())
        .siteName(preview.getSiteName())
        .build();

    broadcastService.broadcast(
        "/topic/chat/" + conversationUuid,
        Map.of(
            "type", "LINK_PREVIEW_READY",
            "messageUuid", messageUuid,
            "preview", previewResponse
        )
    );
  }

  private LinkPreview parseAndBuildPreview(String url, String hash, LinkPreview cached) throws Exception {
    Document doc = Jsoup.connect(url)
        .timeout(5000)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        .followRedirects(true)
        .get();

    String title = extractTitle(doc, url);
    String description = extractDescription(doc);
    String imageUrl = getMetaTag(doc, "og:image");
    String siteName = extractSiteName(doc, url);

    title = truncate(title, 500);
    description = truncate(description, 2000);
    imageUrl = truncate(imageUrl, 2048);
    siteName = truncate(siteName, 200);

    if (cached != null) {
      cached.setTitle(title);
      cached.setDescription(description);
      cached.setImageUrl(imageUrl);
      cached.setSiteName(siteName);
      cached.setFetchedAt(LocalDateTime.now());
      return cached;
    }

    return LinkPreview.builder()
        .url(url)
        .urlHash(hash)
        .title(title)
        .description(description)
        .imageUrl(imageUrl)
        .siteName(siteName)
        .fetchedAt(LocalDateTime.now())
        .build();
  }

  private String extractTitle(Document doc, String url) {
    String title = getMetaTag(doc, "og:title");
    if (title == null || title.isEmpty()) {
      title = doc.title();
    }
    return title.isEmpty() ? extractDomain(url) : title;
  }

  private String extractDescription(Document doc) {
    String description = getMetaTag(doc, "og:description");
    return (description == null || description.isEmpty()) ? getMetaTag(doc, "description") : description;
  }

  private String extractSiteName(Document doc, String url) {
    String siteName = getMetaTag(doc, "og:site_name");
    return (siteName == null || siteName.isEmpty()) ? extractDomain(url) : siteName;
  }

  private String truncate(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.length() > maxLength ? value.substring(0, maxLength) : value;
  }

  private boolean isPrivateIp(String host) {
    try {
      InetAddress[] addresses = InetAddress.getAllByName(host);
      for (InetAddress addr : addresses) {
        if (isPrivateAddress(addr)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  private boolean isPrivateAddress(InetAddress addr) {
    if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress() || addr.isMulticastAddress()) {
      return true;
    }
    byte[] ip = addr.getAddress();
    if (ip.length == 4) {
      return isPrivateIPv4(ip);
    }
    return ip.length == 16 && (addr.isLinkLocalAddress() || addr.isSiteLocalAddress());
  }

  private boolean isPrivateIPv4(byte[] ip) {
    int p1 = ip[0] & 0xFF;
    int p2 = ip[1] & 0xFF;
    return p1 == 10 
        || (p1 == 172 && (p2 >= 16 && p2 <= 31)) 
        || (p1 == 192 && p2 == 168) 
        || p1 == 127 
        || (p1 == 169 && p2 == 254);
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
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

  private String getMetaTag(Document doc, String name) {
    Elements elements = doc.select("meta[property=" + name + "]");
    if (elements.isEmpty()) {
      elements = doc.select("meta[name=" + name + "]");
    }
    if (elements.isEmpty()) {
      return null;
    }
    Element first = elements.first();
    return first == null ? null : first.attr("content");
  }

  private String extractDomain(String url) {
    try {
      URI uri = URI.create(url);
      String domain = uri.getHost();
      return domain != null && domain.startsWith("www.") ? domain.substring(4) : domain;
    } catch (Exception e) {
      return "";
    }
  }
}

package id.xyz.chatapps_graph.infrastructure.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TranslationService {

  private static final String DEFAULT_LOCALE = "id";
  private static final String SYSTEM_MESSAGES_PATH = "i18n/system-messages_";
  private static final String ERRORS_PATH = "i18n/errors_";

  private final ObjectMapper objectMapper;
  private Map<String, String> systemMessagesId = Collections.emptyMap();
  private Map<String, String> systemMessagesEn = Collections.emptyMap();
  private Map<String, String> errorsId = Collections.emptyMap();
  private Map<String, String> errorsEn = Collections.emptyMap();

  public TranslationService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  public void init() {
    systemMessagesId = Collections.unmodifiableMap(loadMessages(SYSTEM_MESSAGES_PATH + "id.json"));
    systemMessagesEn = Collections.unmodifiableMap(loadMessages(SYSTEM_MESSAGES_PATH + "en.json"));
    errorsId = Collections.unmodifiableMap(loadMessages(ERRORS_PATH + "id.json"));
    errorsEn = Collections.unmodifiableMap(loadMessages(ERRORS_PATH + "en.json"));
    log.info("TranslationService initialized with {} system message keys and {} error keys",
        systemMessagesId.size(), errorsId.size());
  }

  public String translateSystemMessage(String key, String locale, Map<String, String> params) {
    Map<String, String> messages = "en".equals(locale) ? systemMessagesEn : systemMessagesId;
    String template = messages.get(key);
    if (template == null) {
      return key;
    }
    return resolvePlaceholders(template, params);
  }

  public String translateError(String errorKey, String locale) {
    Map<String, String> errors = "en".equals(locale) ? errorsEn : errorsId;
    return errors.getOrDefault(errorKey, errorKey);
  }

  private String resolvePlaceholders(String template, Map<String, String> params) {
    if (params == null || params.isEmpty()) {
      return template;
    }
    StringBuilder sb = new StringBuilder(template);
    for (Map.Entry<String, String> entry : params.entrySet()) {
      String placeholder = "{" + entry.getKey() + "}";
      String value = entry.getValue() != null ? entry.getValue() : "";
      int idx;
      while ((idx = sb.indexOf(placeholder)) != -1) {
        sb.replace(idx, idx + placeholder.length(), value);
      }
    }
    return sb.toString();
  }

  private Map<String, String> loadMessages(String path) {
    try (InputStream is = new ClassPathResource(path).getInputStream()) {
      Map<String, String> result = objectMapper.readValue(is, new TypeReference<Map<String, String>>() {});
      return result != null ? result : new HashMap<>();
    } catch (IOException e) {
      log.warn("Failed to load translation file: {}", path);
      return new HashMap<>();
    }
  }
}

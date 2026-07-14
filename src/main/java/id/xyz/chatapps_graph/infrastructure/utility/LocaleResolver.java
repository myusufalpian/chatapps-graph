package id.xyz.chatapps_graph.infrastructure.utility;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("customLocaleResolver")
public class LocaleResolver {

  private static final Set<String> SUPPORTED_LOCALES = Set.of("id", "en");
  private static final String DEFAULT_LOCALE = "id";

  public String resolve(HttpServletRequest request) {
    String acceptLanguage = request.getHeader("Accept-Language");
    if (!StringUtils.hasLength(acceptLanguage)) {
      return DEFAULT_LOCALE;
    }

    String locale = acceptLanguage.trim().toLowerCase();
    if (locale.length() > 2) {
      locale = locale.substring(0, 2);
    }

    return SUPPORTED_LOCALES.contains(locale) ? locale : DEFAULT_LOCALE;
  }
}

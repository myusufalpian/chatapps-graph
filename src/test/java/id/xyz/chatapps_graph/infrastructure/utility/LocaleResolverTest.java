package id.xyz.chatapps_graph.infrastructure.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocaleResolverTest {

  @Mock private HttpServletRequest request;

  private final LocaleResolver localeResolver = new LocaleResolver();

  @ParameterizedTest
  @CsvSource({
      "en, en",
      "id, id",
      "en-US, en",
      "id-ID, id",
      "EN, en",
      "ID, id",
      "en-US;q=0.9, en"
  })
  @DisplayName("resolve: supported locale from Accept-Language header")
  void resolve_SupportedLocale(String header, String expected) {
    when(request.getHeader("Accept-Language")).thenReturn(header);

    String result = localeResolver.resolve(request);

    assertEquals(expected, result);
  }

  @ParameterizedTest
  @CsvSource({
      "fr, id",
      "de-DE, id",
      "ja, id",
      "zh-CN, id",
      "xx, id"
  })
  @DisplayName("resolve: unsupported locale falls back to id")
  void resolve_UnsupportedLocale_FallsBackToId(String header, String expected) {
    when(request.getHeader("Accept-Language")).thenReturn(header);

    String result = localeResolver.resolve(request);

    assertEquals(expected, result);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("resolve: null or empty header returns default id")
  void resolve_NullOrEmpty_ReturnsDefault(String header) {
    when(request.getHeader("Accept-Language")).thenReturn(header);

    String result = localeResolver.resolve(request);

    assertEquals("id", result);
  }

  @Test
  @DisplayName("resolve: whitespace-only header returns default id")
  void resolve_WhitespaceOnly_ReturnsDefault() {
    when(request.getHeader("Accept-Language")).thenReturn("   ");

    String result = localeResolver.resolve(request);

    assertEquals("id", result);
  }
}

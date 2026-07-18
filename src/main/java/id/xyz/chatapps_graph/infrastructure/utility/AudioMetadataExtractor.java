package id.xyz.chatapps_graph.infrastructure.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AudioMetadataExtractor {
  private static final Pattern DURATION = Pattern.compile("duration=([0-9.]+)");
  private static final Pattern CODEC = Pattern.compile("codec_name=([^\\n]+)");
  private static final Pattern SAMPLE_RATE = Pattern.compile("sample_rate=([0-9]+)");
  private static final Pattern CHANNELS = Pattern.compile("channels=([0-9]+)");

  public Result extract(Path audio) throws IOException, InterruptedException {
    String probe = run(List.of("ffprobe", "-v", "error", "-show_entries",
        "format=duration:stream=codec_name,sample_rate,channels", "-of", "default=noprint_wrappers=1", audio.toString()), null);
    double duration = value(DURATION, probe, 0.0);
    String codec = text(CODEC, probe);
    int sampleRate = (int) value(SAMPLE_RATE, probe, 0);
    short channels = (short) value(CHANNELS, probe, 0);

    ByteArrayOutputStream pcm = new ByteArrayOutputStream();
    run(List.of("ffmpeg", "-v", "error", "-i", audio.toString(), "-f", "s16le", "-ac", "1", "-ar", "100", "-"), pcm);
    byte[] bytes = pcm.toByteArray();
    List<Double> samples = new ArrayList<>();
    for (int i = 0; i + 1 < bytes.length; i += 2) {
      short value = ByteBuffer.wrap(bytes, i, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
      samples.add(Math.min(1.0, Math.abs(value / 32768.0)));
    }
    if (samples.size() > 100) {
      List<Double> bounded = new ArrayList<>(100);
      for (int i = 0; i < 100; i++) bounded.add(samples.get((int) ((long) i * samples.size() / 100)));
      samples = bounded;
    }
    String waveform = "{\"version\":1,\"samples\":" + samples + ",\"normalized\":true}";
    return new Result(Math.round(duration * 1000), codec, sampleRate, channels, waveform);
  }

  private String run(List<String> command, ByteArrayOutputStream output)
      throws IOException, InterruptedException {
    ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
    Process process = builder.start();
    if (output == null) {
      output = new ByteArrayOutputStream();
    }
    process.getInputStream().transferTo(output);
    if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS) || process.exitValue() != 0) {
      process.destroyForcibly();
      throw new IOException("Audio tool failed: " + command.getFirst());
    }
    return output.toString(java.nio.charset.StandardCharsets.UTF_8);
  }

  private double value(Pattern pattern, String value, double fallback) {
    try {
      Matcher matcher = pattern.matcher(value);
      return matcher.find() ? Double.parseDouble(matcher.group(1).trim()) : fallback;
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private String text(Pattern pattern, String value) {
    Matcher matcher = pattern.matcher(value);
    return matcher.find() ? matcher.group(1).trim().toLowerCase(Locale.ROOT) : null;
  }

  public record Result(Long durationMs, String codec, int sampleRate, short channels, String waveform) {}
}

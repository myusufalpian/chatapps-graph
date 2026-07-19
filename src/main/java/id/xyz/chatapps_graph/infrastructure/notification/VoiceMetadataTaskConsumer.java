package id.xyz.chatapps_graph.infrastructure.notification;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.framework.dto.VoiceMetadataTask;
import id.xyz.chatapps_graph.infrastructure.config.rabbitmq.RabbitMQConfig;
import id.xyz.chatapps_graph.infrastructure.utility.AudioMetadataExtractor;
import id.xyz.chatapps_graph.infrastructure.utility.TaskFailureClassifier;
import id.xyz.chatapps_graph.infrastructure.monitoring.MetricsFacade;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceMetadataTaskConsumer {
  private final AttachmentRepository attachmentRepository;
  private final FileStoragePort fileStoragePort;
  private final AudioMetadataExtractor extractor;
  private final RabbitTemplate rabbitTemplate;
  private final MetricsFacade metricsFacade;



  @RabbitListener(queues = RabbitMQConfig.VOICE_METADATA_QUEUE)
  @Transactional
  public void consume(VoiceMetadataTask task) {
    long started = System.nanoTime();
    if (attachmentRepository.findById(task.attachmentId()).isEmpty()
        || attachmentRepository.claimMetadata(task.attachmentId()) != 1) {
      return;
    }
    Path temp = null;
    try {
      Attachment attachment = attachmentRepository.findById(task.attachmentId()).orElseThrow();
      temp = Files.createTempFile("voice-metadata-", ".audio");
      try (InputStream input = fileStoragePort.downloadFile(attachment.getFilePath())) {
        Files.copy(input, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      AudioMetadataExtractor.Result result = extractor.extract(temp);
      attachment.setVoiceDurationMs(result.durationMs());
      attachment.setVoiceCodec(result.codec());
      attachment.setVoiceSampleRate(result.sampleRate());
      attachment.setVoiceChannelCount(result.channels());
      attachment.setVoiceWaveform(result.waveform());
      attachment.setMetadataStatus("COMPLETED");
      attachment.setMetadataProcessedAt(OffsetDateTime.now());
      attachment.setMetadataError(null);
      attachmentRepository.save(attachment);
      if (metricsFacade != null) metricsFacade.recordRabbitMQLatency("voice_metadata", "completed", elapsed(started));
    } catch (Exception exception) {
      log.error("Voice metadata extraction failed for attachment {}", task.attachmentId(), exception);
      if (TaskFailureClassifier.isRetryable(exception) && task.retryCount() < 2) {
        attachmentRepository.findById(task.attachmentId()).ifPresent(attachment -> {
          attachment.setMetadataStatus("PENDING");
          attachmentRepository.save(attachment);
        });
        VoiceMetadataTask retry = new VoiceMetadataTask(task.taskId(), task.correlationId(),
            task.attachmentId(), task.retryCount() + 1, task.createdAt());
        String queue = task.retryCount() == 0
            ? RabbitMQConfig.VOICE_METADATA_RETRY_QUEUE
            : RabbitMQConfig.VOICE_METADATA_RETRY_5M_QUEUE;
        rabbitTemplate.convertAndSend(queue, retry);
        if (metricsFacade != null) metricsFacade.recordRabbitMQLatency("voice_metadata", "retry", elapsed(started));
      } else {
        attachmentRepository.failMetadata(task.attachmentId(), exception.getMessage(), java.time.OffsetDateTime.now());
        if (metricsFacade != null) metricsFacade.recordRabbitMQLatency("voice_metadata", "failed", elapsed(started));
      }
    } finally {
      if (temp != null) {
        try { Files.deleteIfExists(temp); } catch (Exception ignored) { }
      }
    }
  }

  private long elapsed(long started) {
    return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
  }
}

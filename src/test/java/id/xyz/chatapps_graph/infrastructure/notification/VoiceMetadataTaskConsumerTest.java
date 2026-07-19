package id.xyz.chatapps_graph.infrastructure.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.framework.dto.VoiceMetadataTask;
import id.xyz.chatapps_graph.infrastructure.utility.AudioMetadataExtractor;
import java.io.ByteArrayInputStream;
import java.net.SocketTimeoutException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class VoiceMetadataTaskConsumerTest {
  @Mock private AttachmentRepository repository;
  @Mock private FileStoragePort storage;
  @Mock private AudioMetadataExtractor extractor;
  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private id.xyz.chatapps_graph.infrastructure.monitoring.MetricsFacade metricsFacade;

  @Test
  void duplicateTaskIsSkippedWhenClaimFails() throws Exception {

    Attachment attachment = Attachment.builder().attachmentId(1L).metadataStatus("COMPLETED").build();
    when(repository.findById(1L)).thenReturn(Optional.of(attachment));
    when(repository.claimMetadata(1L)).thenReturn(0);

    new VoiceMetadataTaskConsumer(repository, storage, extractor, rabbitTemplate, metricsFacade)
        .consume(VoiceMetadataTask.create(1L));

    verify(storage, never()).downloadFile(any());
    verify(extractor, never()).extract(any());
  }

  @Test
  void failedFirstAttemptIsResetAndRepublished() throws Exception {
    Attachment attachment = Attachment.builder().attachmentId(1L).filePath("voice/a.ogg")
        .metadataStatus("PENDING").build();
    when(repository.findById(1L)).thenReturn(Optional.of(attachment));
    when(repository.claimMetadata(1L)).thenReturn(1);
    when(storage.downloadFile("voice/a.ogg")).thenReturn(new ByteArrayInputStream(new byte[]{1}));
    when(extractor.extract(any())).thenThrow(new SocketTimeoutException("temporary failure"));

    new VoiceMetadataTaskConsumer(repository, storage, extractor, rabbitTemplate, metricsFacade)
        .consume(VoiceMetadataTask.create(1L));

    verify(repository).save(attachment);
    verify(rabbitTemplate).convertAndSend(eq("chat.task.voice-metadata.retry"), any(VoiceMetadataTask.class));
  }
}

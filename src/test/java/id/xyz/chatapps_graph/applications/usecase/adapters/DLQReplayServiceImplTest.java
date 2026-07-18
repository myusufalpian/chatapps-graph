package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.entity.DlqReplayAudit;
import id.xyz.chatapps_graph.domain.entity.ExportJob;
import id.xyz.chatapps_graph.domain.enums.ExportJobStatus;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.DlqReplayAuditRepository;
import id.xyz.chatapps_graph.domain.repository.ExportJobRepository;
import id.xyz.chatapps_graph.framework.dto.ExportTask;
import id.xyz.chatapps_graph.framework.dto.VoiceMetadataTask;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.rabbitmq.RabbitMQConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class DLQReplayServiceImplTest {

  @Mock private ExportJobRepository exportJobRepository;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private DlqReplayAuditRepository auditRepository;
  @Mock private RabbitTemplate rabbitTemplate;

  @Captor private ArgumentCaptor<DlqReplayAudit> auditCaptor;
  @Captor private ArgumentCaptor<ExportTask> exportTaskCaptor;
  @Captor private ArgumentCaptor<VoiceMetadataTask> voiceMetadataTaskCaptor;

  @InjectMocks private DLQReplayServiceImpl replayService;

  @Test
  @DisplayName("replay: should successfully replay failed export job task")
  void testReplayExportJobSuccess() {
    UUID uuid = UUID.randomUUID();
    ExportJob job = ExportJob.builder()
        .exportUuid(uuid)
        .userId(1L)
        .status(ExportJobStatus.FAILED)
        .snapshotAt(OffsetDateTime.now())
        .retryCount(2)
        .build();

    when(exportJobRepository.findByExportUuid(uuid)).thenReturn(Optional.of(job));

    replayService.replay(99L, uuid.toString(), false, "Fix DB Timeout");

    verify(exportJobRepository, times(1)).save(job);
    verify(auditRepository, times(1)).save(auditCaptor.capture());
    assertEquals("Fix DB Timeout", auditCaptor.getValue().getReason());

    verify(rabbitTemplate, times(1)).convertAndSend(
        eq(RabbitMQConfig.CHAT_TASK_EXCHANGE),
        eq(RabbitMQConfig.EXPORT_ROUTING_KEY),
        exportTaskCaptor.capture()
    );
    assertEquals(uuid, exportTaskCaptor.getValue().exportUuid());
  }

  @Test
  @DisplayName("replay: should reset retry count on export job when forced")
  void testReplayExportJobForced() {
    UUID uuid = UUID.randomUUID();
    ExportJob job = ExportJob.builder()
        .exportUuid(uuid)
        .userId(1L)
        .status(ExportJobStatus.FAILED)
        .snapshotAt(OffsetDateTime.now())
        .retryCount(2)
        .build();

    when(exportJobRepository.findByExportUuid(uuid)).thenReturn(Optional.of(job));

    replayService.replay(99L, uuid.toString(), true, "Manual force replay");

    verify(exportJobRepository, times(1)).save(job);
    verify(auditRepository, times(1)).save(auditCaptor.capture());
    assertEquals("Manual force replay", auditCaptor.getValue().getReason());

    verify(rabbitTemplate, times(1)).convertAndSend(
        eq(RabbitMQConfig.CHAT_TASK_EXCHANGE),
        eq(RabbitMQConfig.EXPORT_ROUTING_KEY),
        exportTaskCaptor.capture()
    );
    assertEquals(0, exportTaskCaptor.getValue().retryCount());
  }

  @Test
  @DisplayName("replay: should throw exception when export job is not in FAILED state")
  void testReplayExportJobNotFailed() {
    UUID uuid = UUID.randomUUID();
    ExportJob job = ExportJob.builder()
        .exportUuid(uuid)
        .userId(1L)
        .status(ExportJobStatus.PROCESSING)
        .build();

    when(exportJobRepository.findByExportUuid(uuid)).thenReturn(Optional.of(job));

    assertThrows(GeneralException.class, () ->
        replayService.replay(99L, uuid.toString(), false, "Invalid state"));
  }

  @Test
  @DisplayName("replay: should successfully replay failed attachment metadata task")
  void testReplayAttachmentMetadataSuccess() {
    Attachment attachment = Attachment.builder()
        .attachmentId(10L)
        .attachmentUuid(UUID.randomUUID().toString())
        .metadataStatus("FAILED")
        .build();

    when(attachmentRepository.findById(10L)).thenReturn(Optional.of(attachment));

    replayService.replay(99L, "10", false, "Retry metadata extraction");

    verify(attachmentRepository, times(1)).save(attachment);
    verify(auditRepository, times(1)).save(auditCaptor.capture());
    assertEquals("Retry metadata extraction", auditCaptor.getValue().getReason());

    verify(rabbitTemplate, times(1)).convertAndSend(
        eq(RabbitMQConfig.CHAT_TASK_EXCHANGE),
        eq(RabbitMQConfig.VOICE_METADATA_ROUTING_KEY),
        voiceMetadataTaskCaptor.capture()
    );
    assertEquals(10L, voiceMetadataTaskCaptor.getValue().attachmentId());
  }

  @Test
  @DisplayName("replay: should throw exception when task is not found")
  void testReplayTaskNotFound() {
    String invalidId = "invalid-task-id";
    assertThrows(GeneralException.class, () ->
        replayService.replay(99L, invalidId, false, "Not found"));
  }
}

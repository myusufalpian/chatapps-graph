package id.xyz.chatapps_graph.infrastructure.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.domain.entity.ExportJob;
import id.xyz.chatapps_graph.domain.enums.ExportJobStatus;
import id.xyz.chatapps_graph.domain.repository.ExportJobRepository;
import id.xyz.chatapps_graph.framework.dto.ExportTask;
import id.xyz.chatapps_graph.infrastructure.config.rabbitmq.RabbitMQConfig;
import java.time.OffsetDateTime;
import java.util.List;
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
class ExportJobSchedulerTest {

  @Mock private ExportJobRepository repository;
  @Mock private FileStoragePort fileStoragePort;
  @Mock private RabbitTemplate rabbitTemplate;

  @Captor private ArgumentCaptor<ExportTask> exportTaskCaptor;
  @Captor private ArgumentCaptor<OffsetDateTime> timeCaptor;

  @InjectMocks private ExportJobScheduler scheduler;

  @Test
  @DisplayName("cleanupExpiredExports: should delete files from storage and set status to EXPIRED")
  void testCleanupExpiredExports() {
    ExportJob job = ExportJob.builder()
        .exportJobId(1L)
        .storagePath("exports/1/file.json")
        .status(ExportJobStatus.COMPLETED)
        .build();

    when(repository.findByStatusAndExpiresAtBefore(eq(ExportJobStatus.COMPLETED), timeCaptor.capture()))
        .thenReturn(List.of(job));

    scheduler.cleanupExpiredExports();

    verify(fileStoragePort, times(1)).deleteFile("exports/1/file.json");
    verify(repository, times(1)).save(job);
  }

  @Test
  @DisplayName("recoverStuckJobs: should retry stuck jobs when retry limit is not exceeded")
  void testRecoverStuckJobsRetry() {
    UUID uuid = UUID.randomUUID();
    ExportJob job = ExportJob.builder()
        .exportJobId(2L)
        .exportUuid(uuid)
        .userId(100L)
        .status(ExportJobStatus.PROCESSING)
        .retryCount(0)
        .snapshotAt(OffsetDateTime.now())
        .build();

    when(repository.findByStatusAndLeaseExpiresAtBefore(eq(ExportJobStatus.PROCESSING), timeCaptor.capture()))
        .thenReturn(List.of(job));

    scheduler.recoverStuckJobs();

    verify(repository, times(1)).save(job);
    verify(rabbitTemplate, times(1)).convertAndSend(
        eq(RabbitMQConfig.CHAT_TASK_EXCHANGE),
        eq(RabbitMQConfig.EXPORT_ROUTING_KEY),
        exportTaskCaptor.capture()
    );
    assertEquals(uuid, exportTaskCaptor.getValue().exportUuid());
  }

  @Test
  @DisplayName("recoverStuckJobs: should fail stuck jobs and route to DLQ when retry limit is exceeded")
  void testRecoverStuckJobsExceeded() {
    UUID uuid = UUID.randomUUID();
    ExportJob job = ExportJob.builder()
        .exportJobId(2L)
        .exportUuid(uuid)
        .userId(100L)
        .status(ExportJobStatus.PROCESSING)
        .retryCount(2)
        .snapshotAt(OffsetDateTime.now())
        .build();

    when(repository.findByStatusAndLeaseExpiresAtBefore(eq(ExportJobStatus.PROCESSING), timeCaptor.capture()))
        .thenReturn(List.of(job));

    scheduler.recoverStuckJobs();

    verify(repository, times(1)).save(job);
    verify(rabbitTemplate, times(1)).convertAndSend(
        eq(RabbitMQConfig.CHAT_TASK_EXCHANGE_DLX),
        eq(RabbitMQConfig.EXPORT_ROUTING_KEY),
        exportTaskCaptor.capture()
    );
    assertEquals(uuid, exportTaskCaptor.getValue().exportUuid());
  }
}

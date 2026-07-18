package id.xyz.chatapps_graph.infrastructure.scheduler;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.domain.entity.ExportJob;
import id.xyz.chatapps_graph.domain.enums.ExportJobStatus;
import id.xyz.chatapps_graph.domain.repository.ExportJobRepository;
import id.xyz.chatapps_graph.framework.dto.ExportTask;
import id.xyz.chatapps_graph.infrastructure.config.rabbitmq.RabbitMQConfig;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportJobScheduler {

  private final ExportJobRepository repository;
  private final FileStoragePort fileStoragePort;
  private final RabbitTemplate rabbitTemplate;

  @Scheduled(cron = "${chat.export.cleanup-cron:0 0 * * * *}")
  @Transactional
  public void cleanupExpiredExports() {
    log.info("Starting expired export files cleanup job...");
    try {
      OffsetDateTime now = OffsetDateTime.now();
      List<ExportJob> expiredJobs = repository.findByStatusAndExpiresAtBefore(ExportJobStatus.COMPLETED, now);
      for (ExportJob job : expiredJobs) {
        if (job.getStoragePath() != null) {
          log.info("Deleting expired export file: {}", job.getStoragePath());
          fileStoragePort.deleteFile(job.getStoragePath());
        }
        job.setStatus(ExportJobStatus.EXPIRED);
        repository.save(job);
      }
      log.info("Expired export files cleanup job finished. Total cleaned: {}", expiredJobs.size());
    } catch (Exception e) {
      log.error("Error occurred during expired export files cleanup", e);
    }
  }

  @Scheduled(cron = "${chat.export.recovery-cron:0 */5 * * * *}")
  @Transactional
  public void recoverStuckJobs() {
    log.info("Starting stuck export jobs recovery job...");
    try {
      OffsetDateTime now = OffsetDateTime.now();
      List<ExportJob> stuckJobs = repository.findByStatusAndLeaseExpiresAtBefore(ExportJobStatus.PROCESSING, now);
      for (ExportJob job : stuckJobs) {
        if (job.getRetryCount() < 2) {
          log.info("Recovering stuck export job: {}, retry count: {}", job.getExportUuid(), job.getRetryCount());
          job.setStatus(ExportJobStatus.PENDING);
          job.setRetryCount(job.getRetryCount() + 1);
          job.setLastErrorCode("EXPORT_STUCK_RECOVERED");
          job.setLastErrorMessage("Job was stuck in PROCESSING state beyond lease expiration");
          repository.save(job);

          ExportTask task = new ExportTask(
              java.util.UUID.randomUUID().toString(),
              java.util.UUID.randomUUID().toString(),
              job.getExportUuid(),
              job.getUserId(),
              job.getSnapshotAt(),
              job.getRetryCount(),
              OffsetDateTime.now()
          );
          rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_TASK_EXCHANGE, RabbitMQConfig.EXPORT_ROUTING_KEY, task);
        } else {
          log.warn("Export job {} exceeded maximum retry attempts, failing job", job.getExportUuid());
          job.setStatus(ExportJobStatus.FAILED);
          job.setLastErrorCode("EXPORT_MAX_RETRIES_EXCEEDED");
          job.setLastErrorMessage("Job processing timed out and exceeded maximum retries");
          repository.save(job);

          ExportTask dlqTask = new ExportTask(
              java.util.UUID.randomUUID().toString(),
              java.util.UUID.randomUUID().toString(),
              job.getExportUuid(),
              job.getUserId(),
              job.getSnapshotAt(),
              job.getRetryCount(),
              OffsetDateTime.now()
          );
          rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_TASK_EXCHANGE_DLX, RabbitMQConfig.EXPORT_ROUTING_KEY, dlqTask);
        }
      }
      log.info("Stuck export jobs recovery job finished. Total recovered/failed: {}", stuckJobs.size());
    } catch (Exception e) {
      log.error("Error occurred during stuck export jobs recovery", e);
    }
  }

  @Scheduled(cron = "${chat.export.pending-recovery-cron:30 */5 * * * *}")
  @Transactional
  public void recoverOrphanedPendingJobs() {
    OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(5);
    List<ExportJob> jobs = repository.findByStatusAndCreatedAtBefore(ExportJobStatus.PENDING, threshold);
    for (ExportJob job : jobs) {
      ExportTask task = new ExportTask(
          java.util.UUID.randomUUID().toString(), java.util.UUID.randomUUID().toString(),
          job.getExportUuid(), job.getUserId(), job.getSnapshotAt(), job.getRetryCount(), OffsetDateTime.now());
      rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_TASK_EXCHANGE,
          RabbitMQConfig.EXPORT_ROUTING_KEY, task);
    }
  }
}

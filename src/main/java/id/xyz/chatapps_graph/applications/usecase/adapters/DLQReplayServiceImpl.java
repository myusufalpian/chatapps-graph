package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.DLQReplayService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DLQReplayServiceImpl implements DLQReplayService {

  private final ExportJobRepository exportJobRepository;
  private final AttachmentRepository attachmentRepository;
  private final DlqReplayAuditRepository auditRepository;
  private final RabbitTemplate rabbitTemplate;

  @Override
  @Transactional
  public void replay(Long operatorId, String taskId, boolean force, String reason) {
    // 1. Try treating taskId as UUID and finding in ExportJob
    UUID exportUuid = null;
    try {
      exportUuid = UUID.fromString(taskId);
    } catch (IllegalArgumentException ignored) {
    }

    if (exportUuid != null) {
      Optional<ExportJob> exportJobOpt = exportJobRepository.findByExportUuid(exportUuid);
      if (exportJobOpt.isPresent()) {
        ExportJob job = exportJobOpt.get();
        if (job.getStatus() != ExportJobStatus.FAILED) {
          throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "INVALID_JOB_STATE",
              "Export job is not in FAILED state");
        }

        String oldStatus = job.getStatus().name();
        job.setStatus(ExportJobStatus.PENDING);
        if (force) {
          job.setRetryCount(0);
        }
        exportJobRepository.save(job);

        // Record audit
        DlqReplayAudit audit = DlqReplayAudit.builder()
            .operatorId(operatorId)
            .taskId(taskId)
            .reason(reason)
            .oldStatus(oldStatus)
            .newStatus("PENDING")
            .correlationId(UUID.randomUUID().toString())
            .replayedAt(OffsetDateTime.now())
            .build();
        auditRepository.save(audit);

        // Publish task
        ExportTask task = new ExportTask(
            UUID.randomUUID().toString(),
            audit.getCorrelationId(),
            job.getExportUuid(),
            job.getUserId(),
            job.getSnapshotAt(),
            job.getRetryCount(),
            OffsetDateTime.now()
          );
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_TASK_EXCHANGE, RabbitMQConfig.EXPORT_ROUTING_KEY, task);
        return;
      }
    }

    // 2. Try treating as Attachment
    Optional<Attachment> attachmentOpt = Optional.empty();
    if (exportUuid != null) {
      attachmentOpt = attachmentRepository.findByAttachmentUuid(taskId);
    } else {
      try {
        Long attachmentId = Long.parseLong(taskId);
        attachmentOpt = attachmentRepository.findById(attachmentId);
      } catch (NumberFormatException ignored) {
      }
    }

    if (attachmentOpt.isPresent()) {
      Attachment attachment = attachmentOpt.get();
      if (!"FAILED".equals(attachment.getMetadataStatus())) {
        throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "INVALID_ATTACHMENT_STATE",
            "Attachment metadata status is not FAILED");
      }

      String oldStatus = attachment.getMetadataStatus();
      attachment.setMetadataStatus("PENDING");
      attachmentRepository.save(attachment);

      // Record audit
      DlqReplayAudit audit = DlqReplayAudit.builder()
          .operatorId(operatorId)
          .taskId(taskId)
          .reason(reason)
          .oldStatus(oldStatus)
          .newStatus("PENDING")
          .correlationId(UUID.randomUUID().toString())
          .replayedAt(OffsetDateTime.now())
          .build();
      auditRepository.save(audit);

      // Publish task
      VoiceMetadataTask task = new VoiceMetadataTask(
          UUID.randomUUID().toString(),
          audit.getCorrelationId(),
          attachment.getAttachmentId(),
          0,
          OffsetDateTime.now()
      );
      rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_TASK_EXCHANGE, RabbitMQConfig.VOICE_METADATA_ROUTING_KEY, task);
      return;
    }

    throw new GeneralException(HttpStatus.NOT_FOUND.value(), "TASK_NOT_FOUND",
        "Could not find matching task for ID: " + taskId);
  }
}

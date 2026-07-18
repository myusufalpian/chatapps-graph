package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.applications.usecase.ExportService;
import id.xyz.chatapps_graph.domain.entity.ExportJob;
import id.xyz.chatapps_graph.domain.enums.ExportJobStatus;
import id.xyz.chatapps_graph.domain.repository.ExportJobRepository;
import id.xyz.chatapps_graph.framework.dto.ExportJobResponse;
import id.xyz.chatapps_graph.framework.dto.ExportTask;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.rabbitmq.RabbitMQConfig;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {
  private final ExportJobRepository repository;
  private final RabbitTemplate rabbitTemplate;
  private final JdbcTemplate jdbcTemplate;
  private final FileStoragePort storage;

  @Override
  @Transactional
  public ExportJobResponse request(Long userId) {
    OffsetDateTime snapshot = jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP", OffsetDateTime.class);
    ExportJob job = ExportJob.builder().userId(userId).status(ExportJobStatus.PENDING)
        .format("JSON").snapshotAt(snapshot).expiresAt(snapshot.plusHours(24)).retryCount(0).build();
    try {
      job = repository.saveAndFlush(job);
    } catch (DataIntegrityViolationException exception) {
      throw new GeneralException(HttpStatus.CONFLICT.value(), "EXPORT_ALREADY_ACTIVE",
          "An export is already active for this user");
    }
    rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_TASK_EXCHANGE,
        RabbitMQConfig.EXPORT_ROUTING_KEY, ExportTask.create(job.getExportUuid(), userId, snapshot));
    return response(job);
  }

  @Override
  @Transactional(readOnly = true)
  public ExportJobResponse status(Long userId, UUID exportUuid) {
    return response(findOwned(userId, exportUuid));
  }

  @Override
  @Transactional(readOnly = true)
  public String downloadUrl(Long userId, UUID exportUuid) {
    ExportJob job = findOwned(userId, exportUuid);
    if (job.getStatus() != ExportJobStatus.COMPLETED || job.getExpiresAt().isBefore(OffsetDateTime.now())) {
      throw new GeneralException(HttpStatus.GONE.value(), "EXPORT_EXPIRED", "Export is no longer available");
    }
    return storage.createPresignedUrl(job.getStoragePath(), Duration.ofMinutes(10));
  }

  private ExportJob findOwned(Long userId, UUID uuid) {
    ExportJob job = repository.findByExportUuid(uuid).orElseThrow(() ->
        new GeneralException(HttpStatus.NOT_FOUND.value(), "EXPORT_NOT_FOUND", "Export not found"));
    if (!job.getUserId().equals(userId)) {
      throw new GeneralException(HttpStatus.FORBIDDEN.value(), "EXPORT_FORBIDDEN", "Export is not owned by user");
    }
    return job;
  }

  private ExportJobResponse response(ExportJob job) {
    return new ExportJobResponse(job.getExportUuid(), job.getStatus(), job.getExpiresAt(), job.getSnapshotAt(), null);
  }
}

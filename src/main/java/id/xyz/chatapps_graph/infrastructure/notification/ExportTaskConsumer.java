package id.xyz.chatapps_graph.infrastructure.notification;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.domain.entity.ExportJob;
import id.xyz.chatapps_graph.domain.enums.ExportJobStatus;
import id.xyz.chatapps_graph.domain.repository.ExportJobRepository;
import id.xyz.chatapps_graph.framework.dto.ExportTask;
import id.xyz.chatapps_graph.infrastructure.config.rabbitmq.RabbitMQConfig;
import id.xyz.chatapps_graph.infrastructure.monitoring.MetricsFacade;
import id.xyz.chatapps_graph.infrastructure.utility.TaskFailureClassifier;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskConsumer {
  private final ExportJobRepository repository;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final FileStoragePort storage;
  private final RabbitTemplate rabbitTemplate;
  private final MetricsFacade metricsFacade;

  @RabbitListener(queues = RabbitMQConfig.EXPORT_QUEUE)
  @Transactional
  public void consume(ExportTask task) {
    long started = System.nanoTime();
    if (repository.claim(task.exportUuid(), OffsetDateTime.now().plusMinutes(15)) != 1) return;
    ExportJob job = repository.findByExportUuid(task.exportUuid()).orElseThrow();
    Path file = null;
    try {
      file = Files.createTempFile("export-", ".json");
      try (OutputStream output = Files.newOutputStream(file);
           JsonGenerator json = objectMapper.getFactory().createGenerator(output)) {
        json.writeStartObject();
        json.writeNumberField("schemaVersion", 1);
        json.writeStringField("snapshotAt", task.snapshotAt().toString());
        writeSingleObject(json, "user", "SELECT user_uuid, user_phone, user_mail, user_full_name, profile_photo "
            + "FROM users WHERE user_id = ? AND created_at <= ?", task.userId(), task.snapshotAt());
        writeArray(json, "conversations", "SELECT c.conversation_uuid, c.conversation_type, c.created_at "
            + "FROM conversation c JOIN conversation_participant p ON p.conversation_id = c.conversation_id "
            + "WHERE p.user_id = ? AND c.created_at <= ?", task.userId(), task.snapshotAt());
        writeArray(json, "messages", "SELECT m.message_uuid, m.conversation_id, m.sender_id, m.message_type, "
            + "m.content, m.attachment_id, m.message_status, m.created_at FROM message m "
            + "JOIN conversation_participant p ON p.conversation_id = m.conversation_id "
            + "WHERE p.user_id = ? AND m.created_at <= ? AND m.deleted_at IS NULL ORDER BY m.created_at, m.message_id",
            task.userId(), task.snapshotAt());
        writeArray(json, "attachments", "SELECT DISTINCT a.attachment_uuid, a.file_name, a.file_size, "
            + "a.content_type, a.attachment_type, a.file_path, a.created_at FROM attachment a "
            + "JOIN message m ON m.attachment_id = a.attachment_id JOIN conversation_participant p "
            + "ON p.conversation_id = m.conversation_id WHERE p.user_id = ? AND a.created_at <= ?",
            task.userId(), task.snapshotAt());
        json.writeEndObject();
      }
      String path = "exports/" + task.userId() + "/" + task.exportUuid() + ".json";
      try (InputStream input = Files.newInputStream(file)) {
        storage.uploadFile(path, input, "application/json", Files.size(file));
      }
      job.setStoragePath(path);
      job.setFileSize(Files.size(file));
      job.setStatus(ExportJobStatus.COMPLETED);
      job.setCompletedAt(OffsetDateTime.now());
      repository.save(job);
      metricsFacade.incrementExports("completed");
      metricsFacade.recordRabbitMQLatency("export", "completed", elapsed(started));
    } catch (Exception exception) {
      if (TaskFailureClassifier.isRetryable(exception) && task.retryCount() < 2) {
        job.setStatus(ExportJobStatus.PENDING);
        job.setRetryCount(task.retryCount() + 1);
        job.setLastErrorCode("EXPORT_RETRYABLE_FAILURE");
        job.setLastErrorMessage(exception.getMessage());
        repository.save(job);
        ExportTask retry = new ExportTask(task.taskId(), task.correlationId(), task.exportUuid(),
            task.userId(), task.snapshotAt(), task.retryCount() + 1, task.createdAt());
        String queue = task.retryCount() == 0
            ? RabbitMQConfig.EXPORT_RETRY_QUEUE
            : RabbitMQConfig.EXPORT_RETRY_5M_QUEUE;
        rabbitTemplate.convertAndSend(queue, retry);
        metricsFacade.incrementExports("retry");
        metricsFacade.recordRabbitMQLatency("export", "retry", elapsed(started));
      } else {
        job.setStatus(ExportJobStatus.FAILED);
        job.setLastErrorCode("EXPORT_FAILED");
        job.setLastErrorMessage(exception.getMessage());
        repository.save(job);
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_TASK_EXCHANGE_DLX,
            RabbitMQConfig.EXPORT_ROUTING_KEY, task);
        metricsFacade.incrementExports("failed");
        metricsFacade.recordRabbitMQLatency("export", "failed", elapsed(started));
      }
      log.error("Export generation failed for {}", task.exportUuid(), exception);
    } finally {
      if (file != null) try { Files.deleteIfExists(file); } catch (Exception ignored) { }
    }
  }

  private long elapsed(long started) {
    return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
  }

  private void writeSingleObject(JsonGenerator json, String field, String sql, Object... args) throws Exception {
    json.writeFieldName(field);
    jdbcTemplate.query(sql, args, result -> {
      try {
        if (result.next()) writeCurrent(json, result);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return null;
    });
  }

  private void writeArray(JsonGenerator json, String field, String sql, Object... args) throws Exception {
    json.writeArrayFieldStart(field);
    jdbcTemplate.query(sql, args, result -> {
      try {
        while (result.next()) writeCurrent(json, result);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return null;
    });
    json.writeEndArray();
  }


  private void writeCurrent(JsonGenerator json, java.sql.ResultSet result) throws java.sql.SQLException, java.io.IOException {
    json.writeStartObject();
    java.sql.ResultSetMetaData meta = result.getMetaData();
    for (int i = 1; i <= meta.getColumnCount(); i++) {
      String name = meta.getColumnLabel(i);
      Object value = result.getObject(i);
      if (value == null) json.writeNullField(name);
      else {
        json.writeFieldName(name);
        json.writeObject(value);
      }
    }
    json.writeEndObject();
  }
}

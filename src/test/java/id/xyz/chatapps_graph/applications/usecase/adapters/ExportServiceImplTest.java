package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.domain.entity.ExportJob;
import id.xyz.chatapps_graph.domain.enums.ExportJobStatus;
import id.xyz.chatapps_graph.domain.repository.ExportJobRepository;
import id.xyz.chatapps_graph.framework.dto.ExportTask;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {
  @Mock private ExportJobRepository repository;
  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private FileStoragePort storage;

  @Captor private ArgumentCaptor<ExportJob> exportJobCaptor;
  @Captor private ArgumentCaptor<ExportTask> exportTaskCaptor;

  private ExportServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ExportServiceImpl(repository, rabbitTemplate, jdbcTemplate, storage);
  }

  @Test
  void requestCreatesPendingJobAndPublishesTask() {
    OffsetDateTime now = OffsetDateTime.now();
    UUID uuid = UUID.randomUUID();
    when(jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP", OffsetDateTime.class)).thenReturn(now);
    when(repository.saveAndFlush(exportJobCaptor.capture())).thenAnswer(invocation -> {
      ExportJob job = exportJobCaptor.getValue();
      job.setExportUuid(uuid);
      return job;
    });

    var result = service.request(7L);

    assertEquals(uuid, result.exportUuid());
    assertEquals(ExportJobStatus.PENDING, result.status());
    verify(rabbitTemplate).convertAndSend(
        eq("chat.task.exchange"),
        eq("export"),
        exportTaskCaptor.capture()
    );
    assertEquals(uuid, exportTaskCaptor.getValue().exportUuid());
  }

  @Test
  void downloadRejectsNonOwner() {
    UUID uuid = UUID.randomUUID();
    ExportJob job = ExportJob.builder().exportUuid(uuid).userId(10L)
        .status(ExportJobStatus.COMPLETED).expiresAt(OffsetDateTime.now().plusHours(1))
        .storagePath("exports/file.json").build();
    when(repository.findByExportUuid(uuid)).thenReturn(Optional.of(job));

    GeneralException exception = assertThrows(GeneralException.class,
        () -> service.downloadUrl(11L, uuid));

    assertEquals(403, exception.getHttpCode());
  }
}

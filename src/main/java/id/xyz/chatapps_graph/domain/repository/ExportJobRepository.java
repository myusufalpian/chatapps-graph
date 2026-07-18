package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.ExportJob;
import id.xyz.chatapps_graph.domain.enums.ExportJobStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {
  Optional<ExportJob> findByExportUuid(UUID exportUuid);

  boolean existsByUserIdAndStatusIn(Long userId, Iterable<ExportJobStatus> statuses);

  @Modifying
  @Query("UPDATE ExportJob e SET e.status = 'PROCESSING', e.startedAt = CURRENT_TIMESTAMP, "
      + "e.processingStartedAt = CURRENT_TIMESTAMP, e.leaseExpiresAt = :leaseExpiresAt "
      + "WHERE e.exportUuid = :uuid AND e.status = 'PENDING'")
  int claim(@Param("uuid") UUID exportUuid, @Param("leaseExpiresAt") OffsetDateTime leaseExpiresAt);

  @Modifying
  @Query("UPDATE ExportJob e SET e.status = 'EXPIRED' "
      + "WHERE e.status = 'COMPLETED' AND e.expiresAt < :now")
  int expireCompleted(@Param("now") OffsetDateTime now);
  java.util.List<ExportJob> findByStatusAndLeaseExpiresAtBefore(ExportJobStatus status, OffsetDateTime now);

  java.util.List<ExportJob> findByStatusAndExpiresAtBefore(ExportJobStatus status, OffsetDateTime now);
  java.util.List<ExportJob> findByStatusAndCreatedAtBefore(ExportJobStatus status, OffsetDateTime time);
}

package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  @Modifying
  @Query("UPDATE Attachment a SET a.metadataStatus = 'PROCESSING' "
      + "WHERE a.attachmentId = :id AND a.metadataStatus = 'PENDING'")
  int claimMetadata(@Param("id") Long attachmentId);

  @Modifying
  @Query("UPDATE Attachment a SET a.metadataStatus = 'FAILED', a.metadataError = :error, "
      + "a.metadataProcessedAt = :now WHERE a.attachmentId = :id")
  int failMetadata(@Param("id") Long attachmentId, @Param("error") String error, @Param("now") java.time.OffsetDateTime now);

  java.util.Optional<Attachment> findByAttachmentUuid(String attachmentUuid);
}


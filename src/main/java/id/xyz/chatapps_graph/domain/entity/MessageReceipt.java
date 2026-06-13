package id.xyz.chatapps_graph.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "message_receipt")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReceipt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "receipt_id")
  private Long receiptId;

  @Column(name = "message_id", nullable = false)
  private Long messageId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "status", nullable = false)
  private Integer status;

  @Column(name = "is_deleted_for_me", nullable = false)
  private Boolean isDeletedForMe;

  @Column(name = "read_at")
  private OffsetDateTime readAt;
}

package id.xyz.chatapps_graph.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_Detail")
@Getter
@Setter
public class ChatDetail extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_chat_detail_id")
  private Long userChatDetailId;

  @Column(name = "user_chat_detail_uuid", length = 64, columnDefinition = "varchar(64) default gen_random_uuid()")
  private String userChatDetailUuid;

  @Column(name = "chat_id")
  private Long chatId;

  @Column(name = "chat_desc", columnDefinition = "TEXT")
  private String chatDesc;

  @Column(name = "chat_status", columnDefinition = "integer default 0")
  private Integer chatStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_attachment_id")
  private Attachment attachment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id")
  private Group group;

  @Column(name = "is_favorite", columnDefinition = "integer default 0")
  private Integer isFavorite;
}

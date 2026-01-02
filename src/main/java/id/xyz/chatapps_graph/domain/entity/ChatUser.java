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
@Table(name = "chat_user")
@Getter
@Setter
public class ChatUser extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "chat_user_id")
  private Long chatUserId;

  @Column(name = "chat_user_uuid", length = 64, columnDefinition = "varchar(64) default gen_random_uuid()")
  private String chatUserUuid;

  @Column(name = "chat_id")
  private Long chatId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_to_user_id")
  private User chatToUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_to_group_id")
  private Group chatToGroup;

  @Column(name = "chat_type", length = 50)
  private String chatType;

  @Column(name = "is_active", columnDefinition = "integer default 0")
  private Integer isActive;
}

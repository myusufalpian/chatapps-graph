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
@Table(name = "user_linked_account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLinkedAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "linked_account_id")
  private Long linkedAccountId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "provider", nullable = false, length = 20)
  private String provider;

  @Column(name = "provider_sub", nullable = false, length = 255)
  private String providerSub;

  @Column(name = "provider_email", length = 100)
  private String providerEmail;

  @Column(name = "linked_at")
  private OffsetDateTime linkedAt;
}

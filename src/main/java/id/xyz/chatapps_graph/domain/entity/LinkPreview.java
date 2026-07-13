package id.xyz.chatapps_graph.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "link_preview")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkPreview {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "preview_id")
  private Long previewId;

  @Column(nullable = false, length = 2048)
  private String url;

  @Column(name = "url_hash", nullable = false, length = 64)
  private String urlHash;

  @Column(length = 500)
  private String title;

  @Column(length = 2000)
  private String description;

  @Column(name = "image_url", length = 2048)
  private String imageUrl;

  @Column(name = "site_name", length = 200)
  private String siteName;

  @Column(name = "fetched_at", nullable = false)
  private LocalDateTime fetchedAt;
}

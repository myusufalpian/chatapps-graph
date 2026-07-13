package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.LinkPreview;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkPreviewRepository extends JpaRepository<LinkPreview, Long> {

  Optional<LinkPreview> findByUrl(String url);

  Optional<LinkPreview> findByUrlHash(String urlHash);
}

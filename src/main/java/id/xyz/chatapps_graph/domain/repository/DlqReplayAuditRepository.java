package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.DlqReplayAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DlqReplayAuditRepository extends JpaRepository<DlqReplayAudit, Long> {
}

package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.GroupMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

  @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.user WHERE gm.group.groupId = :groupId AND gm.user.userId = :userId AND gm.isActive = :isActive")
  Optional<GroupMember> findByGroupIdAndUserIdAndIsActive(Long groupId, Long userId, Integer isActive);

  @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.user WHERE gm.group.groupId = :groupId AND gm.isActive = :isActive")
  List<GroupMember> findAllByGroupIdWithUser(Long groupId, Integer isActive);

  @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.groupId = :groupId AND gm.isActive = :isActive")
  int countActiveMembers(Long groupId, Integer isActive);

  @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.groupId = :groupId AND gm.memberType = :memberType AND gm.isActive = :isActive")
  int countByRoleAndActive(Long groupId, String memberType, Integer isActive);
}

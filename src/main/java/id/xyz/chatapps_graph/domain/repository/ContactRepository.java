package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.Contact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

  @EntityGraph(attributePaths = {"contactUser", "contactUser.about"})
  List<Contact> findByOwnerUserId(Long ownerUserId);

  Optional<Contact> findByOwnerUserIdAndContactUserUserId(Long ownerUserId, Long contactUserId);

  @EntityGraph(attributePaths = {"contactUser", "contactUser.about"})
  Optional<Contact> findByOwnerUserIdAndContactUserUserUuid(Long ownerUserId, String contactUserUuid);

  @EntityGraph(attributePaths = {"contactUser", "contactUser.about"})
  List<Contact> findByOwnerUserIdAndContactUserUserIdIn(Long ownerUserId, List<Long> contactUserIds);

  boolean existsByOwnerUserIdAndContactUserUserId(Long ownerUserId, Long contactUserId);
}

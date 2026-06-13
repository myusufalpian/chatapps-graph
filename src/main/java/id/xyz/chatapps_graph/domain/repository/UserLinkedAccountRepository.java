package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.UserLinkedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLinkedAccountRepository extends JpaRepository<UserLinkedAccount, Long> {

  Optional<UserLinkedAccount> findByProviderAndProviderSub(String provider, String providerSub);

  List<UserLinkedAccount> findAllByUserId(Long userId);
}

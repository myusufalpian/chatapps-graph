package id.xyz.chatapps_graph.domain.repository;

import id.xyz.chatapps_graph.domain.entity.UserOtp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserOtpRepository extends JpaRepository<UserOtp, Long> {
  Optional<UserOtp> findTopByUserIdAndPurposeAndOtpStatusOrderByCreatedAtDesc(
      Long userId, String purpose, Integer otpStatus);
}

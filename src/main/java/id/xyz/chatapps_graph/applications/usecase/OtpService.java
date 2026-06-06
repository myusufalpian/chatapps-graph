package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.enums.OtpPurpose;

public interface OtpService {
  boolean isRateLimited(String phone);
  String generateAndSaveOtp(String phone, OtpPurpose purpose);
  boolean validateOtp(String phone, String inputOtp, OtpPurpose purpose);
}

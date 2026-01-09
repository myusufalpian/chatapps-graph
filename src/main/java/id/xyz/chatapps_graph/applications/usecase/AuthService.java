package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.framework.dto.UserSignInRequestDTO;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;

public interface AuthService {
  String getTokenAuth(UserSignInRequestDTO signInRequest) throws GeneralException;

}

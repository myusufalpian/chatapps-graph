package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.repository.ContactRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServicePrivacyTest {

  @Mock private UserRepository userRepository;
  @Mock private ContactRepository contactRepository;

  @InjectMocks private ProfileServiceImpl profileService;

  private static final Long USER_ID = 1L;

  @Test
  @DisplayName("updatePrivacySetting: sets hideReadReceipt to true")
  void updatePrivacy_SetsHideReadReceiptTrue() {
    User user = User.builder().userId(USER_ID).hideReadReceipt(false).build();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    profileService.updatePrivacySetting(USER_ID, true);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertEquals(true, captor.getValue().getHideReadReceipt());
  }

  @Test
  @DisplayName("updatePrivacySetting: sets hideReadReceipt to false")
  void updatePrivacy_SetsHideReadReceiptFalse() {
    User user = User.builder().userId(USER_ID).hideReadReceipt(true).build();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    profileService.updatePrivacySetting(USER_ID, false);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertEquals(false, captor.getValue().getHideReadReceipt());
  }

  @Test
  @DisplayName("updatePrivacySetting: user not found throws 404")
  void updatePrivacy_UserNotFound_Throws404() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class,
        () -> profileService.updatePrivacySetting(99L, true));

    assertEquals(404, ex.getHttpCode());
    assertEquals("USER_NOT_FOUND", ex.getKey());
  }
}

package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.domain.entity.Contact;
import id.xyz.chatapps_graph.domain.entity.MstAbout;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.repository.ContactRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private ContactRepository contactRepository;
  @Captor private ArgumentCaptor<List<Contact>> contactListCaptor;

  @InjectMocks private ProfileServiceImpl profileService;

  // --- getMyProfile ---

  @Test
  void getMyProfile_ActiveUser_ReturnsUser() {
    User user = buildUser(1L, "uuid-owner", "+6281234567890", "John Doe");
    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(user));

    User result = profileService.getMyProfile("+6281234567890");

    assertEquals("uuid-owner", result.getUserUuid());
    assertEquals("+6281234567890", result.getUserPhone());
    assertEquals("John Doe", result.getUserFullName());
    verify(userRepository, times(1)).findUserByUserPhoneAndUserStatus("+6281234567890", 0);
  }

  @Test
  void getMyProfile_UserNotFound_ThrowsGeneralException() {
    when(userRepository.findUserByUserPhoneAndUserStatus("+6280000000000", 0))
        .thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class,
        () -> profileService.getMyProfile("+6280000000000"));

    assertEquals(404, ex.getHttpCode());
    assertEquals("USER_NOT_FOUND", ex.getKey());
  }

  // --- getUserProfile ---

  @Test
  void getUserProfile_Found_ReturnsUser() {
    User user = buildUser(2L, "uuid-123", "+6289999999999", "Jane Doe");
    when(userRepository.findUserByUserUuidAndUserStatus("uuid-123", 0))
        .thenReturn(Optional.of(user));

    User result = profileService.getUserProfile("uuid-123");

    assertEquals("uuid-123", result.getUserUuid());
    assertEquals("Jane Doe", result.getUserFullName());
    verify(userRepository, times(1)).findUserByUserUuidAndUserStatus("uuid-123", 0);
  }

  @Test
  void getUserProfile_NotFound_ThrowsGeneralException() {
    when(userRepository.findUserByUserUuidAndUserStatus("uuid-not-exist", 0))
        .thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class,
        () -> profileService.getUserProfile("uuid-not-exist"));

    assertEquals(404, ex.getHttpCode());
    assertEquals("USER_NOT_FOUND", ex.getKey());
  }

  // --- updateMyFullName ---

  @Test
  void updateMyFullName_Success_UpdatesAndReturns() {
    User user = buildUser(1L, "uuid-owner", "+6281234567890", "Old Name");
    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    User result = profileService.updateMyFullName("+6281234567890", "New Name");

    assertEquals("New Name", result.getUserFullName());
    verify(userRepository, times(1)).save(user);
  }

  // --- updateMyStatus ---

  @Test
  void updateMyStatus_Success_UpdatesAboutDesc() {
    MstAbout about = new MstAbout();
    about.setAboutDesc("Available");
    User user = buildUser(1L, "uuid-owner", "+6281234567890", "John Doe");
    user.setAbout(about);
    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    User result = profileService.updateMyStatus("+6281234567890", "Busy");

    assertEquals("Busy", result.getAbout().getAboutDesc());
    verify(userRepository, times(1)).save(user);
  }

  // --- updateMyProfilePhoto ---

  @Test
  void updateMyProfilePhoto_Success_UpdatesPhoto() {
    User user = buildUser(1L, "uuid-owner", "+6281234567890", "John Doe");
    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    User result = profileService.updateMyProfilePhoto("+6281234567890", "https://minio/photo.jpg");

    assertEquals("https://minio/photo.jpg", result.getProfilePhoto());
    verify(userRepository, times(1)).save(user);
  }

  // --- updateContactDisplayName ---

  @Test
  void updateContactDisplayName_Found_UpdatesName() {
    User owner = buildUser(1L, "uuid-owner", "+6281234567890", "John Doe");
    User contactUser = buildUser(2L, "uuid-contact", "+6289999999999", "Jane Doe");
    Contact contact = buildContact(10L, "contact-uuid-1", owner, contactUser, "Jane");

    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(owner));
    when(contactRepository.findByOwnerUserIdAndContactUserUserUuid(1L, "uuid-contact"))
        .thenReturn(Optional.of(contact));
    when(contactRepository.save(contact)).thenReturn(contact);

    Contact result = profileService.updateContactDisplayName("+6281234567890", "uuid-contact", "Custom Name");

    assertEquals("Custom Name", result.getDisplayName());
    verify(contactRepository, times(1)).save(contact);
  }

  @Test
  void updateContactDisplayName_NotFound_Throws() {
    User owner = buildUser(1L, "uuid-owner", "+6281234567890", "John Doe");
    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(owner));
    when(contactRepository.findByOwnerUserIdAndContactUserUserUuid(1L, "uuid-unknown"))
        .thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class,
        () -> profileService.updateContactDisplayName("+6281234567890", "uuid-unknown", "Name"));

    assertEquals(404, ex.getHttpCode());
    assertEquals("CONTACT_NOT_FOUND", ex.getKey());
  }

  // --- syncContacts ---

  @SuppressWarnings("unchecked")
  @Test
  void syncContacts_NewContact_CreatesContact() {
    User owner = buildUser(1L, "uuid-owner", "+6281234567890", "John Doe");
    User target = buildUser(2L, "uuid-target", "+6281111111111", "Target User");

    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(owner));
    when(userRepository.findByUserPhoneInAndUserStatus(List.of("+6281111111111"), 0))
        .thenReturn(List.of(target));
    when(contactRepository.findByOwnerUserIdAndContactUserUserIdIn(1L, List.of(2L)))
        .thenReturn(List.of());

    // Stub saveAll to return saved contacts with uuid set
    when(contactRepository.saveAll(contactListCaptor.capture())).thenAnswer(invocation -> {
      List<Contact> input = invocation.getArgument(0);
      input.forEach(c -> c.setContactUuid("new-contact-uuid"));
      return input;
    });

    List<Contact> result = profileService.syncContacts("+6281234567890", List.of("+6281111111111"));

    assertEquals(1, result.size());
    assertEquals("new-contact-uuid", result.get(0).getContactUuid());

    List<Contact> captured = contactListCaptor.getValue();
    assertEquals(1, captured.size());
    assertEquals("Target User", captured.get(0).getDisplayName());
    assertEquals(2L, captured.get(0).getContactUser().getUserId());
    assertEquals(1L, captured.get(0).getOwner().getUserId());
  }

  @Test
  void syncContacts_ExistingContact_Skips() {
    User owner = buildUser(1L, "uuid-owner", "+6281234567890", "John Doe");
    User target = buildUser(2L, "uuid-target", "+6281111111111", "Target User");
    Contact existingContact = buildContact(10L, "existing-uuid", owner, target, "My Friend");

    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(owner));
    when(userRepository.findByUserPhoneInAndUserStatus(List.of("+6281111111111"), 0))
        .thenReturn(List.of(target));
    when(contactRepository.findByOwnerUserIdAndContactUserUserIdIn(1L, List.of(2L)))
        .thenReturn(List.of(existingContact));

    List<Contact> result = profileService.syncContacts("+6281234567890", List.of("+6281111111111"));

    assertEquals(1, result.size());
    assertEquals("existing-uuid", result.get(0).getContactUuid());
    assertEquals("My Friend", result.get(0).getDisplayName());
    verify(contactRepository, times(1)).findByOwnerUserIdAndContactUserUserIdIn(1L, List.of(2L));
    verifyNoMoreInteractions(contactRepository);
  }

  @Test
  void syncContacts_PhoneNotRegistered_Skipped() {
    User owner = buildUser(1L, "uuid-owner", "+6281234567890", "John Doe");

    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(owner));
    when(userRepository.findByUserPhoneInAndUserStatus(List.of("+6280000000000"), 0))
        .thenReturn(List.of());

    List<Contact> result = profileService.syncContacts("+6281234567890", List.of("+6280000000000"));

    assertTrue(result.isEmpty());
    verifyNoInteractions(contactRepository);
  }

  @Test
  void syncContacts_OwnPhone_Skipped() {
    User owner = buildUser(1L, "uuid-owner", "+6281234567890", "John Doe");

    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(owner));
    when(userRepository.findByUserPhoneInAndUserStatus(List.of("+6281234567890"), 0))
        .thenReturn(List.of(owner));

    List<Contact> result = profileService.syncContacts("+6281234567890", List.of("+6281234567890"));

    assertTrue(result.isEmpty());
    verifyNoInteractions(contactRepository);
  }

  // --- getContacts ---

  @Test
  void getContacts_ReturnsList() {
    User owner = buildUser(1L, "uuid-owner", "+6281234567890", "John Doe");
    User contact1User = buildUser(2L, "uuid-c1", "+6281111111111", "Contact One");
    User contact2User = buildUser(3L, "uuid-c2", "+6282222222222", "Contact Two");
    Contact c1 = buildContact(10L, "cuuid-1", owner, contact1User, "Contact One");
    Contact c2 = buildContact(11L, "cuuid-2", owner, contact2User, "Contact Two");

    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", 0))
        .thenReturn(Optional.of(owner));
    when(contactRepository.findByOwnerUserId(1L)).thenReturn(List.of(c1, c2));

    List<Contact> result = profileService.getContacts("+6281234567890");

    assertEquals(2, result.size());
    assertEquals("cuuid-1", result.get(0).getContactUuid());
    assertEquals("cuuid-2", result.get(1).getContactUuid());
    verify(contactRepository, times(1)).findByOwnerUserId(1L);
  }

  // --- helpers ---

  private User buildUser(Long id, String uuid, String phone, String fullName) {
    User user = new User();
    user.setUserId(id);
    user.setUserUuid(uuid);
    user.setUserPhone(phone);
    user.setUserFullName(fullName);
    user.setUserStatus(0);
    return user;
  }

  private Contact buildContact(Long id, String uuid, User owner, User contactUser, String displayName) {
    Contact contact = new Contact();
    contact.setContactId(id);
    contact.setContactUuid(uuid);
    contact.setOwner(owner);
    contact.setContactUser(contactUser);
    contact.setDisplayName(displayName);
    return contact;
  }
}

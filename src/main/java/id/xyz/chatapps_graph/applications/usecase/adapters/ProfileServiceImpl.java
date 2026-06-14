package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.ProfileService;
import id.xyz.chatapps_graph.domain.entity.Contact;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.PresenceStatus;
import id.xyz.chatapps_graph.domain.enums.PresenceVisibility;
import id.xyz.chatapps_graph.domain.repository.ContactRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.PresenceResponse;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

  private static final int MAX_FULL_NAME_LENGTH = 255;
  private static final int MAX_ABOUT_DESC_LENGTH = 500;
  private static final int MAX_PHOTO_URL_LENGTH = 2048;
  private static final int MAX_SYNC_PHONE_NUMBERS = 500;
  private static final long ONLINE_THRESHOLD_MINUTES = 2;

  private final UserRepository userRepository;
  private final ContactRepository contactRepository;

  @Override
  @Transactional(readOnly = true)
  public User getMyProfile(String principal) {
    return findActiveUserByPhone(principal);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserProfile(String userUuid) {
    return userRepository.findUserByUserUuidAndUserStatus(userUuid, StatusConstants.ACTIVE)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", "User not found"));
  }

  @Override
  @Transactional
  public User updateMyFullName(String principal, String fullName) {
    validateLength(fullName, MAX_FULL_NAME_LENGTH, "fullName");
    User user = findActiveUserByPhone(principal);
    user.setUserFullName(fullName);
    user.setUpdatedAt(OffsetDateTime.now());
    return userRepository.save(user);
  }

  @Override
  @Transactional
  public User updateMyStatus(String principal, String aboutDesc) {
    validateLength(aboutDesc, MAX_ABOUT_DESC_LENGTH, "aboutDesc");
    User user = findActiveUserByPhone(principal);
    if (user.getAbout() != null) {
      user.getAbout().setAboutDesc(aboutDesc);
    }
    user.setUpdatedAt(OffsetDateTime.now());
    return userRepository.save(user);
  }

  @Override
  @Transactional
  public User updateMyProfilePhoto(String principal, String photoUrl) {
    validateLength(photoUrl, MAX_PHOTO_URL_LENGTH, "photoUrl");
    User user = findActiveUserByPhone(principal);
    user.setProfilePhoto(photoUrl);
    user.setUpdatedAt(OffsetDateTime.now());
    return userRepository.save(user);
  }

  @Override
  @Transactional
  public Contact updateContactDisplayName(String principal, String contactUserUuid, String displayName) {
    validateLength(displayName, MAX_FULL_NAME_LENGTH, "displayName");
    User owner = findActiveUserByPhone(principal);
    Contact contact = contactRepository.findByOwnerUserIdAndContactUserUserUuid(owner.getUserId(), contactUserUuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "CONTACT_NOT_FOUND", "Contact not found"));
    contact.setDisplayName(displayName);
    contact.setUpdatedAt(OffsetDateTime.now());
    return contactRepository.save(contact);
  }

  @Override
  @Transactional
  public List<Contact> syncContacts(String principal, List<String> phoneNumbers) {
    if (phoneNumbers.size() > MAX_SYNC_PHONE_NUMBERS) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "SYNC_LIMIT_EXCEEDED",
          "Maximum " + MAX_SYNC_PHONE_NUMBERS + " phone numbers per sync");
    }

    User owner = findActiveUserByPhone(principal);

    // Batch fetch all matching registered users
    List<User> matchedUsers = userRepository.findByUserPhoneInAndUserStatus(phoneNumbers, StatusConstants.ACTIVE);

    // Exclude self
    List<User> targets = matchedUsers.stream()
        .filter(u -> !u.getUserId().equals(owner.getUserId()))
        .toList();

    if (targets.isEmpty()) {
      return List.of();
    }

    List<Long> targetIds = targets.stream().map(User::getUserId).toList();

    // Batch fetch existing contacts
    Map<Long, Contact> existingMap = contactRepository
        .findByOwnerUserIdAndContactUserUserIdIn(owner.getUserId(), targetIds)
        .stream()
        .collect(Collectors.toMap(c -> c.getContactUser().getUserId(), Function.identity()));

    List<Contact> newContacts = new ArrayList<>();
    List<Contact> result = new ArrayList<>();

    for (User target : targets) {
      Contact existing = existingMap.get(target.getUserId());
      if (existing != null) {
        result.add(existing);
      } else {
        Contact contact = Contact.builder().owner(owner).contactUser(target).displayName(target.getUserFullName()).build();
        contact.setCreatedAt(OffsetDateTime.now());
        contact.setCreatedBy(owner.getUserUuid());
        newContacts.add(contact);
      }
    }

    if (!newContacts.isEmpty()) {
      try {
        result.addAll(contactRepository.saveAll(newContacts));
      } catch (DataIntegrityViolationException e) {
        // Race condition: concurrent sync created duplicates — re-fetch actual state
        result.clear();
        result.addAll(contactRepository.findByOwnerUserIdAndContactUserUserIdIn(owner.getUserId(), targetIds));
      }
    }

    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Contact> getContacts(String principal) {
    User owner = findActiveUserByPhone(principal);
    return contactRepository.findByOwnerUserId(owner.getUserId());
  }

  private User findActiveUserByPhone(String phone) {
    return userRepository.findUserByUserPhoneAndUserStatus(phone, StatusConstants.ACTIVE)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", "User not found"));
  }

  @Override
  @Transactional
  public void updatePresence(Long userId) {
    userRepository.updateLastSeenAt(userId);
  }

  @Override
  @Transactional(readOnly = true)
  public PresenceResponse getPresence(Long requesterId, String targetUuid) {
    User target = userRepository.findUserByUserUuidAndUserStatus(targetUuid, StatusConstants.ACTIVE)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", "User not found"));

    String visibility = target.getPresenceVisibility() != null
        ? target.getPresenceVisibility() : PresenceVisibility.EVERYONE.name();

    if (PresenceVisibility.NOBODY.name().equals(visibility)) {
      return PresenceResponse.builder().status(PresenceStatus.UNKNOWN.name()).build();
    }

    if (PresenceVisibility.CONTACTS_ONLY.name().equals(visibility)) {
      boolean isContact = contactRepository.existsByOwnerUserIdAndContactUserUserId(target.getUserId(), requesterId);
      if (!isContact) {
        return PresenceResponse.builder().status(PresenceStatus.UNKNOWN.name()).build();
      }
    }

    if (target.getLastSeenAt() == null) {
      return PresenceResponse.builder().status(PresenceStatus.OFFLINE.name()).build();
    }

    boolean isOnline = target.getLastSeenAt().isAfter(OffsetDateTime.now().minusMinutes(ONLINE_THRESHOLD_MINUTES));
    return PresenceResponse.builder()
        .status(isOnline ? PresenceStatus.ONLINE.name() : PresenceStatus.OFFLINE.name())
        .lastSeenAt(isOnline ? null : target.getLastSeenAt())
        .build();
  }

  private void validateLength(String value, int max, String fieldName) {
    if (value != null && value.length() > max) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
          fieldName + " must not exceed " + max + " characters");
    }
  }
}

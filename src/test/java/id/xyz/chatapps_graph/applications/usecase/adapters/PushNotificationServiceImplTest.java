package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.entity.UserDevice;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserDeviceRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceImplTest {

  @Mock private ConversationParticipantRepository participantRepository;
  @Mock private UserDeviceRepository userDeviceRepository;
  @Mock private UserRepository userRepository;
  @Mock private ConversationRepository conversationRepository;
  @Mock private MessageRepository messageRepository;
  @Mock private RabbitTemplate rabbitTemplate;

  @InjectMocks private PushNotificationServiceImpl pushService;

  private MockedStatic<FirebaseApp> firebaseAppStatic;
  private MockedStatic<FirebaseMessaging> firebaseMessagingStatic;
  @Mock private FirebaseMessaging firebaseMessaging;

  private static final Long SENDER_ID = 1L;
  private static final Long RECIPIENT_ID = 2L;
  private static final Long CONVERSATION_ID = 10L;

  @BeforeEach
  void setUp() {
    firebaseAppStatic = Mockito.mockStatic(FirebaseApp.class);
    firebaseAppStatic.when(FirebaseApp::getApps).thenReturn(List.of(Mockito.mock(FirebaseApp.class)));

    firebaseMessagingStatic = Mockito.mockStatic(FirebaseMessaging.class);
    firebaseMessagingStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
  }

  @AfterEach
  void tearDown() {
    firebaseAppStatic.close();
    firebaseMessagingStatic.close();
  }

  private Message buildMessage(String content) {
    Message m = new Message();
    m.setMessageId(100L);
    m.setMessageUuid("msg-uuid-123");
    m.setConversationId(CONVERSATION_ID);
    m.setSenderId(SENDER_ID);
    m.setMessageType("TEXT");
    m.setContent(content);
    return m;
  }

  private ConversationParticipant buildParticipant(Long userId, boolean muted) {
    return ConversationParticipant.builder()
        .userId(userId)
        .conversationId(CONVERSATION_ID)
        .isMuted(muted)
        .build();
  }

  private UserDevice buildDevice(Long userId, String token) {
    return UserDevice.builder().userId(userId).deviceToken(token).platform("ANDROID").build();
  }

  private void setupStandardMocks(Message message) {
    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(participantRepository.findAllByConversationId(CONVERSATION_ID))
        .thenReturn(List.of(buildParticipant(SENDER_ID, false), buildParticipant(RECIPIENT_ID, false)));
    when(userDeviceRepository.findByUserIdInAndDeletedAtIsNull(List.of(RECIPIENT_ID)))
        .thenReturn(List.of(buildDevice(RECIPIENT_ID, "token-abc")));
    when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(User.builder().userFullName("Alice").build()));
    when(conversationRepository.findById(CONVERSATION_ID))
        .thenReturn(Optional.of(Conversation.builder().conversationUuid("conv-uuid").build()));
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> extractData(MulticastMessage message) throws Exception {
    Field dataField = MulticastMessage.class.getDeclaredField("data");
    dataField.setAccessible(true);
    return (Map<String, String>) dataField.get(message);
  }

  @SuppressWarnings("unchecked")
  private List<String> extractTokens(MulticastMessage message) throws Exception {
    Field tokensField = MulticastMessage.class.getDeclaredField("tokens");
    tokensField.setAccessible(true);
    return (List<String>) tokensField.get(message);
  }

  @Test
  @DisplayName("sendPush: new message — sends correct payload to active tokens")
  void sendPush_NewMessage_SendsCorrectPayload() throws Exception {
    Message message = buildMessage("Hello");
    setupStandardMocks(message);

    SendResponse successResponse = Mockito.mock(SendResponse.class);
    when(successResponse.isSuccessful()).thenReturn(true);
    BatchResponse batchResponse = Mockito.mock(BatchResponse.class);
    when(batchResponse.getResponses()).thenReturn(List.of(successResponse));
    when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

    pushService.executeSendPush(message.getMessageId(), SENDER_ID, CONVERSATION_ID);

    ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
    verify(firebaseMessaging).sendEachForMulticast(captor.capture());

    MulticastMessage captured = captor.getValue();
    Map<String, String> data = extractData(captured);
    List<String> tokens = extractTokens(captured);

    assertEquals("conv-uuid", data.get("conversationUuid"));
    assertEquals("msg-uuid-123", data.get("messageUuid"));
    assertEquals(List.of("token-abc"), tokens);
  }

  @Test
  @DisplayName("sendPush: conversation muted — skips user")
  void sendPush_ConversationMuted_SkipsUser() throws Exception {
    Message message = buildMessage("Hello");
    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(participantRepository.findAllByConversationId(CONVERSATION_ID))
        .thenReturn(List.of(buildParticipant(SENDER_ID, false), buildParticipant(RECIPIENT_ID, true)));

    pushService.executeSendPush(message.getMessageId(), SENDER_ID, CONVERSATION_ID);

    verify(userDeviceRepository, never()).findByUserIdInAndDeletedAtIsNull(any());
    verify(firebaseMessaging, never()).sendEachForMulticast(any());
  }

  @Test
  @DisplayName("sendPush: no active tokens — no FCM call")
  void sendPush_NoActiveTokens_NoFcmCall() throws Exception {
    Message message = buildMessage("Hello");
    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(participantRepository.findAllByConversationId(CONVERSATION_ID))
        .thenReturn(List.of(buildParticipant(SENDER_ID, false), buildParticipant(RECIPIENT_ID, false)));
    when(userDeviceRepository.findByUserIdInAndDeletedAtIsNull(List.of(RECIPIENT_ID)))
        .thenReturn(List.of());

    pushService.executeSendPush(message.getMessageId(), SENDER_ID, CONVERSATION_ID);

    verify(firebaseMessaging, never()).sendEachForMulticast(any());
  }

  @Test
  @DisplayName("sendPush: unregistered token — soft deletes device")
  void sendPush_UnregisteredToken_SoftDeletes() throws Exception {
    Message message = buildMessage("Hello");
    UserDevice device = buildDevice(RECIPIENT_ID, "token-expired");

    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(participantRepository.findAllByConversationId(CONVERSATION_ID))
        .thenReturn(List.of(buildParticipant(SENDER_ID, false), buildParticipant(RECIPIENT_ID, false)));
    when(userDeviceRepository.findByUserIdInAndDeletedAtIsNull(List.of(RECIPIENT_ID)))
        .thenReturn(List.of(device));
    when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(User.builder().userFullName("Alice").build()));
    when(conversationRepository.findById(CONVERSATION_ID))
        .thenReturn(Optional.of(Conversation.builder().conversationUuid("conv-uuid").build()));

    SendResponse failResponse = Mockito.mock(SendResponse.class);
    when(failResponse.isSuccessful()).thenReturn(false);
    FirebaseMessagingException fme = Mockito.mock(FirebaseMessagingException.class);
    when(fme.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
    when(failResponse.getException()).thenReturn(fme);

    BatchResponse batchResponse = Mockito.mock(BatchResponse.class);
    when(batchResponse.getResponses()).thenReturn(List.of(failResponse));
    when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

    pushService.executeSendPush(message.getMessageId(), SENDER_ID, CONVERSATION_ID);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UserDevice>> saveCaptor = ArgumentCaptor.forClass(List.class);
    verify(userDeviceRepository).saveAll(saveCaptor.capture());
    List<UserDevice> deletedDevices = saveCaptor.getValue();
    assertEquals(1, deletedDevices.size());
    assertNotNull(deletedDevices.get(0).getDeletedAt());
    assertEquals("token-expired", deletedDevices.get(0).getDeviceToken());
  }

  @Test
  @DisplayName("sendPush: content > 100 chars — truncates preview to max 100")
  void sendPush_TruncatesContentTo100Chars() throws Exception {
    String longContent = "A".repeat(200);
    Message message = buildMessage(longContent);

    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(participantRepository.findAllByConversationId(CONVERSATION_ID))
        .thenReturn(List.of(buildParticipant(SENDER_ID, false), buildParticipant(RECIPIENT_ID, false)));
    when(userDeviceRepository.findByUserIdInAndDeletedAtIsNull(List.of(RECIPIENT_ID)))
        .thenReturn(List.of(buildDevice(RECIPIENT_ID, "token-1")));
    when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(User.builder().userFullName("Alice").build()));
    when(conversationRepository.findById(CONVERSATION_ID))
        .thenReturn(Optional.of(Conversation.builder().conversationUuid("conv-uuid").build()));

    SendResponse successResp = Mockito.mock(SendResponse.class);
    when(successResp.isSuccessful()).thenReturn(true);
    BatchResponse batchResponse = Mockito.mock(BatchResponse.class);
    when(batchResponse.getResponses()).thenReturn(List.of(successResp));
    when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

    pushService.executeSendPush(message.getMessageId(), SENDER_ID, CONVERSATION_ID);

    ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
    verify(firebaseMessaging).sendEachForMulticast(captor.capture());

    MulticastMessage captured = captor.getValue();
    Field notifField = MulticastMessage.class.getDeclaredField("notification");
    notifField.setAccessible(true);
    com.google.firebase.messaging.Notification notification =
        (com.google.firebase.messaging.Notification) notifField.get(captured);

    Field bodyField = notification.getClass().getDeclaredField("body");
    bodyField.setAccessible(true);
    String body = (String) bodyField.get(notification);

    assertEquals(100, body.length());
    assertEquals("A".repeat(100), body);
  }

  @Test
  @DisplayName("sendPush: verifies notification title is sender name")
  void sendPush_NotificationTitle_IsSenderName() throws Exception {
    Message message = buildMessage("Hi there");
    setupStandardMocks(message);

    SendResponse successResp = Mockito.mock(SendResponse.class);
    when(successResp.isSuccessful()).thenReturn(true);
    BatchResponse batchResponse = Mockito.mock(BatchResponse.class);
    when(batchResponse.getResponses()).thenReturn(List.of(successResp));
    when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

    pushService.executeSendPush(message.getMessageId(), SENDER_ID, CONVERSATION_ID);

    ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
    verify(firebaseMessaging).sendEachForMulticast(captor.capture());

    MulticastMessage captured = captor.getValue();
    Field notifField = MulticastMessage.class.getDeclaredField("notification");
    notifField.setAccessible(true);
    com.google.firebase.messaging.Notification notification =
        (com.google.firebase.messaging.Notification) notifField.get(captured);

    Field titleField = notification.getClass().getDeclaredField("title");
    titleField.setAccessible(true);
    String title = (String) titleField.get(notification);

    assertEquals("Alice", title);
  }

  @Test
  @DisplayName("sendPush: Firebase not initialized — returns immediately")
  void sendPush_FirebaseNotInit_ReturnsImmediately() {
    firebaseAppStatic.when(FirebaseApp::getApps).thenReturn(List.of());
    Message message = buildMessage("Hello");

    pushService.executeSendPush(message.getMessageId(), SENDER_ID, CONVERSATION_ID);

    verify(participantRepository, never()).findAllByConversationId(any());
  }

  @Test
  @DisplayName("sendPush: null content — uses message type as preview")
  void sendPush_NullContent_UsesMessageType() throws Exception {
    Message message = buildMessage(null);
    message.setMessageType("IMAGE");

    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(participantRepository.findAllByConversationId(CONVERSATION_ID))
        .thenReturn(List.of(buildParticipant(SENDER_ID, false), buildParticipant(RECIPIENT_ID, false)));
    when(userDeviceRepository.findByUserIdInAndDeletedAtIsNull(List.of(RECIPIENT_ID)))
        .thenReturn(List.of(buildDevice(RECIPIENT_ID, "token-1")));
    when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(User.builder().userFullName("Alice").build()));
    when(conversationRepository.findById(CONVERSATION_ID))
        .thenReturn(Optional.of(Conversation.builder().conversationUuid("conv-uuid").build()));

    SendResponse sr = Mockito.mock(SendResponse.class);
    when(sr.isSuccessful()).thenReturn(true);
    BatchResponse batchResponse = Mockito.mock(BatchResponse.class);
    when(batchResponse.getResponses()).thenReturn(List.of(sr));
    when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

    pushService.executeSendPush(message.getMessageId(), SENDER_ID, CONVERSATION_ID);

    ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
    verify(firebaseMessaging).sendEachForMulticast(captor.capture());

    MulticastMessage captured = captor.getValue();
    Field notifField = MulticastMessage.class.getDeclaredField("notification");
    notifField.setAccessible(true);
    com.google.firebase.messaging.Notification notification =
        (com.google.firebase.messaging.Notification) notifField.get(captured);

    Field bodyField = notification.getClass().getDeclaredField("body");
    bodyField.setAccessible(true);
    String body = (String) bodyField.get(notification);

    assertEquals("IMAGE", body);
  }

  @Test
  @DisplayName("sendPush: FCM throws exception — does not propagate, logs error")
  void sendPush_FcmException_DoesNotPropagate() throws Exception {
    Message message = buildMessage("Hello");
    setupStandardMocks(message);

    when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
        .thenThrow(new RuntimeException("FCM unavailable"));

    pushService.executeSendPush(message.getMessageId(), SENDER_ID, CONVERSATION_ID);

    verify(firebaseMessaging).sendEachForMulticast(any(MulticastMessage.class));
  }

  @Test
  @DisplayName("sendPush: content exactly 100 chars — not truncated")
  void sendPush_Content100Chars_NotTruncated() throws Exception {
    String content100 = "B".repeat(100);
    Message message = buildMessage(content100);

    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(participantRepository.findAllByConversationId(CONVERSATION_ID))
        .thenReturn(List.of(buildParticipant(SENDER_ID, false), buildParticipant(RECIPIENT_ID, false)));
    when(userDeviceRepository.findByUserIdInAndDeletedAtIsNull(List.of(RECIPIENT_ID)))
        .thenReturn(List.of(buildDevice(RECIPIENT_ID, "token-1")));
    when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(User.builder().userFullName("Alice").build()));
    when(conversationRepository.findById(CONVERSATION_ID))
        .thenReturn(Optional.of(Conversation.builder().conversationUuid("conv-uuid").build()));

    SendResponse sr = Mockito.mock(SendResponse.class);
    when(sr.isSuccessful()).thenReturn(true);
    BatchResponse batchResponse = Mockito.mock(BatchResponse.class);
    when(batchResponse.getResponses()).thenReturn(List.of(sr));
    when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

    pushService.executeSendPush(message.getMessageId(), SENDER_ID, CONVERSATION_ID);

    ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
    verify(firebaseMessaging).sendEachForMulticast(captor.capture());

    MulticastMessage captured = captor.getValue();
    Field notifField = MulticastMessage.class.getDeclaredField("notification");
    notifField.setAccessible(true);
    com.google.firebase.messaging.Notification notification =
        (com.google.firebase.messaging.Notification) notifField.get(captured);

    Field bodyField = notification.getClass().getDeclaredField("body");
    bodyField.setAccessible(true);
    String body = (String) bodyField.get(notification);

    assertEquals(100, body.length());
    assertEquals(content100, body);
  }
}

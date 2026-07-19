package id.xyz.chatapps_graph.framework.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationListService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.MessageEditResult;
import id.xyz.chatapps_graph.applications.usecase.MessageService;
import id.xyz.chatapps_graph.applications.usecase.WebSocketBroadcastService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import id.xyz.chatapps_graph.framework.dto.CreateMultiChatRequest;
import id.xyz.chatapps_graph.framework.dto.DisappearingTtlRequest;
import id.xyz.chatapps_graph.framework.dto.DisappearingTtlResponse;
import id.xyz.chatapps_graph.framework.dto.EditMessageRequest;
import id.xyz.chatapps_graph.framework.dto.MessageReactionResult;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.MultiChatResponse;
import id.xyz.chatapps_graph.framework.dto.ReactionRequest;
import id.xyz.chatapps_graph.framework.dto.SendMessageResult;
import id.xyz.chatapps_graph.framework.mapper.MessageResponseMapper;
import id.xyz.chatapps_graph.infrastructure.utility.JsonUtil;
import id.xyz.chatapps_graph.infrastructure.utility.LocaleResolver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import id.xyz.chatapps_graph.applications.usecase.ReadReceiptResult;
import id.xyz.chatapps_graph.framework.dto.CursorPageResponse;
import id.xyz.chatapps_graph.framework.dto.ForwardMessageRequest;
import id.xyz.chatapps_graph.framework.dto.MarkReadRequest;
import id.xyz.chatapps_graph.framework.dto.MessageSearchResponse;
import id.xyz.chatapps_graph.framework.dto.ConversationListResponse;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

  @Mock private MessageService messageService;
  @Mock private AttachmentService attachmentService;
  @Mock private ConversationService conversationService;
  @Mock private ConversationListService conversationListService;
  @Mock private WebSocketBroadcastService broadcastService;
  @Mock private LocaleResolver localeResolver;
  @Mock private MessageResponseMapper messageResponseMapper;

  @InjectMocks private ChatController chatController;

  private static final Long USER_ID = 1L;

  @BeforeEach
  void setUp() {
    JsonUtil.setObjectMapper(new ObjectMapper());
  }

  @Test
  @DisplayName("sendMessage without file succeeds")
  void sendMessage_NoFile_Succeeds() {
    String metadata = """
        {
          "conversationUuid": "conv-uuid",
          "messageType": "TEXT",
          "content": "Hello"
        }
        """;

    Message message = new Message();
    message.setMessageId(100L);
    message.setConversationId(10L);
    message.setContent("Hello");
    message.setMessageType("TEXT");
    message.setMessageStatus(0);

    SendMessageResult result = SendMessageResult.builder()
        .message(message)
        .senderUuid("sender-uuid")
        .conversationUuid("conv-uuid")
        .build();

    when(messageService.sendMessage(eq(USER_ID), any(), eq("conv-uuid"), eq("TEXT"), eq("Hello"), any(), any()))
        .thenReturn(result);

    ResponseEntity<BaseResponse<MessageResponse>> responseEntity = 
        chatController.sendMessage(USER_ID, null, metadata);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals("Hello", responseEntity.getBody().data().content());

    verify(broadcastService).broadcast(eq("/topic/chat/conv-uuid"), any());
  }

  @Test
  @DisplayName("editMessage succeeds and broadcasts")
  void editMessage_Succeeds() {
    EditMessageRequest request = new EditMessageRequest("new text");
    Message message = new Message();
    message.setMessageUuid("msg-uuid");
    message.setContent("new text");
    message.setMessageType("TEXT");
    message.setMessageStatus(0);

    MessageEditResult result = new MessageEditResult(message, true, "conv-uuid", "sender-uuid");

    when(messageService.editMessage(USER_ID, "msg-uuid", "new text")).thenReturn(result);

    ResponseEntity<BaseResponse<MessageResponse>> responseEntity = 
        chatController.editMessage(USER_ID, "msg-uuid", request);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals("new text", responseEntity.getBody().data().content());

    verify(broadcastService).broadcast(eq("/topic/chat/conv-uuid"), any());
  }

  @Test
  @DisplayName("deleteMessage succeeds")
  void deleteMessage_Succeeds() {
    doNothing().when(messageService).deleteMessage("msg-uuid", USER_ID, "FOR_ALL");

    ResponseEntity<BaseResponse<Void>> responseEntity = 
        chatController.deleteMessage(USER_ID, "msg-uuid", "FOR_ALL");

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  @Test
  @DisplayName("addReaction succeeds and broadcasts")
  void addReaction_Succeeds() {
    ReactionRequest request = new ReactionRequest("😀");
    MessageReactionResult result = MessageReactionResult.builder()
        .conversationUuid("conv-uuid")
        .userUuid("user-uuid")
        .emoji("😀")
        .build();

    when(messageService.addReaction(USER_ID, "msg-uuid", "😀")).thenReturn(result);

    ResponseEntity<BaseResponse<Void>> responseEntity = 
        chatController.addReaction(USER_ID, "msg-uuid", request);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    verify(broadcastService).broadcast(eq("/topic/chat/conv-uuid/reactions"), any());
  }

  @Test
  @DisplayName("removeReaction succeeds and broadcasts")
  void removeReaction_Succeeds() {
    MessageReactionResult result = MessageReactionResult.builder()
        .conversationUuid("conv-uuid")
        .userUuid("user-uuid")
        .build();

    when(messageService.removeReaction(USER_ID, "msg-uuid")).thenReturn(result);

    ResponseEntity<BaseResponse<Void>> responseEntity = 
        chatController.removeReaction(USER_ID, "msg-uuid");

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    verify(broadcastService).broadcast(eq("/topic/chat/conv-uuid/reactions"), any());
  }

  @Test
  @DisplayName("createMultiChat succeeds")
  void createMultiChat_Succeeds() {
    CreateMultiChatRequest request = new CreateMultiChatRequest(List.of("uuid-1", "uuid-2"));
    MultiChatResponse response = MultiChatResponse.builder()
        .conversationUuid("conv-uuid")
        .participants(List.of())
        .build();

    when(conversationService.createMultiChatByUuids(USER_ID, List.of("uuid-1", "uuid-2")))
        .thenReturn(response);

    ResponseEntity<BaseResponse<MultiChatResponse>> responseEntity = 
        chatController.createMultiChat(USER_ID, request);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals("conv-uuid", responseEntity.getBody().data().conversationUuid());
  }

  @Test
  @DisplayName("updateDisappearingTtl succeeds and broadcasts")
  void updateDisappearingTtl_Succeeds() {
    DisappearingTtlRequest request = new DisappearingTtlRequest("24h");
    Conversation conversation = new Conversation();
    conversation.setConversationUuid("conv-uuid");
    conversation.setDisappearingTtl(24);

    when(conversationService.updateDisappearingTtl("conv-uuid", USER_ID, 24))
        .thenReturn(conversation);

    ResponseEntity<BaseResponse<DisappearingTtlResponse>> responseEntity = 
        chatController.updateDisappearingTtl(USER_ID, "conv-uuid", request);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals("conv-uuid", responseEntity.getBody().data().conversationUuid());
  }

  @Test
  @DisplayName("listMessages succeeds")
  void listMessages_Succeeds() {
    Conversation conv = new Conversation();
    conv.setConversationId(10L);
    conv.setConversationUuid("conv-uuid");

    when(conversationService.findConversationByUuid("conv-uuid")).thenReturn(conv);
    doNothing().when(conversationService).validateParticipant(10L, USER_ID);
    when(localeResolver.resolve(any())).thenReturn("id");

    Message msg = new Message();
    msg.setMessageId(100L);
    msg.setMessageUuid("msg-uuid");
    msg.setContent("Hello");
    msg.setMessageStatus(0);
    msg.setMessageType("TEXT");

    when(messageService.listMessages(10L, USER_ID, null, 21)).thenReturn(List.of(msg));
    
    MessageResponse mr = MessageResponse.builder()
        .messageUuid("msg-uuid")
        .content("Hello")
        .build();
    when(messageResponseMapper.toResponseList(any(), eq("conv-uuid"), eq(USER_ID), eq("id"))).thenReturn(List.of(mr));

    ResponseEntity<BaseResponse<CursorPageResponse<MessageResponse>>> responseEntity = 
        chatController.listMessages(USER_ID, "conv-uuid", null, 20, null);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().data().messages().size());
  }

  @Test
  @DisplayName("markAsRead succeeds")
  void markAsRead_Succeeds() {
    Conversation conv = new Conversation();
    conv.setConversationId(10L);
    conv.setConversationUuid("conv-uuid");

    when(conversationService.findConversationByUuid("conv-uuid")).thenReturn(conv);
    doNothing().when(conversationService).validateParticipant(10L, USER_ID);

    ReadReceiptResult receiptResult = new ReadReceiptResult(true, List.of("123456789"), "reader-uuid");
    when(messageService.markAsRead("conv-uuid", USER_ID)).thenReturn(receiptResult);

    ResponseEntity<BaseResponse<Void>> responseEntity = 
        chatController.markAsRead(USER_ID, new MarkReadRequest("conv-uuid"));

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    verify(broadcastService).sendToUser(eq("123456789"), eq("/queue/chat/receipts"), any());
  }

  @Test
  @DisplayName("listConversations succeeds")
  void listConversations_Succeeds() {
    ConversationListResponse response = new ConversationListResponse(List.of(), null, false);
    when(conversationListService.listConversations(USER_ID, "ALL", null, 20)).thenReturn(response);

    ResponseEntity<BaseResponse<ConversationListResponse>> responseEntity = 
        chatController.listConversations(USER_ID, "ALL", null, 20);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  @Test
  @DisplayName("searchMessages succeeds")
  void searchMessages_Succeeds() {
    Message msg = new Message();
    msg.setMessageId(100L);
    msg.setContent("query text");
    msg.setCreatedAt(java.time.OffsetDateTime.now());

    when(messageService.searchMessages(USER_ID, "query", "conv-uuid", null, 21))
        .thenReturn(List.of(msg));

    when(messageResponseMapper.toSearchResultItemList(any())).thenReturn(List.of());

    ResponseEntity<BaseResponse<MessageSearchResponse>> responseEntity = 
        chatController.searchMessages(USER_ID, "query", 20, null, "conv-uuid");

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  @Test
  @DisplayName("pin/unpin/archive/unarchive/mute/unmute succeeds")
  void pinUnpinArchiveUnarchiveMuteUnmute_Succeeds() {
    doNothing().when(conversationListService).pinConversation(USER_ID, "conv-uuid");
    doNothing().when(conversationListService).unpinConversation(USER_ID, "conv-uuid");
    doNothing().when(conversationListService).archiveConversation(USER_ID, "conv-uuid");
    doNothing().when(conversationListService).unarchiveConversation(USER_ID, "conv-uuid");
    doNothing().when(conversationListService).muteConversation(USER_ID, "conv-uuid");
    doNothing().when(conversationListService).unmuteConversation(USER_ID, "conv-uuid");

    assertEquals(HttpStatus.OK, chatController.pinConversation(USER_ID, "conv-uuid").getStatusCode());
    assertEquals(HttpStatus.OK, chatController.unpinConversation(USER_ID, "conv-uuid").getStatusCode());
    assertEquals(HttpStatus.OK, chatController.archiveConversation(USER_ID, "conv-uuid").getStatusCode());
    assertEquals(HttpStatus.OK, chatController.unarchiveConversation(USER_ID, "conv-uuid").getStatusCode());
    assertEquals(HttpStatus.OK, chatController.muteConversation(USER_ID, "conv-uuid").getStatusCode());
    assertEquals(HttpStatus.OK, chatController.unmuteConversation(USER_ID, "conv-uuid").getStatusCode());
  }

  @Test
  @DisplayName("forwardMessage succeeds")
  void forwardMessage_Succeeds() {
    ForwardMessageRequest request = new ForwardMessageRequest("msg-uuid", "conv-uuid");
    Message msg = new Message();
    msg.setConversationId(10L);

    Conversation conv = new Conversation();
    conv.setConversationUuid("conv-uuid");

    when(messageService.forwardMessage(USER_ID, "msg-uuid", "conv-uuid")).thenReturn(msg);
    when(conversationService.findConversationById(10L)).thenReturn(conv);

    MessageResponse mr = MessageResponse.builder()
        .messageUuid("msg-uuid")
        .build();
    when(messageResponseMapper.toResponseList(any(), eq("conv-uuid"), eq(USER_ID), eq("id"))).thenReturn(List.of(mr));

    ResponseEntity<BaseResponse<MessageResponse>> responseEntity = 
        chatController.forwardMessage(USER_ID, request);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    verify(broadcastService).broadcast(eq("/topic/chat/conv-uuid"), any());
  }
}

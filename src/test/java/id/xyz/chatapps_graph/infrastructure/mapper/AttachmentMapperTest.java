package id.xyz.chatapps_graph.infrastructure.mapper;

import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.framework.dto.AttachmentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttachmentMapperTest {

  @Test
  @DisplayName("toResponse: maps all fields correctly")
  void toResponse_MapsAllFields() {
    Attachment attachment = Attachment.builder()
        .attachmentUuid("att-uuid")
        .fileName("document.pdf")
        .filePath("chat/user-uuid/1718200000_document.pdf")
        .fileSize(2048L)
        .contentType("application/pdf")
        .attachmentType("FILE")
        .build();

    AttachmentResponse result = AttachmentMapper.toResponse(attachment);

    assertEquals("att-uuid", result.attachmentUuid());
    assertEquals("document.pdf", result.fileName());
    assertEquals("chat/user-uuid/1718200000_document.pdf", result.filePath());
    assertEquals(2048L, result.fileSize());
    assertEquals("application/pdf", result.contentType());
    assertEquals("FILE", result.attachmentType());
  }

  @Test
  @DisplayName("toResponse: null attachment returns null")
  void toResponse_NullAttachment_ReturnsNull() {
    assertNull(AttachmentMapper.toResponse(null));
  }
}

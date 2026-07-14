package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.properties.MediaProperties;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.utility.ImageProcessingService;
import id.xyz.chatapps_graph.infrastructure.utility.VideoThumbnailService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {

  @Mock private FileStoragePort fileStoragePort;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private UserRepository userRepository;
  @Mock private MultipartFile multipartFile;
  @Mock private ImageProcessingService imageProcessingService;
  @Mock private VideoThumbnailService videoThumbnailService;

  private AttachmentServiceImpl attachmentService;

  private static final Long UPLOADER_ID = 1L;
  private static final String UPLOADER_UUID = "user-uuid-123";

  @org.junit.jupiter.api.BeforeEach
  void setUp() throws Exception {
    MediaProperties props = new MediaProperties();
    props.setMaxFileSize(15L * 1024 * 1024);
    MediaProperties.ImageProperties imgProps = new MediaProperties.ImageProperties();
    imgProps.setMaxDimension(1920);
    imgProps.setQuality(85);
    imgProps.setThumbnailDimension(200);
    imgProps.setThumbnailQuality(60);
    props.setImage(imgProps);

    attachmentService = new AttachmentServiceImpl(
        fileStoragePort, attachmentRepository, props, imageProcessingService, videoThumbnailService, userRepository);
  }

  private void setupFile(String contentType, String originalName, long size) throws Exception {
    when(multipartFile.getContentType()).thenReturn(contentType);
    when(multipartFile.getOriginalFilename()).thenReturn(originalName);
    when(multipartFile.getSize()).thenReturn(size);
    org.mockito.Mockito.lenient().when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
  }

  private void mockUserLookup() {
    User user = User.builder()
        .userId(UPLOADER_ID)
        .userUuid(UPLOADER_UUID)
        .build();
    when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user));
  }

  @Test
  @DisplayName("upload IMAGE: valid jpeg — succeeds and saves attachment")
  void upload_Image_ValidJpeg_Succeeds() throws Exception {
    mockUserLookup();
    setupFile("image/jpeg", "photo.jpg", 1024L);
    when(multipartFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
    when(imageProcessingService.compressAndThumbnail(any(InputStream.class), eq(1920), eq(85f), eq(200), eq(60f)))
            .thenReturn(new id.xyz.chatapps_graph.infrastructure.utility.ImageProcessingService.ImageProcessingResult(new byte[]{10, 20}, new byte[]{30, 40}));
    
    when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

    Attachment result = attachmentService.validateAndUpload(multipartFile, "IMAGE", UPLOADER_ID);

    assertNotNull(result);
    assertEquals("photo.jpg", result.getFileName());
    assertEquals("image/jpeg", result.getContentType());
    assertEquals("IMAGE", result.getAttachmentType());
    assertEquals(UPLOADER_ID, result.getUploaderId());
    verify(fileStoragePort, org.mockito.Mockito.atLeastOnce()).uploadFile(anyString(), any(InputStream.class), eq("image/jpeg"), anyLong());
  }

  @Test
  @DisplayName("upload IMAGE: invalid content type — throws GeneralException")
  void upload_Image_InvalidContentType_Throws() {
    when(multipartFile.getSize()).thenReturn(1024L);
    when(multipartFile.getContentType()).thenReturn("application/pdf");

    GeneralException ex = assertThrows(GeneralException.class,
        () -> attachmentService.validateAndUpload(multipartFile, "IMAGE", UPLOADER_ID));

    assertEquals(400, ex.getHttpCode());
    assertEquals("INVALID_CONTENT_TYPE", ex.getKey());
  }

  @Test
  @DisplayName("upload VOICE: ogg format — succeeds")
  void upload_Voice_OggFormat_Succeeds() throws Exception {
    mockUserLookup();
    setupFile("audio/ogg", "voice.ogg", 2048L);
    when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

    Attachment result = attachmentService.validateAndUpload(multipartFile, "VOICE", UPLOADER_ID);

    assertEquals("VOICE", result.getAttachmentType());
    assertEquals("audio/ogg", result.getContentType());
    verify(fileStoragePort).uploadFile(anyString(), any(InputStream.class), eq("audio/ogg"), eq(2048L));
  }

  @Test
  @DisplayName("upload FILE: any content type — succeeds (no restriction)")
  void upload_File_AnyType_Succeeds() throws Exception {
    mockUserLookup();
    setupFile("application/x-custom", "data.bin", 500L);
    when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

    Attachment result = attachmentService.validateAndUpload(multipartFile, "FILE", UPLOADER_ID);

    assertEquals("FILE", result.getAttachmentType());
    assertEquals("application/x-custom", result.getContentType());
  }

  @Test
  @DisplayName("upload: Minio path contains timestamp and uploader UUID")
  void upload_MinioPath_ContainsTimestamp() throws Exception {
    mockUserLookup();
    setupFile("image/png", "img.png", 100L);
    when(multipartFile.getBytes()).thenReturn(new byte[]{1});
    when(imageProcessingService.compressAndThumbnail(any(InputStream.class), eq(1920), eq(85f), eq(200), eq(60f)))
            .thenReturn(new id.xyz.chatapps_graph.infrastructure.utility.ImageProcessingService.ImageProcessingResult(new byte[]{1}, new byte[]{30, 40}));
    
    when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

    Attachment result = attachmentService.validateAndUpload(multipartFile, "IMAGE", UPLOADER_ID);

    assertTrue(result.getFilePath().startsWith("chat/" + UPLOADER_UUID + "/"));
    assertTrue(result.getFilePath().contains("_img.png"));
    
    String pathAfterPrefix = result.getFilePath().replace("chat/" + UPLOADER_UUID + "/", "");
    String timestampPart = pathAfterPrefix.split("_")[0];
    assertTrue(timestampPart.matches("\\d+"));
  }

  @Test
  @DisplayName("deleteFile: delegates to fileStoragePort")
  void deleteFile_CallsMinioRemove() {
    String path = "chat/user-uuid/123_file.txt";

    attachmentService.deleteFile(path);

    verify(fileStoragePort).deleteFile(path);
  }
}

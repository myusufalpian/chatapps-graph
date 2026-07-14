package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.service.FileStoragePort;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.properties.MediaProperties;
import id.xyz.chatapps_graph.infrastructure.utility.ImageProcessingService;
import id.xyz.chatapps_graph.infrastructure.utility.VideoThumbnailService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplSprint2bTest {

  @Mock private FileStoragePort fileStoragePort;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private ImageProcessingService imageProcessingService;
  @Mock private VideoThumbnailService videoThumbnailService;
  @Mock private UserRepository userRepository;
  @Mock private MultipartFile multipartFile;

  private AttachmentServiceImpl attachmentService;

  private static final Long UPLOADER_ID = 1L;
  private static final String UPLOADER_UUID = "user-uuid-123";

  @BeforeEach
  void setUp() {
    MediaProperties mediaProperties = new MediaProperties();
    mediaProperties.setMaxFileSize(20971520L);
    MediaProperties.ImageProperties imgProps = new MediaProperties.ImageProperties();
    imgProps.setMaxDimension(1920);
    imgProps.setQuality(85);
    imgProps.setThumbnailDimension(200);
    imgProps.setThumbnailQuality(60);
    mediaProperties.setImage(imgProps);

    attachmentService = new AttachmentServiceImpl(
        fileStoragePort, attachmentRepository, mediaProperties, imageProcessingService, videoThumbnailService, userRepository);
  }

  private byte[] createTestImageBytes() throws IOException {
    BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "png", baos);
    return baos.toByteArray();
  }

  private void mockUserLookup() {
    User user = User.builder()
        .userId(UPLOADER_ID)
        .userUuid(UPLOADER_UUID)
        .build();
    when(userRepository.findById(UPLOADER_ID)).thenReturn(Optional.of(user));
  }

  @Test
  @DisplayName("uploadImage: compresses and generates thumbnail")
  void uploadImage_CompressesAndGeneratesThumbnail() throws Exception {
    mockUserLookup();
    byte[] rawBytes = createTestImageBytes();
    byte[] compressed = new byte[]{1, 2, 3};
    byte[] thumbnail = new byte[]{4, 5, 6};

    when(multipartFile.getContentType()).thenReturn("image/jpeg");
    when(multipartFile.getOriginalFilename()).thenReturn("photo.jpg");
    when(multipartFile.getSize()).thenReturn(1024L);
    when(multipartFile.getBytes()).thenReturn(rawBytes);
    when(imageProcessingService.compressAndThumbnail(any(InputStream.class), eq(1920), eq(85f), eq(200), eq(60f)))
            .thenReturn(new id.xyz.chatapps_graph.infrastructure.utility.ImageProcessingService.ImageProcessingResult(compressed, thumbnail));
    
    when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

    Attachment result = attachmentService.validateAndUpload(multipartFile, "IMAGE", UPLOADER_ID);

    verify(fileStoragePort, times(2)).uploadFile(anyString(), any(InputStream.class), eq("image/jpeg"), anyLong());
    assertNotNull(result.getThumbnailPath());
    assertTrue(result.getThumbnailPath().startsWith("thumbnails/"));
  }

  @Test
  @DisplayName("uploadImage: exceeds max size — rejects with 413")
  void uploadImage_ExceedsMaxSize_Rejects() {
    when(multipartFile.getSize()).thenReturn(30_000_000L);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> attachmentService.validateAndUpload(multipartFile, "IMAGE", UPLOADER_ID));

    assertEquals(413, ex.getHttpCode());
    assertEquals("FILE_TOO_LARGE", ex.getKey());
    verify(fileStoragePort, never()).uploadFile(anyString(), any(InputStream.class), anyString(), anyLong());
  }

  @Test
  @DisplayName("uploadVideo: ffmpeg available — generates thumbnail")
  void uploadVideo_FfmpegAvailable_GeneratesThumbnail() throws Exception {
    mockUserLookup();
    byte[] thumbnail = new byte[]{7, 8, 9};
    when(multipartFile.getContentType()).thenReturn("video/mp4");
    when(multipartFile.getOriginalFilename()).thenReturn("video.mp4");
    when(multipartFile.getSize()).thenReturn(5000L);
    when(multipartFile.getInputStream()).thenReturn(InputStream.nullInputStream());
    when(videoThumbnailService.extractFirstFrame(any(Path.class), eq(200))).thenReturn(thumbnail);
    when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

    Attachment result = attachmentService.validateAndUpload(multipartFile, "VIDEO", UPLOADER_ID);

    assertNotNull(result.getThumbnailPath());
    assertTrue(result.getThumbnailPath().contains("_thumb.jpg"));
  }

  @Test
  @DisplayName("uploadVideo: ffmpeg unavailable — thumbnail null")
  void uploadVideo_FfmpegUnavailable_ThumbnailNull() throws Exception {
    mockUserLookup();
    when(multipartFile.getContentType()).thenReturn("video/mp4");
    when(multipartFile.getOriginalFilename()).thenReturn("video.mp4");
    when(multipartFile.getSize()).thenReturn(5000L);
    when(multipartFile.getInputStream()).thenReturn(InputStream.nullInputStream());
    when(videoThumbnailService.extractFirstFrame(any(Path.class), eq(200))).thenReturn(null);
    when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

    Attachment result = attachmentService.validateAndUpload(multipartFile, "VIDEO", UPLOADER_ID);

    assertNull(result.getThumbnailPath());
  }

  @Test
  @DisplayName("uploadFile: non-image — no compression, no thumbnail")
  void uploadFile_NonImage_NoCompression() throws Exception {
    mockUserLookup();
    when(multipartFile.getContentType()).thenReturn("application/pdf");
    when(multipartFile.getOriginalFilename()).thenReturn("doc.pdf");
    when(multipartFile.getSize()).thenReturn(1024L);
    when(multipartFile.getInputStream()).thenReturn(InputStream.nullInputStream());
    when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

    Attachment result = attachmentService.validateAndUpload(multipartFile, "FILE", UPLOADER_ID);

    assertNull(result.getThumbnailPath());
    verify(imageProcessingService, never()).compressAndThumbnail(any(), eq(1920), eq(85f), eq(200), eq(60f));
  }

  @Test
  @DisplayName("attachmentResponse: thumbnailUrl included when path exists")
  void attachmentResponse_IncludesThumbnailUrl() throws Exception {
    mockUserLookup();
    byte[] rawBytes = createTestImageBytes();
    byte[] compressed = new byte[]{1, 2};
    byte[] thumbnail = new byte[]{3, 4};

    when(multipartFile.getContentType()).thenReturn("image/jpeg");
    when(multipartFile.getOriginalFilename()).thenReturn("img.jpg");
    when(multipartFile.getSize()).thenReturn(512L);
    when(multipartFile.getBytes()).thenReturn(rawBytes);
    when(imageProcessingService.compressAndThumbnail(any(InputStream.class), eq(1920), eq(85f), eq(200), eq(60f)))
            .thenReturn(new id.xyz.chatapps_graph.infrastructure.utility.ImageProcessingService.ImageProcessingResult(compressed, thumbnail));
    
    when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

    Attachment result = attachmentService.validateAndUpload(multipartFile, "IMAGE", UPLOADER_ID);

    var response = id.xyz.chatapps_graph.infrastructure.mapper.AttachmentMapper.toResponse(result, "http://localhost:9000/test-bucket");
    assertNotNull(response.thumbnailUrl());
    assertTrue(response.thumbnailUrl().contains("thumbnails/"));
  }

  @Test
  @DisplayName("uploadImage: IOException during processing — throws UPLOAD_FAILED")
  void uploadImage_IoException_ThrowsUploadFailed() throws Exception {
    mockUserLookup();
    when(multipartFile.getContentType()).thenReturn("image/jpeg");
    when(multipartFile.getOriginalFilename()).thenReturn("photo.jpg");
    when(multipartFile.getSize()).thenReturn(1024L);
    when(multipartFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
    when(imageProcessingService.compressAndThumbnail(any(InputStream.class), eq(1920), eq(85f), eq(200), eq(60f)))
        .thenThrow(new IOException("Unable to decode image"));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> attachmentService.validateAndUpload(multipartFile, "IMAGE", UPLOADER_ID));

    assertEquals(500, ex.getHttpCode());
    assertEquals("UPLOAD_FAILED", ex.getKey());
    verify(fileStoragePort, never()).uploadFile(anyString(), any(InputStream.class), anyString(), anyLong());
  }

  @Test
  @DisplayName("uploadImage: invalid content type for IMAGE — throws 400")
  void uploadImage_InvalidContentType_Throws400() {
    when(multipartFile.getSize()).thenReturn(1024L);
    when(multipartFile.getContentType()).thenReturn("text/plain");

    GeneralException ex = assertThrows(GeneralException.class,
        () -> attachmentService.validateAndUpload(multipartFile, "IMAGE", UPLOADER_ID));

    assertEquals(400, ex.getHttpCode());
    assertEquals("INVALID_CONTENT_TYPE", ex.getKey());
  }
}

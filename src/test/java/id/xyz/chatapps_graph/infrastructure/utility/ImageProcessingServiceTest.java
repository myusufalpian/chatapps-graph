package id.xyz.chatapps_graph.infrastructure.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageProcessingServiceTest {

  private ImageProcessingService imageProcessingService;

  @BeforeEach
  void setUp() {
    imageProcessingService = new ImageProcessingService();
  }

  private byte[] createTestImage(int width, int height) throws IOException {
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "png", baos);
    return baos.toByteArray();
  }

  @Test
  @DisplayName("compressImage: larger than max — resizes to max dimension")
  void compressImage_LargerThanMax_Resizes() throws IOException {
    byte[] input = createTestImage(3000, 2000);

    byte[] result = imageProcessingService.compressImage(new ByteArrayInputStream(input), 1920, 85);

    assertNotNull(result);
    BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));
    assertEquals(1920, output.getWidth());
    assertEquals(1280, output.getHeight());
  }

  @Test
  @DisplayName("compressImage: smaller than max — only recompresses, keeps dimensions")
  void compressImage_SmallerThanMax_OnlyRecompresses() throws IOException {
    byte[] input = createTestImage(800, 600);

    byte[] result = imageProcessingService.compressImage(new ByteArrayInputStream(input), 1920, 85);

    assertNotNull(result);
    BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));
    assertEquals(800, output.getWidth());
    assertEquals(600, output.getHeight());
  }

  @Test
  @DisplayName("compressImage: maintains aspect ratio")
  void compressImage_MaintainsAspectRatio() throws IOException {
    byte[] input = createTestImage(4000, 3000); // 4:3

    byte[] result = imageProcessingService.compressImage(new ByteArrayInputStream(input), 1920, 85);

    BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));
    assertEquals(1920, output.getWidth());
    assertEquals(1440, output.getHeight()); // 4:3 maintained
  }

  @Test
  @DisplayName("generateThumbnail: valid image — returns 200px jpeg")
  void generateThumbnail_ValidImage_Returns200pxJpeg() throws IOException {
    byte[] input = createTestImage(1000, 800);

    byte[] result = imageProcessingService.generateThumbnail(new ByteArrayInputStream(input), 200, 60);

    assertNotNull(result);
    BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));
    assertEquals(200, output.getWidth());
    assertEquals(160, output.getHeight());
  }

  @Test
  @DisplayName("compressImage: portrait image — resizes by height")
  void compressImage_PortraitImage_ResizesByHeight() throws IOException {
    byte[] input = createTestImage(2000, 4000); // tall portrait

    byte[] result = imageProcessingService.compressImage(new ByteArrayInputStream(input), 1920, 85);

    BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));
    assertEquals(960, output.getWidth());
    assertEquals(1920, output.getHeight());
  }

  @Test
  @DisplayName("compressImage: invalid input — throws IOException")
  void compressImage_InvalidInput_Throws() {
    byte[] garbage = new byte[]{0x00, 0x01, 0x02};

    assertThrows(IOException.class,
        () -> imageProcessingService.compressImage(new ByteArrayInputStream(garbage), 1920, 85));
  }

  @Test
  @DisplayName("compressImage: output is valid JPEG")
  void compressImage_OutputIsValidJpeg() throws IOException {
    byte[] input = createTestImage(1920, 1080);

    byte[] result = imageProcessingService.compressImage(new ByteArrayInputStream(input), 1920, 85);

    assertNotNull(result);
    // Verify it's a valid JPEG by reading it back
    BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));
    assertNotNull(output);
    assertEquals(1920, output.getWidth());
  }

  @Test
  @DisplayName("compressImage: exceeds max decoded dimension (8192) — throws IOException")
  void compressImage_ExceedsMaxDecodedDimension_Throws() throws IOException {
    byte[] input = createTestImage(9000, 5000);

    IOException ex = assertThrows(IOException.class,
        () -> imageProcessingService.compressImage(new ByteArrayInputStream(input), 1920, 85));

    assertTrue(ex.getMessage().contains("dimensions exceed maximum"));
  }

  @Test
  @DisplayName("generateThumbnail: exceeds max decoded dimension — throws IOException")
  void generateThumbnail_ExceedsMaxDecodedDimension_Throws() throws IOException {
    byte[] input = createTestImage(5000, 9000);

    IOException ex = assertThrows(IOException.class,
        () -> imageProcessingService.generateThumbnail(new ByteArrayInputStream(input), 200, 60));

    assertTrue(ex.getMessage().contains("dimensions exceed maximum"));
  }

  @Test
  @DisplayName("compressAndThumbnail: produces both compressed and thumbnail in single decode")
  void compressAndThumbnail_ProducesBoth() throws IOException {
    byte[] input = createTestImage(3000, 2000);

    ImageProcessingService.ImageProcessingResult result =
        imageProcessingService.compressAndThumbnail(new ByteArrayInputStream(input), 1920, 85, 200, 60);

    assertNotNull(result.compressed());
    assertNotNull(result.thumbnail());

    BufferedImage compressed = ImageIO.read(new ByteArrayInputStream(result.compressed()));
    assertEquals(1920, compressed.getWidth());
    assertEquals(1280, compressed.getHeight());

    BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(result.thumbnail()));
    assertEquals(200, thumbnail.getWidth());
    assertEquals(133, thumbnail.getHeight());
  }
}

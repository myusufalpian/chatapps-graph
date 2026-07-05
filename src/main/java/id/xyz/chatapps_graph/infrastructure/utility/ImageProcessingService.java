package id.xyz.chatapps_graph.infrastructure.utility;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ImageProcessingService {

  private static final int MAX_DECODED_DIMENSION = 8192;

  public byte[] compressImage(InputStream input, int maxDimension, float quality) throws IOException {
    BufferedImage original = readAndValidate(input);
    BufferedImage resized = resizeIfNeeded(original, maxDimension);
    return writeJpeg(resized, quality);
  }

  public byte[] generateThumbnail(InputStream input, int dimension, float quality) throws IOException {
    BufferedImage original = readAndValidate(input);
    BufferedImage resized = resizeIfNeeded(original, dimension);
    return writeJpeg(resized, quality);
  }

  public ImageProcessingResult compressAndThumbnail(InputStream input, int maxDimension, float compressQuality,
      int thumbDimension, float thumbQuality) throws IOException {
    BufferedImage original = readAndValidate(input);
    BufferedImage compressed = resizeIfNeeded(original, maxDimension);
    byte[] compressedBytes = writeJpeg(compressed, compressQuality);
    BufferedImage thumb = resizeIfNeeded(original, thumbDimension);
    byte[] thumbnailBytes = writeJpeg(thumb, thumbQuality);
    return new ImageProcessingResult(compressedBytes, thumbnailBytes);
  }

  private BufferedImage readAndValidate(InputStream input) throws IOException {
    BufferedImage original = ImageIO.read(input);
    if (original == null) {
      throw new IOException("Unable to decode image");
    }
    validateDecodedDimension(original);
    return original;
  }

  public record ImageProcessingResult(byte[] compressed, byte[] thumbnail) {}

  private void validateDecodedDimension(BufferedImage image) throws IOException {
    if (image.getWidth() > MAX_DECODED_DIMENSION || image.getHeight() > MAX_DECODED_DIMENSION) {
      throw new IOException("Image dimensions exceed maximum allowed: " + MAX_DECODED_DIMENSION + "x" + MAX_DECODED_DIMENSION);
    }
  }

  private BufferedImage resizeIfNeeded(BufferedImage image, int maxDimension) {
    int width = image.getWidth();
    int height = image.getHeight();

    if (width <= maxDimension && height <= maxDimension) {
      return ensureRgb(image);
    }

    double scale = (double) maxDimension / Math.max(width, height);
    int newWidth = (int) (width * scale);
    int newHeight = (int) (height * scale);

    BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = resized.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2d.setColor(java.awt.Color.WHITE);
    g2d.fillRect(0, 0, newWidth, newHeight);
    g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
    g2d.dispose();
    return resized;
  }

  private BufferedImage ensureRgb(BufferedImage image) {
    if (image.getType() == BufferedImage.TYPE_INT_RGB) {
      return image;
    }
    BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = rgb.createGraphics();
    g2d.setColor(java.awt.Color.WHITE);
    g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
    g2d.drawImage(image, 0, 0, null);
    g2d.dispose();
    return rgb;
  }

  private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(quality / 100f);

    try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
      writer.setOutput(ios);
      writer.write(null, new IIOImage(image, null, null), param);
    } finally {
      writer.dispose();
    }
    return baos.toByteArray();
  }
}

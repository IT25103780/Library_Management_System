package lk.lumina.service;

import jakarta.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import lk.lumina.config.AppConfig;
import lk.lumina.util.BusinessRules;

/** Validates and stores uploaded covers and PDFs. */
public final class FileStorageService {
  public static String upload(Part part, boolean pdf, List<Path> created) throws Exception {
    if (part == null || part.getSize() == 0) return null;
    BusinessRules.require(
        part.getSize() <= (pdf ? 50L * 1024 * 1024 : 8L * 1024 * 1024),
        pdf ? "PDF must be under 50 MB." : "Cover must be under 8 MB.");
    Path folder = AppConfig.data().resolve(pdf ? "pdf" : "covers");
    Files.createDirectories(folder);
    String name = UUID.randomUUID() + (pdf ? ".pdf" : ".jpg");
    Path dest = folder.resolve(name);
    if (pdf) {
      try (InputStream in = part.getInputStream()) {
        byte[] magic = in.readNBytes(5);
        BusinessRules.require(Arrays.equals(magic, "%PDF-".getBytes()), "Upload a valid PDF file.");
      }
      try (InputStream in = part.getInputStream()) {
        Files.copy(in, dest);
      }
      created.add(dest);
      try (var document =
          org.apache.pdfbox.pdmodel.PDDocument.load(
              dest.toFile(), org.apache.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())) {
        BusinessRules.require(
            !document.isEncrypted() && document.getNumberOfPages() > 0,
            "Upload an unencrypted PDF with at least one page.");
        BusinessRules.require(
            document.getDocumentCatalog().getOpenAction() == null,
            "Remove automatic actions from the PDF before upload.");
      }
    } else {
      try (var imageInput = ImageIO.createImageInputStream(part.getInputStream())) {
        var readers = ImageIO.getImageReaders(imageInput);
        BusinessRules.require(readers.hasNext(), "Upload a JPG or PNG cover.");
        var reader = readers.next();
        try {
          reader.setInput(imageInput);
          BusinessRules.require(
              Set.of("JPEG", "PNG").contains(reader.getFormatName().toUpperCase(Locale.ROOT)),
              "Upload a JPG or PNG cover.");
          int w = reader.getWidth(0), h = reader.getHeight(0);
          BusinessRules.require(
              w > 0 && h > 0 && (long) w * h <= 24000000,
              "Cover image is too large; use at most 24 megapixels.");
          BufferedImage img = reader.read(0);
          double scale = Math.min(1, 1000.0 / Math.max(w, h));
          BufferedImage out =
              new BufferedImage(
                  Math.max(1, (int) (w * scale)),
                  Math.max(1, (int) (h * scale)),
                  BufferedImage.TYPE_INT_RGB);
          var g = out.createGraphics();
          g.setColor(java.awt.Color.WHITE);
          g.fillRect(0, 0, out.getWidth(), out.getHeight());
          g.setRenderingHint(
              java.awt.RenderingHints.KEY_INTERPOLATION,
              java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
          g.drawImage(img, 0, 0, out.getWidth(), out.getHeight(), null);
          g.dispose();
          ImageIO.write(out, "jpg", dest.toFile());
          created.add(dest);
        } finally {
          reader.dispose();
        }
      }
    }
    return name;
  }
}

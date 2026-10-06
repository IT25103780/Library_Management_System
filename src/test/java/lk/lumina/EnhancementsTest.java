package lk.lumina;
import static org.junit.jupiter.api.Assertions.*;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lk.lumina.config.ApplicationInitializer;
import lk.lumina.config.DatabaseMigrations;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.security.Security;
import lk.lumina.service.PaymentService;
import lk.lumina.service.ReceiptService;
import lk.lumina.util.Barcodes;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class EnhancementsTest {
  @Test
  void passwordRulesRejectEachMissingRequirement() {
    for (String v :
        new String[] {
          "short!A1",
          "longpassword!1",
          "LONGPASSWORD!1",
          "Longpassword!!",
          "Longpassword12",
          "Password123!"
        }) assertThrows(IllegalArgumentException.class, () -> Security.password(v), v);
    assertDoesNotThrow(() -> Security.password("Quiet!River2026"));
    assertDoesNotThrow(() -> Security.password(" Quiet!River2026 "));
  }

  @Test
  void isbnChecksAndConversion() {
    assertEquals("9780306406157", Barcodes.isbn("0-306-40615-2"));
    assertEquals("9780306406157", Barcodes.isbn("978-0-306-40615-7"));
    assertThrows(IllegalArgumentException.class, () -> Barcodes.isbn("9780306406158"));
    assertThrows(IllegalArgumentException.class, () -> Barcodes.isbn("1234567890128"));
    assertFalse(Barcodes.valid("LUM-0001"));
    assertTrue(Barcodes.html("LUM-0001").contains("ISBN NOT ASSIGNED"));
  }

  @Test
  void barcodesDecodeToTheirOriginalIdentifiers() throws Exception {
    for (String value : List.of("9780306406157", "LUM-0001")) {
      BitMatrix m = Barcodes.matrix(value);
      int width = m.getWidth() * 3, height = m.getHeight() * 3;
      int[] pixels = new int[width * height];
      for (int y = 0; y < height; y++)
        for (int x = 0; x < width; x++)
          pixels[y * width + x] = m.get(x / 3, y / 3) ? 0xff000000 : 0xffffffff;
      var bitmap =
          new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));
      assertEquals(value, new MultiFormatReader().decode(bitmap).getText());
    }
  }

  @Test
  void immutableReceiptAndDownload() throws Exception {
    System.setProperty(
        "storage.path", Files.createTempDirectory("lumina-receipt-test-").toString());
    System.setProperty("db.mode", "demo");
    System.setProperty("payment.mode", "demo");
    JdbcRepository.init();
    try {
      ApplicationInitializer.schema();
      DatabaseMigrations.run();
      ApplicationInitializer.seed();
      long uid =
          JdbcRepository.id(
              JdbcRepository.one("SELECT id FROM users WHERE username='reader'"), "id");
      JdbcRepository.update("UPDATE books SET isbn='9780306406157' WHERE id=1");
      var p = PaymentService.createOrder(uid, 1, 0, 7);
      PaymentService.settle(p.get("order_ref").toString(), "receipt-test", "VISA", true);
      long id = JdbcRepository.id(p, "id");
      var receipt = ReceiptService.forOwner(id, uid);
      assertNotNull(receipt.get("receipt_due"));
      assertEquals("9780306406157", receipt.get("isbn"));
      JdbcRepository.update(
          "UPDATE books SET title='Changed title',isbn='9780140449136' WHERE id=1");
      JdbcRepository.update("UPDATE users SET name='Changed name' WHERE id=?", uid);
      DatabaseMigrations.run();
      receipt = ReceiptService.forOwner(id, uid);
      assertEquals("The Art of Paying Attention", receipt.get("title"));
      assertEquals("Demo Reader", receipt.get("name"));
      assertEquals("9780306406157", receipt.get("isbn"));
      assertNull(ReceiptService.forOwner(id, 999));
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ReceiptService.pdf(receipt, bytes);
      try (PDDocument doc = PDDocument.load(bytes.toByteArray())) {
        assertEquals(1, doc.getNumberOfPages());
        String content = new PDFTextStripper().getText(doc);
        assertTrue(content.contains("TOTAL PAID"));
        assertFalse(content.contains("9780306406157"));
        assertTrue(content.contains("NO MONEY CHARGED"));
        var rendered = new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0, 144);
        javax.imageio.ImageIO.write(
            rendered, "png", Path.of("target/receipt-preview.png").toFile());
        int w = rendered.getWidth(), h = rendered.getHeight();
        var bitmap =
            new BinaryBitmap(
                new HybridBinarizer(
                    new RGBLuminanceSource(w, h, rendered.getRGB(0, 0, w, h, null, 0, w))));
        assertThrows(
            NotFoundException.class,
            () ->
                new MultiFormatReader()
                    .decode(
                        bitmap,
                        Map.of(
                            DecodeHintType.TRY_HARDER,
                            true,
                            DecodeHintType.POSSIBLE_FORMATS,
                            List.of(BarcodeFormat.EAN_13))));
      }
    } finally {
      JdbcRepository.close();
    }
  }
}

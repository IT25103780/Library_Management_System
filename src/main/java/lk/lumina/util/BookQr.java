package lk.lumina.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.Map;

/** A compact, self-contained QR linking to this book's detail page. */
public final class BookQr {
  private BookQr() {}

  public static String html(String url) {
    try {
      var matrix =
          new QRCodeWriter()
              .encode(url, BarcodeFormat.QR_CODE, 0, 0, Map.of(EncodeHintType.MARGIN, 4));
      StringBuilder svg =
          new StringBuilder(
              "<div class=\"book-qr\"><svg xmlns=\"http://www.w3.org/2000/svg\" role=\"img\""
                  + " aria-label=\"QR code for this book\" width=\"88\" height=\"88\" viewBox=\"0 0"
                  + " "
                  + matrix.getWidth()
                  + " "
                  + matrix.getHeight()
                  + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\""
                  + " fill=\"white\"/><path fill=\"black\" d=\"");
      for (int y = 0; y < matrix.getHeight(); y++)
        for (int x = 0; x < matrix.getWidth(); x++)
          if (matrix.get(x, y)) svg.append("M").append(x).append(" ").append(y).append("h1v1h-1z");
      return svg.append(
              "\"/></svg><span>Keep this book close.<small>Scan to open its"
                  + " page</small></span></div>")
          .toString();
    } catch (Exception e) {
      return "";
    }
  }
}

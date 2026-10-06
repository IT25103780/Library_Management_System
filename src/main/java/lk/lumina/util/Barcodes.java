package lk.lumina.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lk.lumina.security.Security;

/** ISBN validation and server-rendered, printable vector barcodes. */
public final class Barcodes {
  public static String isbn(String raw) {
    String v = Objects.toString(raw, "").replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    if (v.matches("[0-9]{9}[0-9X]")) {
      int sum = 0;
      for (int i = 0; i < 10; i++) sum += (10 - i) * (v.charAt(i) == 'X' ? 10 : v.charAt(i) - '0');
      if (sum % 11 != 0) throw new IllegalArgumentException("ISBN check digit is incorrect.");
      v = "978" + v.substring(0, 9);
      v += check(v);
    }
    if (!v.matches("97[89][0-9]{10}") || check(v.substring(0, 12)) != v.charAt(12) - '0')
      throw new IllegalArgumentException(
          "Enter a valid ISBN-10 or ISBN-13, including its check digit.");
    return v;
  }

  private static int check(String v) {
    int n = 0;
    for (int i = 0; i < 12; i++) n += (v.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
    return (10 - n % 10) % 10;
  }

  public static boolean valid(Object raw) {
    try {
      isbn(Objects.toString(raw, ""));
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static String value(Object raw) {
    return valid(raw) ? isbn(raw.toString()) : Objects.toString(raw, "");
  }

  public static BitMatrix matrix(Object raw) {
    String v = value(raw);
    if (v.isBlank() || v.length() > 40 || !v.matches("[ -~]+")) return null;
    try {
      return new MultiFormatWriter()
          .encode(
              v,
              valid(raw) ? BarcodeFormat.EAN_13 : BarcodeFormat.CODE_128,
              0,
              64,
              Map.of(EncodeHintType.MARGIN, 24));
    } catch (Exception e) {
      return null;
    }
  }

  public static String html(Object raw) {
    boolean official = valid(raw);
    String v = value(raw);
    BitMatrix m = matrix(raw);
    if (m == null) return "<div class='isbn-panel'><small>ISBN needs correction</small></div>";
    StringBuilder s =
        new StringBuilder(
            "<div class='isbn-panel'><span class='isbn-label'>"
                + (official ? "ISBN-13" : "LIBRARY REFERENCE · ISBN NOT ASSIGNED")
                + "</span><svg class='isbn-bars' role='img' aria-label='"
                + Security.esc((official ? "ISBN " : "Library reference ") + v)
                + "' viewBox='0 0 "
                + m.getWidth()
                + " 84' xmlns='http://www.w3.org/2000/svg'><rect width='100%' height='100%'"
                + " fill='white'/><g fill='black'>");
    for (int x = 0; x < m.getWidth(); x++)
      if (m.get(x, 0)) s.append("<rect x='").append(x).append("' y='5' width='1' height='57'/>");
    return s.append(
            "</g><text x='50%' y='78' text-anchor='middle' font-family='Arial,sans-serif'"
                + " font-size='11' fill='black'>")
        .append(Security.esc(v))
        .append("</text></svg></div>")
        .toString();
  }
}

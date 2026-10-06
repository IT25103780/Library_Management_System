package lk.lumina.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lk.lumina.security.Security;

/** Formatting shared by JSP pages, receipts and notifications. */
public final class ViewUtils {
  public static String e(Object o) {
    return Security.esc(o);
  }

  public static String money(Object o) {
    return "LKR "
        + String.format(
            Locale.US, "%,.2f", o == null ? BigDecimal.ZERO : new BigDecimal(o.toString()));
  }

  public static String date(Object o) {
    return o == null
        ? "â€”"
        : ((Timestamp) o)
            .toInstant()
            .atZone(ZoneId.of("Asia/Colombo"))
            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
  }

  public static String text(Map<String, Object> r, String key) {
    return Objects.toString(r.get(key), "");
  }
}

package lk.lumina.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Security {
  public static String token() {
    byte[] b = new byte[32];
    new SecureRandom().nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public static String hash(String p) throws Exception {
    String salt = token();
    return "pbkdf2$210000$" + salt + "$" + derive(p, salt, 210000);
  }

  private static String derive(String p, String s, int n) throws Exception {
    return Base64.getEncoder()
        .encodeToString(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(
                    new PBEKeySpec(
                        p.toCharArray(),
                        s.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        n,
                        256))
                .getEncoded());
  }

  public static boolean verify(String p, String h) throws Exception {
    String[] a = h.split("\\$");
    return a.length == 4
        && MessageDigest.isEqual(
            derive(p, a[2], Integer.parseInt(a[1])).getBytes(), a[3].getBytes());
  }

  public static String digest(String text, String algorithm) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance(algorithm)
                .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
        .toUpperCase(Locale.ROOT);
  }

  public static void password(String p) {
    if (p == null
        || p.length() < 10
        || p.length() > 128
        || !p.matches("(?s).*[A-Z].*")
        || !p.matches("(?s).*[a-z].*")
        || !p.matches("(?s).*[0-9].*")
        || !p.matches("(?s).*[^A-Za-z0-9\\s].*"))
      throw new IllegalArgumentException(
          "Use 10–128 characters with an uppercase letter, lowercase letter, number and special"
              + " character.");
    String simple = p.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    if (Set.of(
            "password123",
            "password1234",
            "qwerty12345",
            "welcome123",
            "letmein1234",
            "1234567890a")
        .contains(simple))
      throw new IllegalArgumentException("Choose a less predictable password.");
  }

  public static String esc(Object v) {
    return v == null
        ? ""
        : v.toString()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
  }
}

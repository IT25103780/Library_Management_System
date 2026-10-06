package lk.lumina.util;

import java.sql.Timestamp;
import java.time.Instant;

/** BusinessRules operations, preserving existing validation and transaction boundaries. */
public final class BusinessRules {
  public static Timestamp now() {
    return Timestamp.from(Instant.now());
  }

  public static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }
}

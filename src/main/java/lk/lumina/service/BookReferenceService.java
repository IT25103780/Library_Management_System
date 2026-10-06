package lk.lumina.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import lk.lumina.repository.BookRepository;
import lk.lumina.repository.JdbcRepository;

/** Persistent catalog numbering, serialized by a database row lock. */
public final class BookReferenceService {
  private static final String KEY = "last_book_reference";

  private static long highest(Connection c) throws Exception {
    long high = 0;
    for (var row : BookRepository.listLibraryReferences(c)) {
      String value = row.get("isbn").toString();
      if (value.matches("LUM-[0-9]+")) high = Math.max(high, Long.parseLong(value.substring(4)));
    }
    return high;
  }

  public static void initialize() throws Exception {
    JdbcRepository.tx(
        c -> {
          if (BookRepository.findReferenceSetting(c, KEY) == null)
            BookRepository.insertReferenceSetting(c, KEY, "0");
          BookRepository.lockReferenceSetting(c, KEY);
          long high =
              Math.max(
                  highest(c),
                  Long.parseLong(
                      BookRepository.findReferenceSetting(c, KEY).get("setting_value").toString()));
          BookRepository.updateReferenceSetting(c, Long.toString(high), KEY);
          return null;
        });
  }

  private static String format(long value) {
    return String.format(Locale.ROOT, "LUM-%04d", value);
  }

  public static String preview() throws Exception {
    try (Connection c = JdbcRepository.connection()) {
      long last =
          Long.parseLong(
              BookRepository.findReferenceSetting(c, KEY).get("setting_value").toString());
      return format(Math.addExact(Math.max(last, highest(c)), 1));
    }
  }

  public static String next(Connection c) throws Exception {
    BookRepository.lockReferenceSetting(c, KEY);
    long last =
        Long.parseLong(BookRepository.findReferenceSetting(c, KEY).get("setting_value").toString());
    long next = Math.addExact(Math.max(last, highest(c)), 1);
    BookRepository.updateReferenceSetting(c, Long.toString(next), KEY);
    return format(next);
  }

  public static <T> T transaction(JdbcRepository.Job<T> job) throws Exception {
    for (int attempt = 0; ; attempt++)
      try {
        return JdbcRepository.tx(job);
      } catch (SQLException e) {
        if (attempt >= 4 || !("40001".equals(e.getSQLState()) || e.getErrorCode() == 1205)) throw e;
        Thread.sleep(25L * (attempt + 1));
      }
  }
}

package lk.lumina.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Report SQL operations. Connection overloads participate in the caller's existing transaction. */
public final class ReportRepository {
  public static List<Map<String, Object>> queryReport(
      String fragment1, String fragment2, Object... values) throws SQLException {
    return JdbcRepository.list(fragment1 + fragment2, values);
  }

  public static List<Map<String, Object>> listAvailableBranches(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list("SELECT * FROM branches WHERE active=1" + fragment1, values);
  }

  public static List<Map<String, Object>> listActiveCategories(Object... values)
      throws SQLException {
    return JdbcRepository.list("SELECT * FROM categories WHERE active=1", values);
  }
}

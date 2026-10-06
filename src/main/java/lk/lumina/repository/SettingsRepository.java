package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Settings SQL operations. Connection overloads participate in the caller's existing transaction.
 */
public final class SettingsRepository {

  public static Map<String, Object> findByKey(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT setting_value FROM settings WHERE setting_key=?", values);
  }
}

package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Administration SQL operations. Connection overloads participate in the caller's existing
 * transaction.
 */
public final class AdministrationRepository {
  public static List<Map<String, Object>> searchAuditLog(Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT TOP 1000 a.id,u.username,a.action,a.details,a.created_at FROM audit_log a LEFT JOIN"
            + " users u ON u.id=a.user_id WHERE (a.action LIKE ? OR a.details LIKE ? OR u.username"
            + " LIKE ?) ORDER BY a.id DESC",
        values);
  }

  public static List<Map<String, Object>> listSettings(Object... values) throws SQLException {
    return JdbcRepository.list("SELECT * FROM settings", values);
  }

  public static List<Map<String, Object>> listRecentAudit(Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT TOP 50 a.id,u.username,a.action,a.details,a.created_at FROM audit_log a LEFT JOIN"
            + " users u ON u.id=a.user_id ORDER BY a.id DESC",
        values);
  }

  public static long updateSetting(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE settings SET setting_value=? WHERE setting_key=?", values);
  }

  public static Map<String, Object> findAccount(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT id FROM users WHERE id=?", values);
  }

  public static long updatePassword(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(connection, "UPDATE users SET password_hash=? WHERE id=?", values);
  }

  public static long invalidateResetTokens(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE reset_tokens SET used=1 WHERE user_id=?", values);
  }

  public static List<Map<String, Object>> listActiveAccounts(
      Connection connection, Object... values) throws SQLException {
    return JdbcRepository.list(connection, "SELECT id FROM users WHERE active=1", values);
  }
}

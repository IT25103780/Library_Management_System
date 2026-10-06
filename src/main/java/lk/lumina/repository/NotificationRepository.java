package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Notification SQL operations. Connection overloads participate in the caller's existing
 * transaction.
 */
public final class NotificationRepository {
  public static Map<String, Object> unreadSummary(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT COUNT(*) AS n,COALESCE(MAX(id),0) AS latest FROM notifications WHERE user_id=? AND"
            + " is_read=0",
        values);
  }

  public static List<Map<String, Object>> listForMember(Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT TOP 100 * FROM notifications WHERE user_id=? ORDER BY id DESC", values);
  }

  public static long markAllRead(String fragment1, Object... values) throws SQLException {
    return JdbcRepository.update(
        "UPDATE notifications SET is_read=1 WHERE user_id=?" + fragment1, values);
  }

  public static List<Map<String, Object>> listPendingEmails(Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT TOP 20 n.*,u.email FROM notifications n JOIN users u ON u.id=n.user_id WHERE"
            + " n.email_sent=0 ORDER BY n.id",
        values);
  }

  public static long markEmailSent(Object... values) throws SQLException {
    return JdbcRepository.update("UPDATE notifications SET email_sent=1 WHERE id=?", values);
  }

  public static Map<String, Object> findByDedupeKey(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM notifications WHERE dedupe_key=?", values);
  }

  public static long insertNotification(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO notifications(user_id,title,message,link,dedupe_key,created_at)"
            + " VALUES(?,?,?,?,?,?)",
        values);
  }
}

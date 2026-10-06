package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Reminder SQL operations. Connection overloads participate in the caller's existing transaction.
 */
public final class ReminderRepository {

  public static List<Map<String, Object>> listLoansDueSoon(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.list(
        connection,
        "SELECT * FROM loans WHERE status IN ('ACTIVE','OVERDUE') AND due_at<=?",
        values);
  }

  public static long updateLoanStatus(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(connection, "UPDATE loans SET status=? WHERE id=?", values);
  }

  public static List<Map<String, Object>> listExpiredReservations(
      Connection connection, Object... values) throws SQLException {
    return JdbcRepository.list(
        connection, "SELECT * FROM reservations WHERE status='READY' AND ready_until<?", values);
  }

  public static long expireReservation(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE reservations SET status='EXPIRED' WHERE id=?", values);
  }

  public static long releaseCopy(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE copies SET status='AVAILABLE' WHERE id=?", values);
  }

  public static long deleteExpiredResetTokens(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(connection, "DELETE FROM reset_tokens WHERE expires_at<?", values);
  }
}

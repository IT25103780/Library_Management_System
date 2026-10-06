package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Inventory SQL operations. Connection overloads participate in the caller's existing transaction.
 */
public final class InventoryRepository {

  public static Map<String, Object> findCopy(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM copies WHERE id=?", values);
  }

  public static long updateCondition(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(connection, "UPDATE copies SET status=? WHERE id=?", values);
  }

  public static Map<String, Object> findLoanHistory(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT id FROM loans WHERE copy_id=?", values);
  }

  public static Map<String, Object> findReservationHistory(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT id FROM reservations WHERE copy_id=?", values);
  }

  public static long deleteCopy(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(connection, "DELETE FROM copies WHERE id=?", values);
  }

  public static Map<String, Object> findActiveBranch(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM branches WHERE id=? AND active=1", values);
  }

  public static long updateLocation(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE copies SET branch_id=?,shelf=? WHERE id=?", values);
  }
}

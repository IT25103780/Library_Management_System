package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Reservation SQL operations. Connection overloads participate in the caller's existing
 * transaction.
 */
public final class ReservationRepository {
  public static List<Map<String, Object>> listActiveBranches(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list("SELECT * FROM branches WHERE active=1" + fragment1, values);
  }

  public static List<Map<String, Object>> listVisibleReservations(
      String fragment1, Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT r.*,b.title,u.name,br.name AS branch FROM reservations r JOIN books b ON"
            + " b.id=r.book_id JOIN users u ON u.id=r.user_id JOIN branches br ON br.id=r.branch_id"
            + " WHERE "
            + fragment1
            + " ORDER BY r.id DESC",
        values);
  }

  public static Map<String, Object> findActiveMember(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT id FROM users WHERE id=? AND active=1", values);
  }

  public static Map<String, Object> findPhysicalBook(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT * FROM books WHERE id=? AND active=1 AND format<>'DIGITAL'", values);
  }

  public static Map<String, Object> findActiveBranch(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM branches WHERE id=? AND active=1", values);
  }

  public static Map<String, Object> findOpenMemberReservation(
      Connection connection, Object... values) throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT id FROM reservations WHERE user_id=? AND book_id=? AND status IN"
            + " ('WAITING','READY')",
        values);
  }

  public static long insertReservation(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO reservations(user_id,book_id,branch_id,created_at) VALUES(?,?,?,?)",
        values);
  }

  public static List<Map<String, Object>> listBranchStaff(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.list(
        connection,
        "SELECT id FROM users WHERE active=1 AND (role IN ('ADMIN','LIBRARIAN') OR"
            + " (role='BRANCH_MANAGER' AND branch_id=?))",
        values);
  }

  public static Map<String, Object> findById(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM reservations WHERE id=?", values);
  }

  public static long releaseCopy(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE copies SET status='AVAILABLE' WHERE id=?", values);
  }

  public static long cancelReservation(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE reservations SET status='CANCELLED' WHERE id=?", values);
  }

  public static List<Map<String, Object>> listWaitingQueue(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.list(
        connection,
        "SELECT r.* FROM reservations r JOIN users u ON u.id=r.user_id JOIN books b ON"
            + " b.id=r.book_id WHERE r.status='WAITING' AND u.active=1 AND b.active=1 AND"
            + " EXISTS(SELECT 1 FROM branches br WHERE br.id=r.branch_id AND br.active=1) ORDER BY"
            + " r.created_at,r.id",
        values);
  }

  public static Map<String, Object> findAvailableCopy(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT TOP 1 * FROM copies WHERE book_id=? AND branch_id=? AND status='AVAILABLE' ORDER BY"
            + " id",
        values);
  }

  public static long reserveCopy(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE copies SET status='RESERVED' WHERE id=?", values);
  }

  public static long markReady(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "UPDATE reservations SET status='READY',copy_id=?,ready_until=? WHERE id=?",
        values);
  }

  public static long changeBranch(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "UPDATE reservations SET"
            + " branch_id=?,copy_id=NULL,status='WAITING',ready_until=NULL,created_at=? WHERE id=?",
        values);
  }
}

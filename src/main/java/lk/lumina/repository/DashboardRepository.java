package lk.lumina.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Dashboard SQL operations. Connection overloads participate in the caller's existing transaction.
 */
public final class DashboardRepository {
  public static List<Map<String, Object>> listFeaturedBooks(Object... values) throws SQLException {
    return JdbcRepository.list(
        BookRepository.catalogSelect()
            + " WHERE b.active=1 ORDER BY b.id OFFSET 0 ROWS FETCH NEXT 6 ROWS ONLY",
        values);
  }

  public static List<Map<String, Object>> listActiveBranches(Object... values) throws SQLException {
    return JdbcRepository.list("SELECT * FROM branches WHERE active=1", values);
  }

  public static Map<String, Object> countActiveReaders(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        "SELECT COUNT(*) AS n FROM users WHERE role='READER' AND active=1" + fragment1, values);
  }

  public static Map<String, Object> countCopies(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.one("SELECT COUNT(*) AS n FROM copies WHERE 1=1" + fragment1, values);
  }

  public static Map<String, Object> countOverdueLoans(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        "SELECT COUNT(*) AS n FROM loans WHERE kind='PHYSICAL' AND status IN ('ACTIVE','OVERDUE')"
            + " AND due_at<?"
            + fragment1,
        values);
  }

  public static Map<String, Object> countOpenReservations(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        "SELECT COUNT(*) AS n FROM reservations WHERE status IN ('WAITING','READY')" + fragment1,
        values);
  }

  public static Map<String, Object> countActiveBooks(Object... values) throws SQLException {
    return JdbcRepository.one("SELECT COUNT(*) AS n FROM books WHERE active=1", values);
  }

  public static Map<String, Object> countPhysicalLoans(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        "SELECT COUNT(*) AS n FROM loans WHERE kind='PHYSICAL' AND status IN ('ACTIVE','OVERDUE')"
            + fragment1,
        values);
  }

  public static Map<String, Object> countDigitalLoans(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT COUNT(*) AS n FROM loans WHERE kind='DIGITAL' AND status='ACTIVE' AND due_at>?",
        values);
  }

  public static Map<String, Object> totalRevenue(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT COALESCE(SUM(amount),0) AS n FROM payments WHERE status='SUCCESSFUL'", values);
  }

  public static List<Map<String, Object>> listRecentNotifications(Object... values)
      throws SQLException {
    return JdbcRepository.list(
        "SELECT TOP 8 n.* FROM notifications n WHERE user_id=? ORDER BY id DESC", values);
  }

  public static List<Map<String, Object>> listPopularBooks(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list(
        "SELECT TOP 5 b.title,COUNT(l.id) AS n FROM books b LEFT JOIN loans l ON l.book_id=b.id"
            + fragment1
            + " GROUP BY b.title ORDER BY n DESC",
        values);
  }
}

package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Fine SQL operations. Connection overloads participate in the caller's existing transaction. */
public final class FineRepository {
  public static Map<String, Object> findFineDetails(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT f.*,u.name,b.title,cp.branch_id FROM fines f JOIN users u ON u.id=f.user_id JOIN"
            + " loans l ON l.id=f.loan_id JOIN books b ON b.id=l.book_id JOIN copies cp ON"
            + " cp.id=l.copy_id WHERE f.id=?",
        values);
  }

  public static List<Map<String, Object>> listPaymentHistory(Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT id,amount,method,provider,provider_ref,status,created_at,paid_at FROM payments"
            + " WHERE fine_id=? ORDER BY id DESC",
        values);
  }

  public static List<Map<String, Object>> listFineBalances(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list(
        "SELECT f.*,u.name,b.title,(f.amount-(SELECT COALESCE(SUM(pay.amount),0) FROM payments pay"
            + " WHERE pay.fine_id=f.id AND pay.status='SUCCESSFUL')) AS balance FROM fines f JOIN"
            + " users u ON u.id=f.user_id JOIN loans l ON l.id=f.loan_id JOIN books b ON"
            + " b.id=l.book_id WHERE "
            + fragment1
            + " ORDER BY f.id DESC",
        values);
  }

  public static Map<String, Object> findSuccessfulOfflinePayment(
      Connection connection, Object... values) throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT p.*,cp.branch_id FROM payments p JOIN fines f ON f.id=p.fine_id JOIN loans l ON"
            + " l.id=f.loan_id JOIN copies cp ON cp.id=l.copy_id WHERE p.id=? AND"
            + " p.provider='OFFLINE' AND p.status='SUCCESSFUL'",
        values);
  }

  public static long markPaymentRefunded(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE payments SET status='REFUNDED',provider_ref=? WHERE id=?", values);
  }

  public static long correctPaymentMethod(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE payments SET method=?,provider_ref=? WHERE id=?", values);
  }

  public static Map<String, Object> totalPaid(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT COALESCE(SUM(amount),0) AS n FROM payments WHERE fine_id=? AND status='SUCCESSFUL'",
        values);
  }

  public static Map<String, Object> findAmount(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT amount FROM fines WHERE id=?", values);
  }

  public static Map<String, Object> findById(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM fines WHERE id=?", values);
  }

  public static long updateStatus(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(connection, "UPDATE fines SET status=? WHERE id=?", values);
  }

  public static Map<String, Object> findByLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM fines WHERE loan_id=?", values);
  }

  public static long insertFine(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO fines(user_id,loan_id,amount,reason,created_at) VALUES(?,?,?,?,?)",
        values);
  }

  public static long updateAssessment(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE fines SET amount=?,reason=? WHERE id=?", values);
  }

  public static List<Map<String, Object>> listOverdueLoans(
      Connection connection, String fragment1, Object... values) throws SQLException {
    return JdbcRepository.list(
        connection,
        "SELECT l.* FROM loans l JOIN copies cp ON cp.id=l.copy_id WHERE l.kind='PHYSICAL' AND"
            + " l.status IN ('ACTIVE','OVERDUE') AND l.due_at<?"
            + fragment1,
        values);
  }

  public static Map<String, Object> findFineWithBranch(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT f.*,cp.branch_id FROM fines f JOIN loans l ON l.id=f.loan_id JOIN copies cp ON"
            + " cp.id=l.copy_id WHERE f.id=?",
        values);
  }

  public static Map<String, Object> findPendingPayment(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM payments WHERE fine_id=? AND status='PENDING'", values);
  }

  public static long insertOfflinePayment(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO"
            + " payments(order_ref,user_id,fine_id,amount,provider,method,provider_ref,status,created_at,paid_at)"
            + " VALUES(?,?,?,?,'OFFLINE',?,?,'SUCCESSFUL',?,?)",
        values);
  }

  public static long adjustFine(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE fines SET amount=?,reason=?,adjusted=1,status=? WHERE id=?", values);
  }
}

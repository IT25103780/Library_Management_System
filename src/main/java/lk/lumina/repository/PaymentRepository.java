package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Payment SQL operations. Connection overloads participate in the caller's existing transaction.
 */
public final class PaymentRepository {
  public static Map<String, Object> findMemberPayment(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT p.*,b.title,b.isbn FROM payments p LEFT JOIN books b ON b.id=p.book_id WHERE p.id=?"
            + " AND p.user_id=?",
        values);
  }

  public static List<Map<String, Object>> listAllPayments(Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT p.*,u.name,b.title FROM payments p JOIN users u ON u.id=p.user_id LEFT JOIN books b"
            + " ON b.id=p.book_id ORDER BY p.id DESC",
        values);
  }

  public static List<Map<String, Object>> listMemberPayments(Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT"
            + " p.id,p.order_ref,b.title,p.amount,p.provider,p.method,p.status,p.created_at,p.paid_at"
            + " FROM payments p LEFT JOIN books b ON b.id=p.book_id WHERE p.user_id=? ORDER BY p.id"
            + " DESC",
        values);
  }

  public static Map<String, Object> findAccessibleReceipt(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        "SELECT p.*,COALESCE(p.receipt_name,u.name) AS name,COALESCE(p.receipt_email,u.email) AS"
            + " email,COALESCE(p.receipt_title,b.title) AS title,COALESCE(p.receipt_isbn,b.isbn) AS"
            + " isbn FROM payments p JOIN users u ON u.id=p.user_id LEFT JOIN books b ON"
            + " b.id=p.book_id WHERE p.id=? AND p.status='SUCCESSFUL' AND "
            + fragment1,
        values);
  }

  public static Map<String, Object> findPendingMemberPayment(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT * FROM payments WHERE id=? AND user_id=? AND status='PENDING'", values);
  }

  public static Map<String, Object> findPendingMemberPayment(
      Connection connection, Object... values) throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT * FROM payments WHERE id=? AND user_id=? AND status='PENDING'", values);
  }

  public static long updateContactDetails(Object... values) throws SQLException {
    return JdbcRepository.update("UPDATE users SET phone=?,address=? WHERE id=?", values);
  }

  public static Map<String, Object> findRetryablePayment(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT * FROM payments WHERE id=? AND user_id=? AND status IN ('FAILED','CANCELLED')",
        values);
  }

  public static long cancelPayment(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE payments SET status='CANCELLED' WHERE id=?", values);
  }

  public static Map<String, Object> findById(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM payments WHERE id=?", values);
  }

  public static long recordRefund(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE payments SET status='REFUNDED',provider_ref=? WHERE id=?", values);
  }

  public static Map<String, Object> findLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM loans WHERE id=?", values);
  }

  public static Map<String, Object> findSuccessfulLoanPayment(
      Connection connection, Object... values) throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM payments WHERE loan_id=? AND status='SUCCESSFUL'", values);
  }

  public static long updateRefundedLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE loans SET due_at=?,status=? WHERE id=?", values);
  }

  public static Map<String, Object> findGatewayOrder(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT * FROM payments WHERE order_ref=? AND provider='PAYHERE'", values);
  }

  public static long captureLastFour(Object... values) throws SQLException {
    return JdbcRepository.update(
        "UPDATE payments SET card_last4=? WHERE id=? AND card_last4 IS NULL", values);
  }

  public static Map<String, Object> findOutstandingMemberFine(
      Connection connection, Object... values) throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT * FROM fines WHERE id=? AND user_id=? AND status IN"
            + " ('OUTSTANDING','PARTIALLY_PAID')",
        values);
  }

  public static Map<String, Object> findActiveBook(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM books WHERE id=? AND active=1", values);
  }

  public static Map<String, Object> findActiveDigitalLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT id FROM loans WHERE user_id=? AND book_id=? AND kind='DIGITAL' AND status='ACTIVE'"
            + " AND due_at>?",
        values);
  }

  public static Map<String, Object> findPendingOrder(
      Connection connection, String fragment1, Object... values) throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT TOP 1 * FROM payments WHERE user_id=? AND "
            + fragment1
            + " AND status='PENDING' ORDER BY id DESC",
        values);
  }

  public static long insertOrder(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO"
            + " payments(order_ref,user_id,book_id,fine_id,amount,duration_days,provider,created_at)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        values);
  }

  public static Map<String, Object> findByOrderReference(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM payments WHERE order_ref=?", values);
  }

  public static long markFailed(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "UPDATE payments SET status='FAILED',provider_ref=?,method=? WHERE id=?",
        values);
  }

  public static Map<String, Object> findFine(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM fines WHERE id=?", values);
  }

  public static Map<String, Object> findCurrentDigitalLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT * FROM loans WHERE user_id=? AND book_id=? AND kind='DIGITAL' AND status='ACTIVE'"
            + " AND due_at>?",
        values);
  }

  public static long insertDigitalLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO loans(user_id,book_id,kind,status,start_at,due_at)"
            + " VALUES(?,?,'DIGITAL','ACTIVE',?,?)",
        values);
  }

  public static long extendDigitalLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(connection, "UPDATE loans SET due_at=? WHERE id=?", values);
  }

  public static long markSuccessful(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "UPDATE payments SET status='SUCCESSFUL',provider_ref=?,method=?,loan_id=?,paid_at=? WHERE"
            + " id=?",
        values);
  }
}

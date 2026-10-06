package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Receipt SQL operations. Connection overloads participate in the caller's existing transaction.
 */
public final class ReceiptRepository {

  public static Map<String, Object> findSnapshotSource(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT p.*,u.name,u.email,b.title,b.isbn,l.start_at,l.due_at FROM payments p JOIN users u"
            + " ON u.id=p.user_id LEFT JOIN fines f ON f.id=p.fine_id LEFT JOIN loans fl ON"
            + " fl.id=f.loan_id LEFT JOIN books b ON b.id=COALESCE(p.book_id,fl.book_id) LEFT JOIN"
            + " loans l ON l.id=p.loan_id WHERE p.id=?",
        values);
  }

  public static long captureSnapshot(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "UPDATE payments SET"
            + " receipt_title=?,receipt_isbn=?,receipt_name=?,receipt_email=?,receipt_start=?,receipt_due=?"
            + " WHERE id=? AND receipt_name IS NULL",
        values);
  }

  public static Map<String, Object> findSuccessfulMemberReceipt(Object... values)
      throws SQLException {
    return JdbcRepository.one(
        "SELECT p.*,COALESCE(p.receipt_name,u.name) AS name,COALESCE(p.receipt_email,u.email) AS"
            + " email,COALESCE(p.receipt_title,b.title) AS title,COALESCE(p.receipt_isbn,b.isbn) AS"
            + " isbn FROM payments p JOIN users u ON u.id=p.user_id LEFT JOIN books b ON"
            + " b.id=p.book_id WHERE p.id=? AND p.user_id=? AND p.status='SUCCESSFUL'",
        values);
  }
}

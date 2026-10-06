package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Purchase SQL operations. Connection overloads participate in the caller's existing transaction.
 */
public final class PurchaseRepository {

  public static Map<String, Object> findOpenPurchase(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT * FROM purchases WHERE id=? AND status IN ('ORDERED','PARTIALLY_RECEIVED')",
        values);
  }

  public static long cancelPurchase(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE purchases SET status='CANCELLED' WHERE id=?", values);
  }

  public static long updatePurchase(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "UPDATE purchases SET quantity=?,unit_cost=?,invoice_ref=?,notes=? WHERE id=?",
        values);
  }

  public static Map<String, Object> findActiveSupplier(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM suppliers WHERE id=? AND active=1", values);
  }

  public static Map<String, Object> findActiveBranch(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM branches WHERE id=? AND active=1", values);
  }

  public static Map<String, Object> findSupplierInvoice(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT id FROM purchases WHERE supplier_id=? AND invoice_ref=? AND status<>'CANCELLED'",
        values);
  }

  public static Map<String, Object> findActivePhysicalBook(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM books WHERE id=? AND active=1 AND format<>'DIGITAL'", values);
  }

  public static long insertPurchase(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO"
            + " purchases(supplier_id,book_id,branch_id,quantity,unit_cost,created_at,invoice_ref,notes)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        values);
  }

  public static Map<String, Object> findById(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM purchases WHERE id=?", values);
  }

  public static long insertReceivedCopy(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "INSERT INTO copies(book_id,branch_id) VALUES(?,?)", values);
  }

  public static long updateReceivedQuantity(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE purchases SET received_quantity=?,status=? WHERE id=?", values);
  }
}

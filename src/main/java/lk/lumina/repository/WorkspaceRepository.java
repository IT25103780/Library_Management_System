package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Workspace SQL operations. Connection overloads participate in the caller's existing transaction.
 */
public final class WorkspaceRepository {

  public static long toggleActive(Connection connection, String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection,
        "UPDATE " + fragment1 + " SET active=CASE WHEN active=1 THEN 0 ELSE 1 END WHERE id=?",
        values);
  }

  public static long deleteUnusedRecord(Connection connection, String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.update(connection, "DELETE FROM " + fragment1 + " WHERE id=?", values);
  }

  public static List<Map<String, Object>> listWorkspace(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list(fragment1 + " ORDER BY id DESC", values);
  }

  public static Map<String, Object> findDirectoryRecord(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.one("SELECT * FROM " + fragment1 + " WHERE id=?", values);
  }

  public static Map<String, Object> findEditableAccount(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT id,name,email,username,phone,address,member_type,role,branch_id,active FROM users"
            + " WHERE id=?",
        values);
  }

  public static List<Map<String, Object>> listPhysicalBooks(Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT id,title FROM books WHERE active=1 AND format<>'DIGITAL'", values);
  }

  public static List<Map<String, Object>> listActiveBranches(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list("SELECT * FROM branches WHERE active=1" + fragment1, values);
  }

  public static List<Map<String, Object>> listActiveSuppliers(Object... values)
      throws SQLException {
    return JdbcRepository.list("SELECT * FROM suppliers WHERE active=1", values);
  }

  public static Map<String, Object> findDuplicateName(
      Connection connection, String fragment1, Object... values) throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT id FROM " + fragment1 + " WHERE LOWER(name)=LOWER(?) AND id<>?",
        values);
  }

  public static long insertDirectoryRecord(
      Connection connection, String fragment1, String fragment2, String fragment3, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO " + fragment1 + "(" + fragment2 + ") VALUES(" + fragment3 + ")",
        values);
  }

  public static long updateDirectoryRecord(
      Connection connection, String fragment1, String fragment2, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE " + fragment1 + " SET " + fragment2 + " WHERE id=?", values);
  }

  public static Map<String, Object> findDuplicateAccount(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM users WHERE (email=? OR username=?) AND id<>?", values);
  }

  public static Map<String, Object> findActiveBranch(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM branches WHERE id=? AND active=1", values);
  }

  public static long insertAccount(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO"
            + " users(name,email,username,password_hash,role,branch_id,created_at,phone,address,member_type)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?)",
        values);
  }

  public static long updateAccount(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "UPDATE users SET"
            + " name=?,email=?,username=?,role=?,branch_id=?,phone=?,address=?,member_type=? WHERE"
            + " id=?",
        values);
  }

  public static Map<String, Object> findActivePhysicalBook(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM books WHERE id=? AND active=1 AND format<>'DIGITAL'", values);
  }

  public static Map<String, Object> findActiveSupplier(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM suppliers WHERE id=? AND active=1", values);
  }

  public static long insertPurchase(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO"
            + " purchases(supplier_id,book_id,branch_id,quantity,unit_cost,created_at,invoice_ref,notes)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        values);
  }

  public static long insertCopy(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "INSERT INTO copies(book_id,branch_id) VALUES(?,?)", values);
  }
}

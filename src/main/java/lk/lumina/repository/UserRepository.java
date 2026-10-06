package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** User SQL operations. Connection overloads participate in the caller's existing transaction. */
public final class UserRepository {
  public static Map<String, Object> findMemberDetails(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT id,name,email,phone,address,member_type,branch_id,active FROM users WHERE id=?",
        values);
  }

  public static Map<String, Object> findMemberBranchLoan(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT l.id FROM loans l JOIN copies cp ON cp.id=l.copy_id WHERE l.user_id=? AND"
            + " cp.branch_id=?",
        values);
  }

  public static List<Map<String, Object>> listMemberLoans(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list(
        "SELECT l.id,b.title,l.kind,l.status,l.start_at,l.due_at,l.returned_at FROM loans l JOIN"
            + " books b ON b.id=l.book_id WHERE l.user_id=?"
            + fragment1
            + " ORDER BY l.id DESC",
        values);
  }

  public static List<Map<String, Object>> listMemberFines(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list(
        "SELECT f.id,f.amount,f.status,f.reason FROM fines f JOIN loans l ON l.id=f.loan_id WHERE"
            + " f.user_id=?"
            + fragment1,
        values);
  }

  public static Map<String, Object> findPasswordHash(Object... values) throws SQLException {
    return JdbcRepository.one("SELECT password_hash FROM users WHERE id=?", values);
  }

  public static long updatePassword(Object... values) throws SQLException {
    return JdbcRepository.update("UPDATE users SET password_hash=? WHERE id=?", values);
  }

  public static long updatePassword(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(connection, "UPDATE users SET password_hash=? WHERE id=?", values);
  }

  public static long updateProfile(Object... values) throws SQLException {
    return JdbcRepository.update("UPDATE users SET name=?,phone=?,address=? WHERE id=?", values);
  }

  public static Map<String, Object> findActiveLogin(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT * FROM users WHERE (username=? OR email=?) AND active=1", values);
  }

  public static Map<String, Object> findDuplicateAccount(Object... values) throws SQLException {
    return JdbcRepository.one("SELECT id FROM users WHERE email=? OR username=?", values);
  }

  public static long insertReader(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO users(name,email,username,password_hash,phone,address,role,created_at)"
            + " VALUES(?,?,?,?,?,?,'READER',?)",
        values);
  }

  public static Map<String, Object> findActiveEmail(Object... values) throws SQLException {
    return JdbcRepository.one("SELECT id FROM users WHERE email=? AND active=1", values);
  }

  public static long insertResetToken(Object... values) throws SQLException {
    return JdbcRepository.update(
        "INSERT INTO reset_tokens(user_id,token_hash,expires_at) VALUES(?,?,?)", values);
  }

  public static Map<String, Object> findValidResetToken(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT * FROM reset_tokens WHERE token_hash=? AND used=0 AND expires_at>?",
        values);
  }

  public static long invalidateResetTokens(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE reset_tokens SET used=1 WHERE user_id=?", values);
  }

  public static Map<String, Object> findSessionAccount(Object... values) throws SQLException {
    return JdbcRepository.one(
        "SELECT id,name,email,username,phone,address,role,branch_id,active FROM users WHERE id=?"
            + " AND active=1",
        values);
  }
}

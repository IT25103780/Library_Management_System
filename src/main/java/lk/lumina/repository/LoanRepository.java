package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Loan SQL operations. Connection overloads participate in the caller's existing transaction. */
public final class LoanRepository {
  public static List<Map<String, Object>> listMemberBooks(Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT l.*,b.title,b.isbn,b.author_id,a.name AS author FROM loans l JOIN books b ON"
            + " b.id=l.book_id JOIN authors a ON a.id=b.author_id WHERE l.user_id=? ORDER BY l.id"
            + " DESC",
        values);
  }

  public static long incrementReadCount(Object... values) throws SQLException {
    return JdbcRepository.update("UPDATE loans SET read_count=read_count+1 WHERE id=?", values);
  }

  public static long saveReadingProgress(Object... values) throws SQLException {
    return JdbcRepository.update("UPDATE loans SET page_number=? WHERE id=? AND user_id=?", values);
  }

  public static List<Map<String, Object>> listActiveReaders(Object... values) throws SQLException {
    return JdbcRepository.list(
        "SELECT id,name,email FROM users WHERE active=1 ORDER BY name", values);
  }

  public static List<Map<String, Object>> listIssuableCopies(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list(
        "SELECT c.*,b.title,br.name AS branch FROM copies c JOIN books b ON b.id=c.book_id JOIN"
            + " branches br ON br.id=c.branch_id WHERE b.active=1 AND c.status IN"
            + " ('AVAILABLE','RESERVED')"
            + fragment1
            + " ORDER BY b.title",
        values);
  }

  public static List<Map<String, Object>> listPhysicalLoans(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list(
        "SELECT l.*,b.title,u.name,c.branch_id FROM loans l JOIN books b ON b.id=l.book_id JOIN"
            + " users u ON u.id=l.user_id JOIN copies c ON c.id=l.copy_id WHERE l.kind='PHYSICAL'"
            + fragment1
            + " ORDER BY l.id DESC",
        values);
  }

  public static Map<String, Object> findAuthorizedDigitalLoan(Object... values)
      throws SQLException {
    return JdbcRepository.one(
        "SELECT l.*,b.title,b.pdf_path FROM loans l JOIN books b ON b.id=l.book_id WHERE l.id=? AND"
            + " l.user_id=? AND l.kind='DIGITAL' AND l.status='ACTIVE' AND l.due_at>? AND"
            + " EXISTS(SELECT 1 FROM payments p WHERE p.loan_id=l.id AND p.status='SUCCESSFUL')",
        values);
  }

  public static Map<String, Object> findActiveBorrower(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(connection, "SELECT * FROM users WHERE id=? AND active=1", values);
  }

  public static Map<String, Object> findIssuableCopy(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT c.* FROM copies c JOIN books b ON b.id=c.book_id JOIN branches br ON"
            + " br.id=c.branch_id WHERE c.id=? AND b.active=1 AND br.active=1",
        values);
  }

  public static Map<String, Object> findReadyReservation(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT * FROM reservations WHERE copy_id=? AND user_id=? AND status='READY' AND"
            + " ready_until>?",
        values);
  }

  public static Map<String, Object> countOpenPhysicalLoans(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT COUNT(*) AS n FROM loans WHERE user_id=? AND kind='PHYSICAL' AND status IN"
            + " ('ACTIVE','OVERDUE')",
        values);
  }

  public static Map<String, Object> findOverdueLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT id FROM loans WHERE user_id=? AND kind='PHYSICAL' AND status IN"
            + " ('ACTIVE','OVERDUE') AND due_at<?",
        values);
  }

  public static Map<String, Object> findOutstandingFine(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT id FROM fines WHERE user_id=? AND status IN ('OUTSTANDING','PARTIALLY_PAID')",
        values);
  }

  public static long insertPhysicalLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO loans(user_id,book_id,copy_id,kind,status,start_at,due_at)"
            + " VALUES(?,?,?,'PHYSICAL','ACTIVE',?,?)",
        values);
  }

  public static long markCopyBorrowed(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE copies SET status='BORROWED' WHERE id=?", values);
  }

  public static long fulfillReservation(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE reservations SET status='FULFILLED' WHERE id=?", values);
  }

  public static Map<String, Object> findOpenPhysicalLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT l.*,c.branch_id FROM loans l JOIN copies c ON c.id=l.copy_id WHERE l.id=? AND"
            + " l.kind='PHYSICAL' AND l.status IN ('ACTIVE','OVERDUE')",
        values);
  }

  public static long markReturned(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE loans SET status='RETURNED',returned_at=? WHERE id=?", values);
  }

  public static long releaseCopy(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE copies SET status='AVAILABLE' WHERE id=?", values);
  }

  public static Map<String, Object> findPhysicalLoan(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT l.*,cp.branch_id FROM loans l JOIN copies cp ON cp.id=l.copy_id WHERE l.id=? AND"
            + " l.kind='PHYSICAL'",
        values);
  }

  public static Map<String, Object> findWaitingReservation(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT id FROM reservations WHERE book_id=? AND branch_id=? AND status='WAITING'",
        values);
  }

  public static Map<String, Object> findUnresolvedFine(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection,
        "SELECT id FROM fines WHERE loan_id=? AND status NOT IN ('VOID','WAIVED')",
        values);
  }

  public static long extendDueDate(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE loans SET due_at=?,status='ACTIVE' WHERE id=?", values);
  }

  public static long markVoided(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(connection, "UPDATE loans SET status='VOID' WHERE id=?", values);
  }
}

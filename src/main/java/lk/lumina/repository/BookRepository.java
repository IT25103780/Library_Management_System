package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Book SQL operations. Connection overloads participate in the caller's existing transaction. */
public final class BookRepository {
  public static String catalogSelect() {
    return "SELECT b.*,a.name AS author,c.name AS category,p.name AS publisher,(SELECT COUNT(*)"
        + " FROM copies cp WHERE cp.book_id=b.id AND cp.status='AVAILABLE') AS available"
        + " FROM books b JOIN authors a ON a.id=b.author_id JOIN categories c ON"
        + " c.id=b.category_id JOIN publishers p ON p.id=b.publisher_id";
  }

  public static Map<String, Object> countCatalog(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        "SELECT COUNT(*) AS n FROM books b JOIN authors a ON a.id=b.author_id JOIN publishers p ON"
            + " p.id=b.publisher_id"
            + fragment1,
        values);
  }

  public static List<Map<String, Object>> searchCatalog(
      String fragment1, String fragment2, String fragment3, Object... values) throws SQLException {
    return JdbcRepository.list(
        BookRepository.catalogSelect()
            + fragment1
            + " ORDER BY "
            + fragment2
            + " OFFSET "
            + fragment3
            + " ROWS FETCH NEXT 12 ROWS ONLY",
        values);
  }

  public static List<Map<String, Object>> listActiveCategories(Object... values)
      throws SQLException {
    return JdbcRepository.list("SELECT * FROM categories WHERE active=1 ORDER BY name", values);
  }

  public static List<Map<String, Object>> listActiveBranches(Object... values) throws SQLException {
    return JdbcRepository.list("SELECT * FROM branches WHERE active=1", values);
  }

  public static Map<String, Object> findCatalogBook(Object... values) throws SQLException {
    return JdbcRepository.one(
        BookRepository.catalogSelect() + " WHERE b.id=? AND b.active=1", values);
  }

  public static List<Map<String, Object>> listBranchAvailability(Object... values)
      throws SQLException {
    return JdbcRepository.list(
        "SELECT br.*, (SELECT COUNT(*) FROM copies c WHERE c.book_id=? AND c.branch_id=br.id AND"
            + " c.status='AVAILABLE') AS available FROM branches br WHERE br.active=1",
        values);
  }

  public static Map<String, Object> findById(Object... values) throws SQLException {
    return JdbcRepository.one("SELECT * FROM books WHERE id=?", values);
  }

  public static List<Map<String, Object>> listActiveDirectory(String fragment1, Object... values)
      throws SQLException {
    return JdbcRepository.list(
        "SELECT * FROM " + fragment1 + " WHERE active=1 ORDER BY name", values);
  }

  public static Map<String, Object> findActiveDirectoryRecord(
      Connection connection, String fragment1, Object... values) throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM " + fragment1 + " WHERE id=? AND active=1", values);
  }

  public static long insertBook(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO"
            + " books(title,isbn,author_id,category_id,publisher_id,description,publication_year,edition,format,fee,duration_days,cover_path,pdf_path,created_at)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        values);
  }

  public static long updateBook(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "UPDATE books SET"
            + " title=?,isbn=?,author_id=?,category_id=?,publisher_id=?,description=?,publication_year=?,edition=?,format=?,fee=?,duration_days=?,cover_path=?,pdf_path=?"
            + " WHERE id=?",
        values);
  }

  public static Map<String, Object> findActiveBranch(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT id FROM branches WHERE id=? AND active=1", values);
  }

  public static long insertCopy(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection, "INSERT INTO copies(book_id,branch_id) VALUES(?,?)", values);
  }

  public static Map<String, Object> findCover(Object... values) throws SQLException {
    return JdbcRepository.one("SELECT cover_path FROM books WHERE id=?", values);
  }

  public static List<Map<String, Object>> listLibraryReferences(
      Connection connection, Object... values) throws SQLException {
    return JdbcRepository.list(
        connection, "SELECT isbn FROM books WHERE isbn LIKE 'LUM-%'", values);
  }

  public static Map<String, Object> findReferenceSetting(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.one(
        connection, "SELECT setting_value FROM settings WHERE setting_key=?", values);
  }

  public static long insertReferenceSetting(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "INSERT INTO settings(setting_key,setting_value) VALUES(?,?)", values);
  }

  public static long lockReferenceSetting(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE settings SET setting_value=setting_value WHERE setting_key=?", values);
  }

  public static long updateReferenceSetting(Connection connection, Object... values)
      throws SQLException {
    return JdbcRepository.update(
        connection, "UPDATE settings SET setting_value=? WHERE setting_key=?", values);
  }
}

package lk.lumina.repository;

import java.sql.Connection;
import java.sql.SQLException;

/** Audit SQL operations. Connection overloads participate in the caller's existing transaction. */
public final class AuditRepository {

  public static long insertAuditEvent(Connection connection, Object... values) throws SQLException {
    return JdbcRepository.update(
        connection,
        "INSERT INTO audit_log(user_id,action,details,created_at) VALUES(?,?,?,?)",
        values);
  }
}

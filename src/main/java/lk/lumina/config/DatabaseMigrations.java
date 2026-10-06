package lk.lumina.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.service.BookReferenceService;
import lk.lumina.service.ReceiptService;

/** Additive migrations keep databases from earlier releases usable. */
public final class DatabaseMigrations {
  public static void run() throws Exception {
    try (Connection c = JdbcRepository.connection()) {
      add(c, "payments", "receipt_title", "nvarchar(240)");
      add(c, "payments", "receipt_isbn", "nvarchar(40)");
      add(c, "payments", "receipt_name", "nvarchar(180)");
      add(c, "payments", "receipt_email", "nvarchar(180)");
      add(c, "payments", "receipt_start", "datetime2");
      add(c, "payments", "receipt_due", "datetime2");
      add(c, "payments", "card_last4", "varchar(4)");
      add(c, "users", "member_type", "varchar(20) NOT NULL DEFAULT 'STUDENT'");
      add(c, "copies", "shelf", "nvarchar(100)");
      add(c, "purchases", "received_quantity", "int NOT NULL DEFAULT 0");
      add(c, "purchases", "invoice_ref", "nvarchar(100)");
      add(c, "purchases", "notes", "nvarchar(500)");
      add(c, "fines", "adjusted", "int NOT NULL DEFAULT 0");
      for (var payment :
          JdbcRepository.list(
              c, "SELECT id FROM payments WHERE status='SUCCESSFUL' AND receipt_name IS NULL"))
        ReceiptService.capture(c, JdbcRepository.id(payment, "id"));
      JdbcRepository.update(
          c,
          "UPDATE purchases SET received_quantity=quantity WHERE status='RECEIVED' AND"
              + " received_quantity=0");
      for (String[] pair :
          new String[][] {
            {"physical_days", "14"},
            {"max_loans", "5"},
            {"fine_per_day", "20"},
            {"reservation_hours", "48"}
          })
        if (JdbcRepository.one(c, "SELECT setting_key FROM settings WHERE setting_key=?", pair[0])
            == null)
          JdbcRepository.update(
              c, "INSERT INTO settings(setting_key,setting_value) VALUES(?,?)", pair[0], pair[1]);
    }
    BookReferenceService.initialize();
  }

  private static void add(Connection c, String table, String column, String type) throws Exception {
    boolean found = false;
    try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), c.getSchema(), table, null)) {
      while (r.next()) if (column.equalsIgnoreCase(r.getString("COLUMN_NAME"))) found = true;
    }
    if (!found)
      try (Statement s = c.createStatement()) {
        s.execute("ALTER TABLE " + table + " ADD " + column + " " + type);
      }
  }
}

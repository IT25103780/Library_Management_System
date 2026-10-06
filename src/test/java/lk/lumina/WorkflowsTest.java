package lk.lumina;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lk.lumina.config.ApplicationInitializer;
import lk.lumina.config.DatabaseMigrations;
import lk.lumina.dto.PurchaseItem;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.security.Security;
import lk.lumina.service.CirculationService;
import lk.lumina.service.FineService;
import lk.lumina.service.InventoryService;
import lk.lumina.service.PaymentGatewayService;
import lk.lumina.service.ProcurementService;
import lk.lumina.service.ReservationService;
import lk.lumina.util.BusinessRules;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkflowsTest {
  static long actor, member;
  long copy, loan;

  @BeforeAll
  static void setup() throws Exception {
    System.setProperty("storage.path", Files.createTempDirectory("lumina-workflow-").toString());
    System.setProperty("db.mode", System.getProperty("test.db.mode", "demo"));
    if (System.getProperty("test.db.url") != null)
      System.setProperty("db.url", System.getProperty("test.db.url"));
    JdbcRepository.init();
    ApplicationInitializer.schema();
    DatabaseMigrations.run();
    ApplicationInitializer.seed();
    DatabaseMigrations.run();
    actor =
        JdbcRepository.id(JdbcRepository.one("SELECT id FROM users WHERE username='admin'"), "id");
    String name = "workflow" + System.nanoTime();
    member =
        JdbcRepository.update(
            "INSERT INTO users(name,email,username,password_hash,role,created_at)"
                + " VALUES(?,?,?,?,'READER',?)",
            name,
            name + "@test.local",
            name,
            Security.hash("Workflow@2026"),
            BusinessRules.now());
  }

  @AfterAll
  static void stop() {
    JdbcRepository.close();
  }

  @BeforeEach
  void freshLoan() throws Exception {
    JdbcRepository.update("UPDATE fines SET status='VOID' WHERE user_id=?", member);
    JdbcRepository.update(
        "UPDATE loans SET status='VOID' WHERE user_id=? AND status IN ('ACTIVE','OVERDUE')",
        member);
    copy = JdbcRepository.update("INSERT INTO copies(book_id,branch_id) VALUES(1,1)");
    CirculationService.issue(actor, member, copy, null);
    loan =
        JdbcRepository.id(JdbcRepository.one("SELECT id FROM loans WHERE copy_id=?", copy), "id");
  }

  @Test
  void dueDateAndVoidMaintainStock() throws Exception {
    Timestamp due = Timestamp.from(Instant.now().plusSeconds(86400 * 20));
    CirculationService.dueDate(actor, loan, due, "Approved extension", null);
    assertEquals(
        due.toInstant().getEpochSecond(),
        ((Timestamp) JdbcRepository.one("SELECT due_at FROM loans WHERE id=?", loan).get("due_at"))
            .toInstant()
            .getEpochSecond());
    assertThrows(
        IllegalArgumentException.class,
        () -> CirculationService.dueDate(actor, loan, due, "Wrong branch", 2L));
    CirculationService.voidLoan(actor, loan, "Wrong issue", null);
    assertEquals(
        "AVAILABLE",
        JdbcRepository.one("SELECT status FROM copies WHERE id=?", copy).get("status"));
    assertThrows(
        IllegalArgumentException.class, () -> CirculationService.returnBook(actor, loan, null));
  }

  @Test
  void partialPaymentRefundAndAssessmentDoNotDuplicate() throws Exception {
    JdbcRepository.update(
        "UPDATE loans SET due_at=? WHERE id=?",
        Timestamp.from(Instant.now().minusSeconds(90000)),
        loan);
    FineService.assessOverdue(actor, null);
    FineService.assessOverdue(actor, null);
    var f = JdbcRepository.one("SELECT * FROM fines WHERE loan_id=?", loan);
    long id = JdbcRepository.id(f, "id");
    assertEquals(1, JdbcRepository.list("SELECT id FROM fines WHERE loan_id=?", loan).size());
    long payment = FineService.offlinePayment(actor, id, new BigDecimal("10"), "CASH", "R-1", null);
    assertEquals(
        "PARTIALLY_PAID",
        JdbcRepository.one("SELECT status FROM fines WHERE id=?", id).get("status"));
    assertThrows(
        IllegalArgumentException.class,
        () -> FineService.offlinePayment(actor, id, new BigDecimal("1000"), "CASH", "", null));
    CirculationService.returnBook(actor, loan, null);
    assertEquals(1, JdbcRepository.list("SELECT id FROM fines WHERE loan_id=?", loan).size());
    PaymentGatewayService.reverse(payment, "Correction");
    assertEquals(
        "OUTSTANDING", JdbcRepository.one("SELECT status FROM fines WHERE id=?", id).get("status"));
    FineService.adjustFine(actor, id, new BigDecimal("15"), "Approved correction", null, false);
    FineService.offlinePayment(actor, id, new BigDecimal("15"), "BANK_TRANSFER", "BANK-1", null);
    assertEquals(
        "PAID", JdbcRepository.one("SELECT status FROM fines WHERE id=?", id).get("status"));
  }

  @Test
  void partialReceivingAndCopyHistoryGuards() throws Exception {
    long p =
        JdbcRepository.update(
            "INSERT INTO purchases(supplier_id,book_id,branch_id,quantity,unit_cost,created_at)"
                + " VALUES(1,1,1,3,10,?)",
            BusinessRules.now());
    long before = JdbcRepository.id(JdbcRepository.one("SELECT COUNT(*) AS n FROM copies"), "n");
    ProcurementService.receive(actor, p, 1, null);
    assertEquals(
        "PARTIALLY_RECEIVED",
        JdbcRepository.one("SELECT status FROM purchases WHERE id=?", p).get("status"));
    assertThrows(
        IllegalArgumentException.class, () -> ProcurementService.receive(actor, p, 3, null));
    ProcurementService.receive(actor, p, 2, null);
    assertEquals(
        before + 3, JdbcRepository.id(JdbcRepository.one("SELECT COUNT(*) AS n FROM copies"), "n"));
    assertThrows(
        IllegalArgumentException.class, () -> ProcurementService.receive(actor, p, 1, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> InventoryService.editCopy(actor, copy, 1, "A", null, true));
    CirculationService.returnBook(actor, loan, null);
    assertThrows(
        IllegalArgumentException.class,
        () -> InventoryService.editCopy(actor, copy, 1, "A", null, true));
    InventoryService.editCopy(actor, copy, 2, "Shelf B", null, false);
    assertEquals(
        2,
        JdbcRepository.id(
            JdbcRepository.one("SELECT branch_id FROM copies WHERE id=?", copy), "branch_id"));
  }

  @Test
  void purchaseBatchRollsBackInvalidLines() throws Exception {
    String invoice = "TEST-" + System.nanoTime();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ProcurementService.create(
                actor,
                1,
                1,
                invoice,
                "",
                List.of(
                    new PurchaseItem(1, 2, new BigDecimal("10")),
                    new PurchaseItem(999999, 1, new BigDecimal("10"))),
                null));
    assertNull(JdbcRepository.one("SELECT id FROM purchases WHERE invoice_ref=?", invoice));
    ProcurementService.create(
        actor,
        1,
        1,
        invoice,
        "",
        List.of(
            new PurchaseItem(1, 2, new BigDecimal("10")),
            new PurchaseItem(2, 1, new BigDecimal("20"))),
        null);
    assertEquals(
        2, JdbcRepository.list("SELECT id FROM purchases WHERE invoice_ref=?", invoice).size());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ProcurementService.create(
                actor, 1, 1, invoice, "", List.of(new PurchaseItem(1, 1, BigDecimal.ONE)), null));
  }

  @Test
  void reservationEditOwnershipAndInactiveMember() throws Exception {
    ReservationService.reserve(member, 6, 1);
    var r =
        JdbcRepository.one(
            "SELECT * FROM reservations WHERE user_id=? AND book_id=6 AND status IN"
                + " ('WAITING','READY')",
            member);
    long id = JdbcRepository.id(r, "id");
    assertThrows(
        IllegalArgumentException.class,
        () -> ReservationService.moveReservation(actor, id, 2, false, null));
    ReservationService.moveReservation(member, id, 2, false, null);
    assertEquals(
        2,
        JdbcRepository.id(
            JdbcRepository.one("SELECT branch_id FROM reservations WHERE id=?", id), "branch_id"));
    ReservationService.cancelReservation(member, id, false, null);
    JdbcRepository.update("UPDATE users SET active=0 WHERE id=?", member);
    try {
      assertThrows(IllegalArgumentException.class, () -> ReservationService.reserve(member, 5, 1));
    } finally {
      JdbcRepository.update("UPDATE users SET active=1 WHERE id=?", member);
    }
  }
}

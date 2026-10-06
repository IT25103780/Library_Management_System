package lk.lumina;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lk.lumina.config.ApplicationInitializer;
import lk.lumina.config.DatabaseMigrations;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.security.Security;
import lk.lumina.service.BorrowingService;
import lk.lumina.service.CirculationService;
import lk.lumina.service.PaymentGatewayService;
import lk.lumina.service.PaymentService;
import lk.lumina.service.ReminderService;
import lk.lumina.service.ReservationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LibraryTest {
  static long reader;
  static long other;
  static long loan;
  static String order;

  @BeforeAll
  static void setup() throws Exception {
    System.setProperty("storage.path", Files.createTempDirectory("lumina-test-").toString());
    System.setProperty("db.mode", System.getProperty("test.db.mode", "demo"));
    if (System.getProperty("test.db.url") != null)
      System.setProperty("db.url", System.getProperty("test.db.url"));
    System.setProperty("payment.mode", "demo");
    JdbcRepository.init();
    ApplicationInitializer.schema();
    DatabaseMigrations.run();
    ApplicationInitializer.seed();
    reader =
        JdbcRepository.id(JdbcRepository.one("SELECT id FROM users WHERE username='reader'"), "id");
    other =
        JdbcRepository.id(JdbcRepository.one("SELECT id FROM users WHERE username='admin'"), "id");
  }

  @AfterAll
  static void stop() {
    JdbcRepository.close();
  }

  @Test
  @Order(1)
  void passwordHashesAreSaltedAndVerified() throws Exception {
    String h1 = Security.hash("correct-password"), h2 = Security.hash("correct-password");
    assertNotEquals(h1, h2);
    assertTrue(Security.verify("correct-password", h1));
    assertFalse(Security.verify("wrong-password", h1));
  }

  @Test
  @Order(2)
  void pendingAndFailedPaymentCannotRead() throws Exception {
    var p = PaymentService.createOrder(reader, 1, 0, 7);
    order = p.get("order_ref").toString();
    assertEquals("PENDING", p.get("status"));
    assertThrows(IllegalArgumentException.class, () -> BorrowingService.reading(reader, 999));
    assertNull(JdbcRepository.one("SELECT id FROM loans WHERE user_id=?", reader));
    PaymentService.settle(order, "failure-test", "DEMO", false);
    assertNull(JdbcRepository.one("SELECT id FROM loans WHERE user_id=?", reader));
    assertEquals(
        "FAILED",
        JdbcRepository.one("SELECT status FROM payments WHERE order_ref=?", order).get("status"));
  }

  @Test
  @Order(3)
  void verifiedPaymentActivatesExactlyOneLoan() throws Exception {
    var p = PaymentService.createOrder(reader, 1, 0, 7);
    order = p.get("order_ref").toString();
    PaymentService.settle(order, "paid-test", "DEMO", true);
    PaymentService.settle(order, "paid-test", "DEMO", true);
    var rows = JdbcRepository.list("SELECT * FROM loans WHERE user_id=? AND book_id=1", reader);
    assertEquals(1, rows.size());
    loan = JdbcRepository.id(rows.get(0), "id");
    assertEquals(
        "The Art of Paying Attention", BorrowingService.reading(reader, loan).get("title"));
    assertThrows(IllegalArgumentException.class, () -> BorrowingService.reading(other, loan));
    assertThrows(IllegalArgumentException.class, () -> PaymentService.createOrder(reader, 1, 0, 7));
  }

  @Test
  @Order(4)
  void expiredLoanBlocksReadingAndRemindersDeduplicate() throws Exception {
    JdbcRepository.update(
        "UPDATE loans SET due_at=? WHERE id=?",
        Timestamp.from(Instant.now().minusSeconds(60)),
        loan);
    assertThrows(IllegalArgumentException.class, () -> BorrowingService.reading(reader, loan));
    ReminderService.reminders();
    long before =
        JdbcRepository.id(JdbcRepository.one("SELECT COUNT(*) AS n FROM notifications"), "n");
    ReminderService.reminders();
    assertEquals(
        before,
        JdbcRepository.id(JdbcRepository.one("SELECT COUNT(*) AS n FROM notifications"), "n"));
    assertEquals(
        "EXPIRED", JdbcRepository.one("SELECT status FROM loans WHERE id=?", loan).get("status"));
  }

  @Test
  @Order(5)
  void reservationAssignsCopyAndCannotBeIssuedToAnotherReader() throws Exception {
    ReservationService.reserve(reader, 2, 1);
    var r = JdbcRepository.one("SELECT * FROM reservations WHERE user_id=? AND book_id=2", reader);
    assertEquals("READY", r.get("status"));
    long copy = JdbcRepository.id(r, "copy_id");
    assertThrows(
        IllegalArgumentException.class, () -> CirculationService.issue(other, other, copy, null));
    CirculationService.issue(other, reader, copy, null);
    assertEquals(
        "FULFILLED",
        JdbcRepository.one("SELECT status FROM reservations WHERE id=?", r.get("id"))
            .get("status"));
    assertEquals(
        "BORROWED", JdbcRepository.one("SELECT status FROM copies WHERE id=?", copy).get("status"));
  }

  @Test
  @Order(6)
  void overdueReturnCreatesFineAndPaymentClearsIt() throws Exception {
    var l = JdbcRepository.one("SELECT * FROM loans WHERE user_id=? AND kind='PHYSICAL'", reader);
    JdbcRepository.update(
        "UPDATE loans SET due_at=? WHERE id=?",
        Timestamp.from(Instant.now().minusSeconds(90000)),
        l.get("id"));
    CirculationService.returnBook(other, JdbcRepository.id(l, "id"), null);
    var f = JdbcRepository.one("SELECT * FROM fines WHERE loan_id=?", l.get("id"));
    assertEquals(0, new BigDecimal("40").compareTo((BigDecimal) f.get("amount")));
    var p = PaymentService.createOrder(reader, 0, JdbcRepository.id(f, "id"), 7);
    PaymentService.settle(p.get("order_ref").toString(), "fine-test", "DEMO", true);
    assertEquals(
        "PAID",
        JdbcRepository.one("SELECT status FROM fines WHERE id=?", f.get("id")).get("status"));
    assertThrows(
        IllegalArgumentException.class,
        () -> CirculationService.returnBook(other, JdbcRepository.id(l, "id"), null));
  }

  @Test
  @Order(7)
  void branchManagerCannotIssueOutsideBranch() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> CirculationService.issue(other, reader, 3, 1L));
  }

  @Test
  @Order(8)
  void cardCallbackValidatesSignatureAmountAndDuplicates() throws Exception {
    System.setProperty("payment.mode", "payhere-sandbox");
    System.setProperty("payhere.merchant.id", "test-merchant");
    System.setProperty("payhere.merchant.secret", "test-secret");
    try {
      var p = PaymentService.createOrder(reader, 3, 0, 7);
      Map<String, String> f = new HashMap<>();
      f.put("merchant_id", "test-merchant");
      f.put("order_id", p.get("order_ref").toString());
      f.put("payhere_amount", p.get("amount").toString());
      f.put("payhere_currency", "LKR");
      f.put("status_code", "2");
      f.put("payment_id", "card-test");
      f.put("md5sig", "INVALID");
      assertThrows(IllegalArgumentException.class, () -> PaymentGatewayService.callback(f));
      f.put("payhere_amount", "1.00");
      sign(f);
      assertThrows(IllegalArgumentException.class, () -> PaymentGatewayService.callback(f));
      f.put("payhere_amount", p.get("amount").toString());
      sign(f);
      PaymentGatewayService.callback(f);
      PaymentGatewayService.callback(f);
      assertEquals(
          "SUCCESSFUL",
          JdbcRepository.one("SELECT status FROM payments WHERE id=?", p.get("id")).get("status"));
      assertEquals(
          1,
          JdbcRepository.list("SELECT id FROM loans WHERE user_id=? AND book_id=3", reader).size());
    } finally {
      System.setProperty("payment.mode", "demo");
    }
  }

  static void sign(Map<String, String> f) throws Exception {
    f.put(
        "md5sig",
        PaymentGatewayService.md5(
            f.get("merchant_id")
                + f.get("order_id")
                + f.get("payhere_amount")
                + f.get("payhere_currency")
                + f.get("status_code")
                + PaymentGatewayService.md5("test-secret")));
  }

  @Test
  @Order(9)
  void concurrentIssueCannotLendTheSameCopyTwice() throws Exception {
    var copy =
        JdbcRepository.one("SELECT TOP 1 id FROM copies WHERE status='AVAILABLE' AND book_id=4");
    long id = JdbcRepository.id(copy, "id");
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      var tasks =
          List.<Callable<Boolean>>of(
              () -> {
                try {
                  CirculationService.issue(other, reader, id, null);
                  return true;
                } catch (Exception e) {
                  return false;
                }
              },
              () -> {
                try {
                  CirculationService.issue(other, other, id, null);
                  return true;
                } catch (Exception e) {
                  return false;
                }
              });
      var results = executor.invokeAll(tasks);
      int successes = 0;
      for (var result : results) if (result.get()) successes++;
      assertEquals(1, successes);
      assertEquals(
          1,
          JdbcRepository.list("SELECT id FROM loans WHERE copy_id=? AND status='ACTIVE'", id)
              .size());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  @Order(10)
  void refundRevokesReadingAndIsIdempotent() throws Exception {
    var p = PaymentService.createOrder(reader, 6, 0, 7);
    PaymentService.settle(p.get("order_ref").toString(), "refund-source", "DEMO", true);
    long id =
        JdbcRepository.id(
            JdbcRepository.one("SELECT loan_id FROM payments WHERE id=?", p.get("id")), "loan_id");
    assertNotNull(BorrowingService.reading(reader, id));
    PaymentGatewayService.reverse(JdbcRepository.id(p, "id"), "refund-1");
    PaymentGatewayService.reverse(JdbcRepository.id(p, "id"), "refund-1");
    assertThrows(IllegalArgumentException.class, () -> BorrowingService.reading(reader, id));
    assertEquals(
        "REFUNDED",
        JdbcRepository.one("SELECT status FROM payments WHERE id=?", p.get("id")).get("status"));
  }

  @Test
  @Order(11)
  void repeatedBorrowClickReusesPendingOrderAndRejectsArbitraryDuration() throws Exception {
    var p = PaymentService.createOrder(reader, 5, 0, 7);
    var again = PaymentService.createOrder(reader, 5, 0, 7);
    assertEquals(p.get("id"), again.get("id"));
    assertThrows(
        IllegalArgumentException.class, () -> PaymentService.createOrder(reader, 5, 0, 999));
  }

  @Test
  @Order(12)
  void fineOwnershipAndReservationCancellationAreEnforced() throws Exception {
    var f = JdbcRepository.one("SELECT * FROM fines WHERE user_id=?", reader);
    assertThrows(
        IllegalArgumentException.class,
        () -> PaymentService.createOrder(other, 0, JdbcRepository.id(f, "id"), 7));
    ReservationService.reserve(reader, 6, 1);
    var res =
        JdbcRepository.one("SELECT * FROM reservations WHERE user_id=? AND book_id=6", reader);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ReservationService.cancelReservation(other, JdbcRepository.id(res, "id"), false, null));
    ReservationService.cancelReservation(reader, JdbcRepository.id(res, "id"), false, null);
    assertEquals(
        "CANCELLED",
        JdbcRepository.one("SELECT status FROM reservations WHERE id=?", res.get("id"))
            .get("status"));
  }
}

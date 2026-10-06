package lk.lumina.service;

import static lk.lumina.util.BusinessRules.now;
import static lk.lumina.util.BusinessRules.require;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lk.lumina.config.AppConfig;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.PaymentRepository;

/** Payment operations, preserving existing validation and transaction boundaries. */
public final class PaymentService {
  public static Map<String, Object> createOrder(long uid, long bookId, long fineId, int days)
      throws Exception {
    return JdbcRepository.tx(
        c -> {
          BigDecimal amount;
          Map<String, Object> b = null;
          if (fineId > 0) {
            var f = PaymentRepository.findOutstandingMemberFine(c, fineId, uid);
            require(f != null, "This fine is no longer outstanding.");
            amount = FineService.balance(c, fineId);
          } else {
            b = PaymentRepository.findActiveBook(c, bookId);
            require(
                b != null && b.get("pdf_path") != null && !b.get("format").equals("PHYSICAL"),
                "This book is not available for digital borrowing.");
            int base = ((Number) b.get("duration_days")).intValue();
            require(
                days == base || days == base * 2 || days == base * 4,
                "Select a listed borrowing period.");
            require(
                PaymentRepository.findActiveDigitalLoan(c, uid, bookId, now()) == null,
                "This book is already on your active bookshelf.");
            amount = ((BigDecimal) b.get("fee")).multiply(BigDecimal.valueOf(days / base));
          }
          var old =
              PaymentRepository.findPendingOrder(
                  c,
                  String.valueOf(
                      (fineId > 0 ? "fine_id=?" : "book_id=? AND duration_days=" + days)),
                  uid,
                  fineId > 0 ? fineId : bookId);
          if (old != null) return old;
          String ref = "LUM-" + UUID.randomUUID();
          long id =
              PaymentRepository.insertOrder(
                  c,
                  ref,
                  uid,
                  fineId > 0 ? null : bookId,
                  fineId > 0 ? fineId : null,
                  amount,
                  days,
                  AppConfig.demo() ? "DEMO" : "PAYHERE",
                  now());
          return PaymentRepository.findById(c, id);
        });
  }

  public static void settle(String ref, String providerRef, String method, boolean success)
      throws Exception {
    JdbcRepository.tx(
        c -> {
          var p = PaymentRepository.findByOrderReference(c, ref);
          require(p != null, "Unknown payment order.");
          if (p.get("status").equals("SUCCESSFUL") || p.get("status").equals("REFUNDED"))
            return null;
          long uid = JdbcRepository.id(p, "user_id");
          if (!success) {
            PaymentRepository.markFailed(c, providerRef, method, p.get("id"));
            NotificationService.notify(
                c,
                uid,
                "Payment unsuccessful",
                "The payment was not completed. You can try again from Payments.",
                "/payments",
                "pay-failed-" + p.get("id"));
            return null;
          }
          Long loanId = null;
          if (p.get("fine_id") != null) {
            var fine = PaymentRepository.findFine(c, p.get("fine_id"));
            require(
                Set.of("OUTSTANDING", "PARTIALLY_PAID").contains(fine.get("status")),
                "Fine already settled; reconcile this payment.");
            require(
                ((BigDecimal) p.get("amount"))
                        .compareTo(FineService.balance(c, JdbcRepository.id(fine, "id")))
                    <= 0,
                "Payment exceeds the remaining fine; reconciliation required.");
          } else {
            var active = PaymentRepository.findCurrentDigitalLoan(c, uid, p.get("book_id"), now());
            // A second genuinely paid order extends the existing loan instead of charging without
            // access.
            Instant start = Instant.now();
            Instant base = active == null ? start : ((Timestamp) active.get("due_at")).toInstant();
            Timestamp due =
                Timestamp.from(
                    base.plus(Duration.ofDays(((Number) p.get("duration_days")).intValue())));
            if (active == null)
              loanId =
                  PaymentRepository.insertDigitalLoan(
                      c, uid, p.get("book_id"), Timestamp.from(start), due);
            else {
              loanId = JdbcRepository.id(active, "id");
              PaymentRepository.extendDigitalLoan(c, due, loanId);
            }
            NotificationService.notify(
                c,
                uid,
                "Your book is ready",
                "Payment confirmed. Open My Books and select Read Now to begin.",
                "/my-books",
                "loan-paid-" + p.get("id"));
          }
          PaymentRepository.markSuccessful(c, providerRef, method, loanId, now(), p.get("id"));
          ReceiptService.capture(c, JdbcRepository.id(p, "id"));
          if (p.get("fine_id") != null) FineService.fineStatus(c, JdbcRepository.id(p, "fine_id"));
          NotificationService.notify(
              c,
              uid,
              "Payment receipt",
              "Payment of LKR "
                  + p.get("amount")
                  + " confirmed. Your receipt is available in Payments.",
              "/payments",
              "pay-success-" + p.get("id"));
          return null;
        });
  }
}

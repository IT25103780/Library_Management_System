package lk.lumina.service;

import static lk.lumina.security.AccessControl.scope;
import static lk.lumina.service.AuditService.audit;
import static lk.lumina.service.SettingsService.setting;
import static lk.lumina.util.BusinessRules.now;
import static lk.lumina.util.BusinessRules.require;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lk.lumina.repository.FineRepository;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.util.ViewUtils;

/** Fine operations, preserving existing validation and transaction boundaries. */
public final class FineService {
  public static BigDecimal paid(Connection c, long fine) throws Exception {
    return (BigDecimal) FineRepository.totalPaid(c, fine).get("n");
  }

  public static BigDecimal balance(Connection c, long fine) throws Exception {
    var f = FineRepository.findAmount(c, fine);
    require(f != null, "Fine not found.");
    return ((BigDecimal) f.get("amount")).subtract(paid(c, fine));
  }

  public static void fineStatus(Connection c, long fine) throws Exception {
    var f = FineRepository.findById(c, fine);
    if (f == null || Set.of("VOID", "WAIVED").contains(f.get("status"))) return;
    BigDecimal paid = paid(c, fine), amount = (BigDecimal) f.get("amount");
    FineRepository.updateStatus(
        c,
        paid.compareTo(amount) >= 0 ? "PAID" : paid.signum() > 0 ? "PARTIALLY_PAID" : "OUTSTANDING",
        fine);
  }

  public static void assess(Connection c, Map<String, Object> loan) throws Exception {
    long seconds =
        Duration.between(((Timestamp) loan.get("due_at")).toInstant(), Instant.now()).getSeconds();
    if (seconds <= 0) return;
    long days = (seconds + 86399) / 86400;
    BigDecimal amount =
        BigDecimal.valueOf(days).multiply(BigDecimal.valueOf(setting(c, "fine_per_day")));
    var f = FineRepository.findByLoan(c, loan.get("id"));
    if (f == null) {
      long id =
          FineRepository.insertFine(
              c, loan.get("user_id"), loan.get("id"), amount, days + " overdue day(s)", now());
      NotificationService.notify(
          c,
          JdbcRepository.id(loan, "user_id"),
          "Overdue fine assessed",
          "An overdue fine is available in Fines. It can grow until the book is returned.",
          "/fines",
          "fine-" + id);
    } else if (JdbcRepository.id(f, "adjusted") == 0
        && !Set.of("VOID", "WAIVED").contains(f.get("status"))) {
      FineRepository.updateAssessment(c, amount, days + " overdue day(s)", f.get("id"));
      fineStatus(c, JdbcRepository.id(f, "id"));
    }
  }

  public static void assessOverdue(long actor, Long branch) throws Exception {
    JdbcRepository.tx(
        c -> {
          for (var l :
              FineRepository.listOverdueLoans(
                  c, String.valueOf((branch == null ? "" : " AND cp.branch_id=" + branch)), now()))
            assess(c, l);
          audit(c, actor, "Assess overdue fines", "Open physical loans");
          return null;
        });
  }

  public static long offlinePayment(
      long actor, long fine, BigDecimal amount, String method, String reference, Long branch)
      throws Exception {
    require(amount.signum() > 0, "Payment amount must be positive.");
    require(
        Set.of("CASH", "BANK_TRANSFER", "OTHER_OFFLINE").contains(method),
        "Choose an offline payment method.");
    return JdbcRepository.tx(
        c -> {
          var f = FineRepository.findFineWithBranch(c, fine);
          scope(f, branch);
          require(
              Set.of("OUTSTANDING", "PARTIALLY_PAID").contains(f.get("status")),
              "This fine is not outstanding.");
          require(
              FineRepository.findPendingPayment(c, fine) == null,
              "Resolve the pending online order before recording an offline payment.");
          require(
              amount.compareTo(balance(c, fine)) <= 0, "Payment exceeds the outstanding balance.");
          long id =
              FineRepository.insertOfflinePayment(
                  c,
                  "OFF-" + UUID.randomUUID(),
                  f.get("user_id"),
                  fine,
                  amount,
                  method,
                  reference,
                  now(),
                  now());
          ReceiptService.capture(c, id);
          fineStatus(c, fine);
          audit(
              c,
              actor,
              "Record fine payment",
              "Payment " + id + " / Fine " + fine + " / " + amount);
          NotificationService.notify(
              c,
              JdbcRepository.id(f, "user_id"),
              "Fine payment recorded",
              "Received " + ViewUtils.money(amount) + ". Your receipt is available in Payments.",
              "/payments",
              "offline-" + id);
          return id;
        });
  }

  public static void adjustFine(
      long actor, long fine, BigDecimal amount, String reason, Long branch, boolean voided)
      throws Exception {
    require(!reason.isBlank(), "A reason is required.");
    require(amount.signum() >= 0, "Amount cannot be negative.");
    JdbcRepository.tx(
        c -> {
          var f = FineRepository.findFineWithBranch(c, fine);
          scope(f, branch);
          require(
              !Set.of("VOID", "WAIVED").contains(f.get("status")), "This fine is already closed.");
          require(
              FineRepository.findPendingPayment(c, fine) == null,
              "Resolve pending payments first.");
          BigDecimal paid = paid(c, fine);
          require(
              amount.compareTo(paid) >= 0 && (!voided || paid.signum() == 0),
              "Reverse recorded payments before reducing the fine below the paid amount.");
          FineRepository.adjustFine(
              c,
              amount,
              reason,
              voided ? "VOID" : amount.signum() == 0 ? "WAIVED" : "OUTSTANDING",
              fine);
          fineStatus(c, fine);
          audit(c, actor, voided ? "Void fine" : "Adjust fine", "Fine " + fine + ": " + reason);
          return null;
        });
  }
}

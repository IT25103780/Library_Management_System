package lk.lumina.service;

import static lk.lumina.security.AccessControl.scope;
import static lk.lumina.service.AuditService.audit;
import static lk.lumina.service.ReservationService.fulfill;
import static lk.lumina.service.SettingsService.setting;
import static lk.lumina.util.BusinessRules.now;
import static lk.lumina.util.BusinessRules.require;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.LoanRepository;
import lk.lumina.util.ViewUtils;

/** Circulation operations, preserving existing validation and transaction boundaries. */
public final class CirculationService {
  public static void issue(long actor, long uid, long copyId, Long branch) throws Exception {
    JdbcRepository.tx(
        c -> {
          var user = LoanRepository.findActiveBorrower(c, uid);
          require(user != null, "Borrower not found or inactive.");
          var copy = LoanRepository.findIssuableCopy(c, copyId);
          require(copy != null, "Copy not found or book inactive.");
          if (branch != null)
            require(
                JdbcRepository.id(copy, "branch_id") == branch,
                "Choose a copy in your assigned branch.");
          var reservation = LoanRepository.findReadyReservation(c, copyId, uid, now());
          require(
              copy.get("status").equals("AVAILABLE")
                  || (copy.get("status").equals("RESERVED") && reservation != null),
              "Copy unavailable or reserved for another reader.");
          long count = JdbcRepository.id(LoanRepository.countOpenPhysicalLoans(c, uid), "n");
          require(count < setting(c, "max_loans"), "Borrowing limit reached.");
          require(
              LoanRepository.findOverdueLoan(c, uid, now()) == null,
              "Return overdue books before borrowing again.");
          require(
              LoanRepository.findOutstandingFine(c, uid) == null,
              "Settle outstanding fines before borrowing.");
          Timestamp due =
              Timestamp.from(Instant.now().plus(Duration.ofDays(setting(c, "physical_days"))));
          long id =
              LoanRepository.insertPhysicalLoan(c, uid, copy.get("book_id"), copyId, now(), due);
          LoanRepository.markCopyBorrowed(c, copyId);
          if (reservation != null) LoanRepository.fulfillReservation(c, reservation.get("id"));
          NotificationService.notify(
              c,
              uid,
              "Book issued",
              "Your physical book is due on " + ViewUtils.date(due) + " (Sri Lanka time).",
              "/my-books",
              "issue-" + id);
          audit(c, actor, "Issue book", "Loan " + id);
          return null;
        });
  }

  public static void returnBook(long actor, long loan, Long branch) throws Exception {
    JdbcRepository.tx(
        c -> {
          var r = LoanRepository.findOpenPhysicalLoan(c, loan);
          require(r != null, "This loan has already been returned or is unavailable.");
          if (branch != null)
            require(
                JdbcRepository.id(r, "branch_id") == branch,
                "This loan belongs to another branch.");
          FineService.assess(c, r);
          LoanRepository.markReturned(c, now(), loan);
          LoanRepository.releaseCopy(c, r.get("copy_id"));
          NotificationService.notify(
              c,
              JdbcRepository.id(r, "user_id"),
              "Book returned",
              "Your return has been recorded. Thank you for reading with Lumina.",
              "/my-books",
              "return-" + loan);
          audit(c, actor, "Return book", "Loan " + loan);
          fulfill(c);
          return null;
        });
  }

  public static void dueDate(long actor, long id, Timestamp due, String reason, Long branch)
      throws Exception {
    require(reason != null && !reason.isBlank(), "Enter a reason for the date change.");
    JdbcRepository.tx(
        c -> {
          var l = LoanRepository.findPhysicalLoan(c, id);
          scope(l, branch);
          require(
              Set.of("ACTIVE", "OVERDUE").contains(l.get("status")),
              "Only an open loan can be changed.");
          require(
              due.after((Timestamp) l.get("start_at")) && due.after(now()),
              "The new due date must be in the future and after issue.");
          require(
              LoanRepository.findWaitingReservation(c, l.get("book_id"), l.get("branch_id"))
                  == null,
              "Another member is waiting for this title.");
          require(
              LoanRepository.findUnresolvedFine(c, id) == null,
              "Resolve an assessed fine before extending this loan.");
          LoanRepository.extendDueDate(c, due, id);
          audit(c, actor, "Change due date", "Loan " + id + ": " + reason);
          NotificationService.notify(
              c,
              JdbcRepository.id(l, "user_id"),
              "Due date updated",
              "Your new due date is " + ViewUtils.date(due) + ". " + reason,
              "/my-books",
              "due-change-" + UUID.randomUUID());
          return null;
        });
  }

  public static void voidLoan(long actor, long id, String reason, Long branch) throws Exception {
    require(reason != null && !reason.isBlank(), "Enter a correction reason.");
    JdbcRepository.tx(
        c -> {
          var l = LoanRepository.findPhysicalLoan(c, id);
          scope(l, branch);
          require(
              Set.of("ACTIVE", "OVERDUE", "RETURNED").contains(l.get("status")),
              "Loan already voided.");
          require(
              LoanRepository.findUnresolvedFine(c, id) == null,
              "Void or settle the associated fine first.");
          if (!l.get("status").equals("RETURNED")) LoanRepository.releaseCopy(c, l.get("copy_id"));
          LoanRepository.markVoided(c, id);
          audit(c, actor, "Void mistaken loan", "Loan " + id + ": " + reason);
          fulfill(c);
          return null;
        });
  }
}

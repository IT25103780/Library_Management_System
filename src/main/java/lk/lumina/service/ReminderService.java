package lk.lumina.service;

import static lk.lumina.service.ReservationService.fulfill;
import static lk.lumina.util.BusinessRules.now;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.ReminderRepository;

/** Reminder operations, preserving existing validation and transaction boundaries. */
public final class ReminderService {
  public static void reminders() throws Exception {
    JdbcRepository.tx(
        c -> {
          Timestamp now = now();
          Timestamp tomorrow = Timestamp.from(Instant.now().plus(Duration.ofDays(1)));
          for (var r : ReminderRepository.listLoansDueSoon(c, tomorrow)) {
            long id = JdbcRepository.id(r, "id");
            boolean expired = ((Timestamp) r.get("due_at")).before(now);
            String type = r.get("kind").equals("DIGITAL") ? "digital" : "physical";
            if (expired)
              ReminderRepository.updateLoanStatus(
                  c, type.equals("digital") ? "EXPIRED" : "OVERDUE", id);
            NotificationService.notify(
                c,
                JdbcRepository.id(r, "user_id"),
                expired
                    ? (type.equals("digital") ? "Digital borrowing expired" : "Book overdue")
                    : "Borrowing ends soon",
                expired
                    ? "Review your bookshelf for borrowing status and next steps."
                    : "Your borrowing period ends within 24 hours. Check My Books for the due"
                        + " date.",
                "/my-books",
                (expired ? "expired-" : "due-")
                    + id
                    + "-"
                    + ((Timestamp) r.get("due_at")).getTime());
          }
          for (var r : ReminderRepository.listExpiredReservations(c, now)) {
            ReminderRepository.expireReservation(c, r.get("id"));
            ReminderRepository.releaseCopy(c, r.get("copy_id"));
            NotificationService.notify(
                c,
                JdbcRepository.id(r, "user_id"),
                "Collection period ended",
                "Your reservation expired before collection. You can reserve again.",
                "/reservations",
                "res-expired-" + r.get("id"));
          }
          fulfill(c);
          ReminderRepository.deleteExpiredResetTokens(
              c, Timestamp.from(Instant.now().minus(Duration.ofDays(1))));
          return null;
        });
  }
}

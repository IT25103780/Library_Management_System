package lk.lumina.service;

import static lk.lumina.security.AccessControl.scope;
import static lk.lumina.service.AuditService.audit;
import static lk.lumina.service.SettingsService.setting;
import static lk.lumina.util.BusinessRules.now;
import static lk.lumina.util.BusinessRules.require;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.ReservationRepository;
import lk.lumina.util.ViewUtils;

/** Reservation operations, preserving existing validation and transaction boundaries. */
public final class ReservationService {
  public static void reserve(long uid, long book, long branch) throws Exception {
    JdbcRepository.tx(
        c -> {
          require(
              ReservationRepository.findActiveMember(c, uid) != null,
              "Member is inactive or missing.");
          var b = ReservationRepository.findPhysicalBook(c, book);
          require(b != null, "Choose an available physical title.");
          require(
              ReservationRepository.findActiveBranch(c, branch) != null,
              "Choose an active branch.");
          require(
              ReservationRepository.findOpenMemberReservation(c, uid, book) == null,
              "You already have an open reservation for this book.");
          long id = ReservationRepository.insertReservation(c, uid, book, branch, now());
          NotificationService.notify(
              c,
              uid,
              "Reservation confirmed",
              "You have joined the collection queue. We will notify you when your copy is ready.",
              "/reservations",
              "reservation-" + id);
          for (var staff : ReservationRepository.listBranchStaff(c, branch))
            NotificationService.notify(
                c,
                JdbcRepository.id(staff, "id"),
                "New reservation",
                "A reader has joined the reservation queue.",
                "/reservations",
                "staff-res-" + id + "-" + staff.get("id"));
          fulfill(c);
          return null;
        });
  }

  public static void cancelReservation(long uid, long id, boolean staff, Long branch)
      throws Exception {
    JdbcRepository.tx(
        c -> {
          var r = ReservationRepository.findById(c, id);
          require(
              r != null && (staff || JdbcRepository.id(r, "user_id") == uid),
              "Reservation unavailable.");
          if (branch != null)
            require(
                JdbcRepository.id(r, "branch_id") == branch,
                "This reservation belongs to another branch.");
          require(
              List.of("WAITING", "READY").contains(r.get("status")),
              "This reservation is already closed.");
          if (r.get("copy_id") != null) ReservationRepository.releaseCopy(c, r.get("copy_id"));
          ReservationRepository.cancelReservation(c, id);
          NotificationService.notify(
              c,
              JdbcRepository.id(r, "user_id"),
              "Reservation cancelled",
              "Your reservation has been cancelled.",
              "/reservations",
              "cancel-res-" + id);
          fulfill(c);
          return null;
        });
  }

  public static void fulfill(Connection c) throws Exception {
    for (var r : ReservationRepository.listWaitingQueue(c)) {
      var copy = ReservationRepository.findAvailableCopy(c, r.get("book_id"), r.get("branch_id"));
      if (copy == null) continue;
      Timestamp until =
          Timestamp.from(Instant.now().plus(Duration.ofHours(setting(c, "reservation_hours"))));
      ReservationRepository.reserveCopy(c, copy.get("id"));
      ReservationRepository.markReady(c, copy.get("id"), until, r.get("id"));
      NotificationService.notify(
          c,
          JdbcRepository.id(r, "user_id"),
          "Ready for collection",
          "Your reserved book is ready. Collect it before "
              + ViewUtils.date(until)
              + " (Sri Lanka time).",
          "/reservations",
          "ready-" + r.get("id") + "-" + until.getTime());
    }
  }

  public static void moveReservation(
      long actor, long id, long destination, boolean staff, Long branch) throws Exception {
    JdbcRepository.tx(
        c -> {
          var r = ReservationRepository.findById(c, id);
          scope(r, branch);
          require(
              staff || JdbcRepository.id(r, "user_id") == actor,
              "This reservation belongs to another member.");
          require(
              Set.of("WAITING", "READY").contains(r.get("status")),
              "Only an open reservation can be modified.");
          require(
              JdbcRepository.id(r, "branch_id") != destination,
              "Choose a different collection branch.");
          if (branch != null) require(destination == branch, "Choose your assigned branch.");
          require(
              ReservationRepository.findActiveBranch(c, destination) != null,
              "Choose an active branch.");
          if (r.get("copy_id") != null) ReservationRepository.releaseCopy(c, r.get("copy_id"));
          ReservationRepository.changeBranch(c, destination, now(), id);
          audit(
              c,
              actor,
              "Change reservation branch",
              "Reservation " + id + "; queue time restarted");
          fulfill(c);
          return null;
        });
  }
}

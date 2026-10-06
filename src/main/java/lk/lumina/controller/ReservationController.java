package lk.lumina.controller;

import static lk.lumina.security.AccessControl.branch;
import static lk.lumina.security.AccessControl.staff;
import static lk.lumina.security.AccessControl.staffOnly;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.ReservationRepository;
import lk.lumina.service.ReservationService;

/** Handles reservation requests using the existing URLs and form contracts. */
public final class ReservationController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/reservations":
        {
          q.setAttribute(
              "branches",
              ReservationRepository.listActiveBranches(
                  String.valueOf((branch(u) == null ? "" : " AND id=" + branch(u)))));
          q.setAttribute(
              "rows",
              ReservationRepository.listVisibleReservations(
                  String.valueOf(
                      (staff(u)
                          ? (branch(u) == null ? "1=1" : "r.branch_id=" + branch(u))
                          : "r.user_id=" + u.get("id")))));
          view(q, r, "reservations");
          break;
        }
      default:
        throw new IllegalArgumentException("Unknown get.");
    }
  }

  public static void action(
      HttpServletRequest q, HttpServletResponse r, String action, Map<String, Object> u)
      throws Exception {
    long uid = JdbcRepository.id(u, "id");
    switch (action) {
      case "reserve":
        {
          ReservationService.reserve(uid, num(q, "book_id"), num(q, "branch_id"));
          go(
              q,
              r,
              "/reservations",
              "Reservation saved. Watch your notifications for collection details.");
          return;
        }

      case "cancel-reservation":
        {
          ReservationService.cancelReservation(uid, num(q, "id"), staff(u), branch(u));
          go(q, r, "/reservations", "Reservation cancelled.");
          return;
        }

      case "reservation-edit":
        {
          ReservationService.moveReservation(
              uid, num(q, "id"), num(q, "branch_id"), staff(u), branch(u));
          go(q, r, "/reservations", "Collection branch updated; queue time restarted.");
          return;
        }

      case "reserve-member":
        {
          staffOnly(u);
          ReservationService.reserve(
              num(q, "user_id"),
              num(q, "book_id"),
              branch(u) == null ? num(q, "branch_id") : branch(u));
          go(q, r, "/reservations", "Reservation created for member.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }
}

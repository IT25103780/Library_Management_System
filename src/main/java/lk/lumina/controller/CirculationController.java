package lk.lumina.controller;

import static lk.lumina.security.AccessControl.branch;
import static lk.lumina.security.AccessControl.staffOnly;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.Map;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.LoanRepository;
import lk.lumina.service.CirculationService;

/** Handles circulation requests using the existing URLs and form contracts. */
public final class CirculationController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/circulation":
        {
          staffOnly(u);
          q.setAttribute("readers", LoanRepository.listActiveReaders());
          q.setAttribute(
              "copies",
              LoanRepository.listIssuableCopies(
                  String.valueOf((branch(u) == null ? "" : " AND c.branch_id=" + branch(u)))));
          q.setAttribute(
              "rows",
              LoanRepository.listPhysicalLoans(
                  String.valueOf((branch(u) == null ? "" : " AND c.branch_id=" + branch(u)))));
          view(q, r, "circulation");
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
      case "issue":
        {
          staffOnly(u);
          CirculationService.issue(uid, num(q, "user_id"), num(q, "copy_id"), branch(u));
          go(q, r, "/circulation", "Book issued and borrower notified.");
          return;
        }

      case "return":
        {
          staffOnly(u);
          CirculationService.returnBook(uid, num(q, "id"), branch(u));
          go(q, r, "/circulation", "Return recorded and availability updated.");
          return;
        }

      case "due-date":
        {
          staffOnly(u);
          CirculationService.dueDate(
              uid,
              num(q, "id"),
              Timestamp.from(
                  java.time.LocalDateTime.parse(p(q, "due_at"))
                      .atZone(ZoneId.of("Asia/Colombo"))
                      .toInstant()),
              required(q, "reason", 500),
              branch(u));
          go(q, r, "/circulation", "Due date updated.");
          return;
        }

      case "void-loan":
        {
          staffOnly(u);
          CirculationService.voidLoan(uid, num(q, "id"), required(q, "reason", 500), branch(u));
          go(q, r, "/circulation", "Mistaken loan voided; history retained.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }
}

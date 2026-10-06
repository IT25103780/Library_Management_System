package lk.lumina.controller;

import static lk.lumina.controller.FileResponse.file;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lk.lumina.config.AppConfig;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.LoanRepository;
import lk.lumina.service.BorrowingService;
import lk.lumina.service.PaymentService;

/** Handles borrowing requests using the existing URLs and form contracts. */
public final class BorrowingController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/my-books":
        {
          q.setAttribute("loans", LoanRepository.listMemberBooks(u.get("id")));
          view(q, r, "my-books");
          break;
        }

      case "/reader":
        {
          var loan = BorrowingService.reading(JdbcRepository.id(u, "id"), num(q, "id"));
          LoanRepository.incrementReadCount(num(q, "id"));
          q.setAttribute("loan", loan);
          view(q, r, "reader");
          break;
        }

      case "/pdf":
        {
          var l = BorrowingService.reading(JdbcRepository.id(u, "id"), num(q, "id"));
          file(
              q,
              r,
              AppConfig.data().resolve("pdf"),
              l.get("pdf_path").toString(),
              "application/pdf");
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
      case "borrow":
        {
          var order =
              PaymentService.createOrder(uid, num(q, "book_id"), 0, positive(q, "days", 3650));
          go(q, r, "/checkout?id=" + order.get("id"), "Review your borrowing order.");
          return;
        }

      case "progress":
        {
          BorrowingService.reading(uid, num(q, "id"));
          LoanRepository.saveReadingProgress(positive(q, "page", 100000), num(q, "id"), uid);
          r.setContentType("application/json");
          r.getWriter().write("{\"saved\":true}");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }
}

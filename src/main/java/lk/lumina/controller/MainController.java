package lk.lumina.controller;

import static lk.lumina.controller.BaseController.p;
import static lk.lumina.controller.BaseController.view;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lk.lumina.repository.UserRepository;
import lk.lumina.security.Security;
import lk.lumina.service.PaymentGatewayService;
import lk.lumina.util.BusinessRules;

@WebServlet(
    urlPatterns = {
      "/",
      "/home",
      "/catalog",
      "/book",
      "/login",
      "/signup",
      "/forgot",
      "/reset",
      "/logout",
      "/dashboard",
      "/my-books",
      "/reader",
      "/pdf",
      "/cover",
      "/checkout",
      "/payment-status",
      "/payment-notify",
      "/payments",
      "/payment-admin",
      "/receipt",
      "/notifications",
      "/api/notifications",
      "/reservations",
      "/fines",
      "/profile",
      "/circulation",
      "/manage",
      "/book-edit",
      "/reports",
      "/export",
      "/settings",
      "/audit",
      "/member",
      "/fine",
      "/action"
    })
@MultipartConfig(maxFileSize = 52428800, maxRequestSize = 57671680, fileSizeThreshold = 1048576)
public final class MainController extends HttpServlet {
  private static final Set<String> PUBLIC =
      Set.of(
          "/",
          "/home",
          "/catalog",
          "/book",
          "/login",
          "/signup",
          "/forgot",
          "/reset",
          "/cover",
          "/payment-notify");

  @SuppressWarnings("unchecked")
  protected void service(HttpServletRequest q, HttpServletResponse r)
      throws ServletException, IOException {
    q.setCharacterEncoding("UTF-8");
    r.setCharacterEncoding("UTF-8");
    r.setHeader("X-Content-Type-Options", "nosniff");
    r.setHeader("Referrer-Policy", "same-origin");
    r.setHeader("X-Frame-Options", "SAMEORIGIN");
    r.setHeader("Cache-Control", "no-store");
    r.setHeader(
        "Content-Security-Policy",
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self'"
            + " data: blob:; frame-src 'self' blob:; worker-src 'self' blob:; font-src 'self' data:"
            + " blob:; connect-src 'self'; object-src 'self'; base-uri 'self'; form-action 'self'"
            + " https://sandbox.payhere.lk https://www.payhere.lk");
    String path = q.getServletPath();
    if (path.isEmpty()) path = "/";
    q.setAttribute("path", path);
    q.setAttribute("ctx", q.getContextPath());
    if (path.startsWith("/assets/")
        && (q.getMethod().equals("GET") || q.getMethod().equals("HEAD"))) {
      r.setHeader("Cache-Control", "public, max-age=86400");
      getServletContext().getNamedDispatcher("default").forward(q, r);
      return;
    }
    try {
      if (path.equals("/payment-notify")) {
        BusinessRules.require(q.getMethod().equals("POST"), "POST required.");
        Map<String, String> f = new HashMap<>();
        q.getParameterMap().forEach((k, v) -> f.put(k, v[0]));
        PaymentGatewayService.callback(f);
        r.setContentType("text/plain");
        r.getWriter().write("OK");
        return;
      }
      HttpSession s = q.getSession();
      if (s.getAttribute("csrf") == null) s.setAttribute("csrf", Security.token());
      Map<String, Object> u = null;
      if (s.getAttribute("uid") != null)
        u = UserRepository.findSessionAccount(s.getAttribute("uid"));
      q.setAttribute("user", u);
      if (!PUBLIC.contains(path) && u == null) {
        r.sendRedirect(q.getContextPath() + "/login");
        return;
      }
      if (q.getMethod().equals("POST")) {
        BusinessRules.require(
            Objects.equals(s.getAttribute("csrf"), p(q, "csrf")),
            "Your session changed. Refresh the page and try again.");
        post(q, r, path, u);
        return;
      }
      if (!q.getMethod().equals("GET") && !q.getMethod().equals("HEAD")) {
        r.sendError(405);
        return;
      }
      get(q, r, path, u);
    } catch (java.time.format.DateTimeParseException ex) {
      r.setStatus(400);
      q.setAttribute("problem", "Enter a valid date and time.");
      view(q, r, "error");
    } catch (java.sql.SQLException ex) {
      getServletContext().log("Database request failed", ex);
      r.setStatus(400);
      q.setAttribute(
          "problem",
          ex.getSQLState() != null && ex.getSQLState().startsWith("23")
              ? "This value already exists or is linked to another record. Check the record and try"
                  + " again."
              : "Database operation failed. Check the SQL connection using Check-Database.cmd.");
      view(q, r, "error");
    } catch (IllegalArgumentException ex) {
      r.setStatus(400);
      q.setAttribute("problem", ex.getMessage());
      view(q, r, "error");
    } catch (Exception ex) {
      getServletContext().log("Request failed: " + path, ex);
      r.setStatus(500);
      q.setAttribute(
          "problem",
          "This action could not be completed. Your saved records are safe. Check the server log"
              + " for details or try again.");
      view(q, r, "error");
    }
  }

  private static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {
    switch (path) {
      case "/":
      case "/home":
      case "/dashboard":
        HomeController.get(q, r, path, u);
        return;
      case "/login":
      case "/signup":
      case "/forgot":
      case "/reset":
        AuthController.get(q, r, path, u);
        return;
      case "/catalog":
      case "/book":
      case "/book-edit":
        BookController.get(q, r, path, u);
        return;
      case "/my-books":
      case "/reader":
      case "/pdf":
        BorrowingController.get(q, r, path, u);
        return;
      case "/cover":
        MediaController.get(q, r, path, u);
        return;
      case "/checkout":
      case "/payment-status":
      case "/payment-admin":
      case "/payments":
      case "/receipt":
        PaymentController.get(q, r, path, u);
        return;
      case "/api/notifications":
      case "/notifications":
        NotificationController.get(q, r, path, u);
        return;
      case "/reservations":
        ReservationController.get(q, r, path, u);
        return;
      case "/fine":
      case "/fines":
        FineController.get(q, r, path, u);
        return;
      case "/profile":
      case "/member":
        AccountController.get(q, r, path, u);
        return;
      case "/circulation":
        CirculationController.get(q, r, path, u);
        return;
      case "/manage":
        ManagementController.get(q, r, path, u);
        return;
      case "/reports":
      case "/export":
        ReportController.get(q, r, path, u);
        return;
      case "/audit":
      case "/settings":
        AdminController.get(q, r, path, u);
        return;
      default:
        r.sendError(404);
    }
  }

  private static void post(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {
    if (path.equals("/action")) {
      action(q, r, p(q, "action"), u);
      return;
    }
    switch (path) {
      case "/login":
      case "/signup":
      case "/forgot":
      case "/reset":
      case "/logout":
        AuthController.post(q, r, path, u);
        return;
      case "/checkout":
        PaymentController.post(q, r, path, u);
        return;
      case "/profile":
        AccountController.post(q, r, path, u);
        return;
      case "/book-edit":
        BookController.post(q, r, path, u);
        return;
      case "/manage":
        ManagementController.post(q, r, path, u);
        return;
      case "/settings":
        AdminController.post(q, r, path, u);
        return;
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }

  private static void action(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {
    switch (path) {
      case "borrow":
      case "progress":
        BorrowingController.action(q, r, path, u);
        return;
      case "retry":
      case "cancel-payment":
      case "record-refund":
        PaymentController.action(q, r, path, u);
        return;
      case "pay-fine":
      case "payment-correct":
      case "payment-void":
      case "offline-fine":
      case "assess-fines":
      case "adjust-fine":
      case "void-fine":
        FineController.action(q, r, path, u);
        return;
      case "reserve":
      case "cancel-reservation":
      case "reservation-edit":
      case "reserve-member":
        ReservationController.action(q, r, path, u);
        return;
      case "read-notifications":
        NotificationController.action(q, r, path, u);
        return;
      case "issue":
      case "return":
      case "due-date":
      case "void-loan":
        CirculationController.action(q, r, path, u);
        return;
      case "purchase-batch":
      case "receive":
      case "purchase-update":
      case "purchase-cancel":
        ProcurementController.action(q, r, path, u);
        return;
      case "copy-edit":
      case "copy-delete":
      case "copy-status":
        InventoryController.action(q, r, path, u);
        return;
      case "toggle":
      case "delete-record":
        ManagementController.action(q, r, path, u);
        return;
      case "reset-account-password":
      case "announce":
        AdminController.action(q, r, path, u);
        return;
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }
}

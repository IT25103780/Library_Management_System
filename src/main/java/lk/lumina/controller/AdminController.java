package lk.lumina.controller;

import static lk.lumina.security.AccessControl.adminOnly;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lk.lumina.repository.AdministrationRepository;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.security.Security;
import lk.lumina.service.AuditService;
import lk.lumina.service.NotificationService;
import lk.lumina.util.BusinessRules;

/** Handles admin requests using the existing URLs and form contracts. */
public final class AdminController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/audit":
        {
          adminOnly(u);
          q.setAttribute(
              "rows",
              AdministrationRepository.searchAuditLog(
                  "%" + p(q, "q") + "%", "%" + p(q, "q") + "%", "%" + p(q, "q") + "%"));
          view(q, r, "audit");
          break;
        }

      case "/settings":
        {
          adminOnly(u);
          q.setAttribute("rows", AdministrationRepository.listSettings());
          q.setAttribute("audit", AdministrationRepository.listRecentAudit());
          view(q, r, "settings");
          break;
        }
      default:
        throw new IllegalArgumentException("Unknown get.");
    }
  }

  public static void post(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {
    long uid = JdbcRepository.id(u, "id");
    switch (path) {
      case "/settings":
        {
          adminOnly(u);
          JdbcRepository.tx(
              c -> {
                for (String key :
                    List.of("physical_days", "max_loans", "fine_per_day", "reservation_hours"))
                  AdministrationRepository.updateSetting(
                      c, Integer.toString(positive(q, key, 10000)), key);
                AuditService.audit(c, uid, "Settings updated", "Borrowing and fine policies");
                return null;
              });
          go(q, r, "/settings", "Library policies updated.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown post.");
    }
  }

  public static void action(
      HttpServletRequest q, HttpServletResponse r, String action, Map<String, Object> u)
      throws Exception {
    long uid = JdbcRepository.id(u, "id");
    switch (action) {
      case "reset-account-password":
        {
          adminOnly(u);
          Security.password(p(q, "password"));
          JdbcRepository.tx(
              c -> {
                BusinessRules.require(
                    AdministrationRepository.findAccount(c, num(q, "id")) != null,
                    "Account not found.");
                AdministrationRepository.updatePassword(
                    c, Security.hash(p(q, "password")), num(q, "id"));
                AdministrationRepository.invalidateResetTokens(c, num(q, "id"));
                AuditService.audit(
                    c, uid, "Administrator password reset", "Account " + num(q, "id"));
                return null;
              });
          go(
              q,
              r,
              "/manage?entity=users",
              "Password reset. Give the new password to the account holder securely.");
          return;
        }

      case "announce":
        {
          adminOnly(u);
          String title = required(q, "title", 180), message = required(q, "message", 1000);
          JdbcRepository.tx(
              c -> {
                String batch = UUID.randomUUID().toString();
                for (var account : AdministrationRepository.listActiveAccounts(c))
                  NotificationService.notify(
                      c,
                      JdbcRepository.id(account, "id"),
                      title,
                      message,
                      "/notifications",
                      "announcement-" + batch + "-" + account.get("id"));
                AuditService.audit(c, uid, "Library announcement", title);
                return null;
              });
          go(q, r, "/settings", "Announcement delivered to active accounts.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }
}

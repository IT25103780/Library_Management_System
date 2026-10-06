package lk.lumina.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.NotificationRepository;

/** Handles notification requests using the existing URLs and form contracts. */
public final class NotificationController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/api/notifications":
        {
          r.setContentType("application/json");
          var n = NotificationRepository.unreadSummary(u.get("id"));
          r.getWriter().write("{\"unread\":" + n.get("n") + ",\"latest\":" + n.get("latest") + "}");
          break;
        }

      case "/notifications":
        {
          q.setAttribute("rows", NotificationRepository.listForMember(u.get("id")));
          view(q, r, "notifications");
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
      case "read-notifications":
        {
          NotificationRepository.markAllRead(
              String.valueOf((num(q, "id") > 0 ? " AND id=" + num(q, "id") : "")), uid);
          go(q, r, "/notifications", "Notifications marked as read.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }
}

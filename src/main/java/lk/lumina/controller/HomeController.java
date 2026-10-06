package lk.lumina.controller;

import static lk.lumina.security.AccessControl.branch;
import static lk.lumina.security.AccessControl.manager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import lk.lumina.repository.DashboardRepository;
import lk.lumina.util.BusinessRules;

/** Handles home requests using the existing URLs and form contracts. */
public final class HomeController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/":
      case "/home":
        {
          q.setAttribute("books", DashboardRepository.listFeaturedBooks());
          q.setAttribute("branches", DashboardRepository.listActiveBranches());
          view(q, r, "home");
          break;
        }

      case "/dashboard":
        {
          BusinessRules.require(manager(u), "Staff dashboard access required.");
          String cpScope = branch(u) == null ? "" : " AND branch_id=" + branch(u);
          q.setAttribute(
              "extraStats",
              List.of(
                  DashboardRepository.countActiveReaders(
                          String.valueOf((branch(u) == null ? "" : " AND branch_id=" + branch(u))))
                      .get("n"),
                  DashboardRepository.countCopies(String.valueOf(cpScope)).get("n"),
                  DashboardRepository.countOverdueLoans(
                          String.valueOf(
                              (branch(u) == null
                                  ? ""
                                  : " AND copy_id IN (SELECT id FROM copies WHERE branch_id="
                                      + branch(u)
                                      + ")")),
                          BusinessRules.now())
                      .get("n"),
                  DashboardRepository.countOpenReservations(String.valueOf(cpScope)).get("n")));
          String loanScope =
              branch(u) == null
                  ? ""
                  : " AND copy_id IN (SELECT id FROM copies WHERE branch_id=" + branch(u) + ")";
          q.setAttribute(
              "stats",
              List.of(
                  DashboardRepository.countActiveBooks().get("n"),
                  DashboardRepository.countPhysicalLoans(String.valueOf(loanScope)).get("n"),
                  DashboardRepository.countDigitalLoans(BusinessRules.now()).get("n"),
                  DashboardRepository.totalRevenue().get("n")));
          q.setAttribute("activity", DashboardRepository.listRecentNotifications(u.get("id")));
          q.setAttribute(
              "popular",
              DashboardRepository.listPopularBooks(
                  String.valueOf(
                      (branch(u) == null
                          ? ""
                          : " AND l.copy_id IN (SELECT id FROM copies WHERE branch_id="
                              + branch(u)
                              + ")"))));
          view(q, r, "dashboard");
          break;
        }
      default:
        throw new IllegalArgumentException("Unknown get.");
    }
  }
}

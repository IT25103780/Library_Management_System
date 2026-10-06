package lk.lumina.controller;

import static lk.lumina.security.AccessControl.manager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lk.lumina.service.ReportService;
import lk.lumina.util.BusinessRules;

/** Handles report requests using the existing URLs and form contracts. */
public final class ReportController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/reports":
      case "/export":
        {
          BusinessRules.require(manager(u), "Reports access required.");
          ReportService.prepare(q, u);
          if (path.equals("/export")) ReportService.export(q, r);
          else view(q, r, "reports");
          break;
        }
      default:
        throw new IllegalArgumentException("Unknown get.");
    }
  }
}

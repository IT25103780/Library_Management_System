package lk.lumina.controller;

import static lk.lumina.security.AccessControl.branch;
import static lk.lumina.security.AccessControl.staffOnly;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;
import lk.lumina.repository.InventoryRepository;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.service.AuditService;
import lk.lumina.service.InventoryService;
import lk.lumina.service.ReservationService;
import lk.lumina.util.BusinessRules;

/** Handles inventory requests using the existing URLs and form contracts. */
public final class InventoryController extends BaseController {
  public static void action(
      HttpServletRequest q, HttpServletResponse r, String action, Map<String, Object> u)
      throws Exception {
    long uid = JdbcRepository.id(u, "id");
    switch (action) {
      case "copy-edit":
      case "copy-delete":
        {
          staffOnly(u);
          InventoryService.editCopy(
              uid,
              num(q, "id"),
              num(q, "branch_id"),
              p(q, "shelf"),
              branch(u),
              action.equals("copy-delete"));
          go(q, r, "/manage?entity=copies", "Copy updated.");
          return;
        }

      case "copy-status":
        {
          staffOnly(u);
          JdbcRepository.tx(
              c -> {
                var copy = InventoryRepository.findCopy(c, num(q, "id"));
                BusinessRules.require(
                    copy != null
                        && Set.of("AVAILABLE", "MAINTENANCE", "LOST", "WITHDRAWN")
                            .contains(copy.get("status")),
                    "Return or release this copy before changing its condition.");
                if (branch(u) != null)
                  BusinessRules.require(
                      JdbcRepository.id(copy, "branch_id") == branch(u),
                      "Copy belongs to another branch.");
                String status = p(q, "status");
                BusinessRules.require(
                    Set.of("AVAILABLE", "MAINTENANCE", "LOST", "WITHDRAWN").contains(status),
                    "Invalid copy condition.");
                InventoryRepository.updateCondition(c, status, copy.get("id"));
                ReservationService.fulfill(c);
                AuditService.audit(c, uid, "Copy condition", copy.get("id") + " / " + status);
                return null;
              });
          go(q, r, "/manage?entity=copies", "Copy condition updated.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }
}
